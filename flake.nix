{
  description = "Reproducible Jolt Android feasibility environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    android = {
      url = "github:tadfisher/android-nixpkgs/stable";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, android, ... }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
    in {
      packages = forAllSystems (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config.allowUnfree = true;
          };
        in
        pkgs.lib.optionalAttrs (system == "x86_64-linux") {
          android-sdk =
            let
              # cmdline-tools 23 added a native `android` CLI binary. At the
              # pinned android-nixpkgs revision it retains the host ELF
              # interpreter, which cannot run in a Nix sandbox while the SDK
              # composition calls `sdkmanager --list`. Patch only that binary
              # until android-nixpkgs incorporates an upstream fix.
              cmdlineTools = android.packages.${system}.cmdline-tools-latest.overrideAttrs (old: {
                postFixup = (old.postFixup or "") + ''
                  ${pkgs.patchelf}/bin/patchelf \
                    --set-interpreter ${pkgs.stdenv.cc.bintools.dynamicLinker} \
                    "$out/bin/android"
                '';
              });
            in
            (import ./nix/android-sdk.nix {
              inherit pkgs;
              packages = android.packages.${system};
            }) (sdkPkgs: with sdkPkgs; [
              cmdlineTools
              build-tools-35-0-0
              platform-tools
              platforms-android-35
              emulator
              system-images-android-35-google-apis-x86-64
              ndk-29-0-14206865
              cmake-3-22-1
            ]);
        });

      devShells = forAllSystems (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config.allowUnfree = true;
          };
          androidSdk = if system == "x86_64-linux"
            then self.packages.${system}.android-sdk
            else null;
          androidSdkRoot = "${androidSdk}/share/android-sdk";
        in {
          default = pkgs.mkShell {
            packages = [
              pkgs.jdk21
              pkgs.gradle
              pkgs.cmake
              pkgs.ninja
              pkgs.clang
              pkgs.lld
              pkgs.pkg-config
              pkgs.git
              pkgs.gnumake
              pkgs.jq
              pkgs.file
              pkgs.binutils
              pkgs.which
            ] ++ pkgs.lib.optionals (system == "x86_64-linux") [ androidSdk ];

            shellHook = pkgs.lib.optionalString (system == "x86_64-linux") ''
              export ANDROID_HOME=${androidSdkRoot}
              export ANDROID_SDK_ROOT="$ANDROID_HOME"
              export ANDROID_NDK_ROOT="$ANDROID_HOME/ndk/29.0.14206865"
              export ANDROID_AVD_HOME="$PWD/.android/avd"
              export GRADLE_USER_HOME="$PWD/.gradle"
              export JAVA_HOME=${pkgs.jdk21.home}
            '';
          };
        });
    };
}
