# Temporary local composition wrapper for android-nixpkgs.
#
# android-nixpkgs' sdk.nix verifies the resulting read-only SDK with
# `sdkmanager --list --verbose`. Command-line tools 23 redirects that command
# through the new Android CLI, which attempts to create a bin directory and
# fails in the Nix build sandbox. The SDK packages themselves are immutable and
# already selected from the pinned android-nixpkgs input. This composition
# preserves its symlink layout, generated license directory, setup hook, and
# package-provided wrappers, but omits only that incompatible mutable-state
# probe. Remove this file when the upstream SDK composition supports the new
# command-line tools.
{
  pkgs,
  packages,
}:
pkgsFun:
let
  inherit (builtins) attrValues concatStringsSep length;
  inherit (pkgs.lib)
    all
    any
    assertMsg
    concatMapStringsSep
    filterAttrs
    groupBy
    groupBy'
    mapAttrs
    mapAttrsToList
    unique;

  packages' = filterAttrs (_: package: pkgs.lib.isDerivation package) packages;
  selectedPackages = unique (pkgsFun packages');
  duplicates = filterAttrs (_: matches: length matches > 1)
    (groupBy (package: package.path) selectedPackages);
  duplicateMessage = concatStringsSep "\n\n" (mapAttrsToList
    (path: matches: "${path}:\n" + concatMapStringsSep "\n"
      (package: "  ${package.name}") matches)
    duplicates);

  licenses = pkgs.linkFarm "android-licenses" (mapAttrsToList
    (id: hashes: {
      name = id;
      path = pkgs.writeText id ("\n" + concatStringsSep "\n" hashes);
    })
    (groupBy' (hashes: package: unique (hashes ++ [ package.license.hash ])) []
      (package: package.license.id) (attrValues packages')));

  installSdk = concatMapStringsSep "\n" (package: ''
    pkgBase="$ANDROID_SDK_ROOT/${package.path}"
    mkdir -p "$(dirname "$pkgBase")"
    cp -as ${package}/ "$pkgBase"
    chmod +w "$pkgBase"
    cp "${package.xml}" "$pkgBase/package.xml"
    ${package.installSdk or ""}
  '') selectedPackages;
in
assert assertMsg (duplicates == { }) "Android SDK packages collide:\n${duplicateMessage}";
assert assertMsg (all (package: package.name != "tools") selectedPackages)
  "The Android tools package is obsolete; use command-line tools.";
assert assertMsg (any (package: package.pname == "cmdline-tools") selectedPackages)
  "Android command-line tools are required.";
pkgs.runCommand "android-sdk-env"
  {
    buildInputs = [ licenses ] ++ selectedPackages;
    nativeBuildInputs = [ pkgs.makeWrapper ];
    preferLocalBuild = true;
    allowSubstitutes = false;
    setupHook = pkgs.writeText "setup-hook" ''
      export ANDROID_SDK_ROOT="@out@/share/android-sdk"
      export ANDROID_HOME="$ANDROID_SDK_ROOT"
    '';
    shellHook = ''
      export ANDROID_SDK_ROOT="$out/share/android-sdk"
      export ANDROID_HOME="$ANDROID_SDK_ROOT"
    '';
    passthru.packages = selectedPackages;
  }
  ''
    export ANDROID_SDK_ROOT="$out/share/android-sdk"
    mkdir -p "$ANDROID_SDK_ROOT" "$out/bin"
    ${installSdk}
    mkdir -p "$ANDROID_SDK_ROOT/licenses"
    cp -as ${licenses}/* "$ANDROID_SDK_ROOT/licenses"
    source ${pkgs.stdenv.setup}
    mkdir -p "$out/nix-support"
    substituteAll "$setupHook" "$out/nix-support/setup-hook"
  ''
