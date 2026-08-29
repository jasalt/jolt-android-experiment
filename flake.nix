{
  description = "Reproducible Jolt Android feasibility environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    android = {
      url = "github:tadfisher/android-nixpkgs/stable";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    jolt = {
      url = "git+https://github.com/jolt-lang/jolt.git?rev=8fcba79f8b33628af926f88032d93a1b31c24235&submodules=1";
      flake = false;
    };
  };

  outputs = { self, nixpkgs, android, jolt, ... }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
      mkJolt = pkgs:
        let
          version = jolt.rev or "dev";
        in
        pkgs.stdenv.mkDerivation {
          pname = "jolt";
          inherit version;
          src = jolt;

          strictDeps = true;
          nativeBuildInputs = [
            pkgs.chez
            pkgs.makeWrapper
            pkgs.pkg-config
            pkgs.xxd
          ];
          buildInputs = [
            pkgs.lz4
            pkgs.zlib
            pkgs.ncurses
            pkgs.openssl
          ] ++ pkgs.lib.optionals pkgs.stdenv.hostPlatform.isLinux [ pkgs.libuuid ]
            ++ pkgs.lib.optionals pkgs.stdenv.hostPlatform.isDarwin [ pkgs.libiconv ];

          JOLT_VERSION = version;
          dontConfigure = true;

          buildPhase = ''
            runHook preBuild
            scheme --script host/chez/build-jolt.ss release target/release/jolt
            runHook postBuild
          '';

          installPhase = ''
            runHook preInstall
            install -Dm755 target/release/jolt "$out/bin/jolt"
            runHook postInstall
          '';

          postFixup = ''
            wrapProgram "$out/bin/jolt" \
              --prefix PATH : "${pkgs.lib.makeBinPath [ pkgs.git pkgs.unzip ]}" \
              --set-default JOLT_OPENSSL_LIBDIR "${pkgs.lib.makeLibraryPath [ pkgs.openssl ]}" \
              --set-default SSL_CERT_FILE "${pkgs.cacert}/etc/ssl/certs/ca-bundle.crt"
          '';
        };
    in {
      packages = forAllSystems (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config.allowUnfree = true;
          };
          androidSupported = builtins.elem system [ "x86_64-linux" "aarch64-darwin" ];
        in
        {
          jolt = mkJolt pkgs;
        } // pkgs.lib.optionalAttrs androidSupported {
          android-sdk =
            let
              # cmdline-tools 23's Linux `android` binary retains the host ELF
              # interpreter at this pinned revision. Patch it only on Linux;
              # the native Darwin binary requires no ELF fixup.
              linuxCmdlineTools = android.packages.${system}.cmdline-tools-latest.overrideAttrs (old: {
                postFixup = (old.postFixup or "") + ''
                  ${pkgs.patchelf}/bin/patchelf \
                    --set-interpreter ${pkgs.stdenv.cc.bintools.dynamicLinker} \
                    "$out/bin/android"
                '';
              });
              cmdlineTools = if pkgs.stdenv.hostPlatform.isLinux
                then linuxCmdlineTools
                else android.packages.${system}.cmdline-tools-latest;
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
              ndk-29-0-14206865
              cmake-3-22-1
            ] ++ pkgs.lib.optionals (system == "x86_64-linux") [
              system-images-android-35-google-apis-x86-64
            ] ++ pkgs.lib.optionals (system == "aarch64-darwin") [
              system-images-android-35-google-apis-arm64-v8a
            ]);
        });

      devShells = forAllSystems (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config.allowUnfree = true;
          };
          androidSupported = builtins.elem system [ "x86_64-linux" "aarch64-darwin" ];
          androidSdk = if androidSupported
            then self.packages.${system}.android-sdk
            else null;
          androidSdkRoot = "${androidSdk}/share/android-sdk";
          gtkLibraryPath = pkgs.lib.makeLibraryPath [ pkgs.glib pkgs.gtk4 ];
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
              pkgs.xxd
              pkgs.which
              pkgs.cacert
              pkgs.chez
              pkgs.ncurses
              self.packages.${system}.jolt
            ] ++ pkgs.lib.optionals pkgs.stdenv.hostPlatform.isLinux [ pkgs.libuuid ] ++ pkgs.lib.optionals androidSupported [ androidSdk ]
              ++ pkgs.lib.optionals pkgs.stdenv.hostPlatform.isLinux [
                pkgs.gtk4
                pkgs.glib
              ];

            shellHook = pkgs.lib.optionalString androidSupported ''
              export ANDROID_HOME=${androidSdkRoot}
              export ANDROID_SDK_ROOT="$ANDROID_HOME"
              export ANDROID_NDK_ROOT="$ANDROID_HOME/ndk/29.0.14206865"
              export ANDROID_AVD_HOME="$PWD/.android/avd"
              export GRADLE_USER_HOME="$PWD/.gradle"
              export JAVA_HOME=${pkgs.jdk21.home}
              export SSL_CERT_FILE=${pkgs.cacert}/etc/ssl/certs/ca-bundle.crt
              export GIT_SSL_CAINFO="$SSL_CERT_FILE"
            '' + pkgs.lib.optionalString pkgs.stdenv.hostPlatform.isLinux ''
              # Jolt FFI resolves the glimmer-gtk native library names with
              # dlopen. Nix keeps them outside the system loader paths.
              export LD_LIBRARY_PATH=${gtkLibraryPath}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
            '';
          };
        });
    };
}
