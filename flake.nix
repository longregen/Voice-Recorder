{
  description = "Fossify Voice Recorder - Android dev environment with API 36 emulator";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = true;
        config.android_sdk.accept_license = true;
      };

      androidComposition = pkgs.androidenv.composeAndroidPackages {
        platformVersions = [ "36.1" ];
        buildToolsVersions = [ "36.1.0" ];
        includeSources = false;
        includeNDK = false;
        includeEmulator = true;
        includeSystemImages = true;
        systemImageTypes = [ "google_apis" ];
        abiVersions = [ "x86_64" ];
      };

      androidSdk = androidComposition.androidsdk;

      emulator = pkgs.androidenv.emulateApp {
        name = "voice-recorder-emulator";
        platformVersion = "36.1";
        abiVersion = "x86_64";
        systemImageType = "google_apis";
        androidEmulatorFlags = "-gpu swiftshader_indirect -no-snapshot";
        sdkExtraArgs = {
          platformVersions = [ "36.1" ];
          buildToolsVersions = [ "36.1.0" ];
          includeEmulator = true;
          includeSystemImages = true;
          systemImageTypes = [ "google_apis" ];
          abiVersions = [ "x86_64" ];
        };
      };

      jdk = pkgs.jdk17;
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        buildInputs = [
          androidSdk
          jdk
          pkgs.gradle
          emulator
        ];

        ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
        ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
        JAVA_HOME = "${jdk}";
        GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/libexec/android-sdk/build-tools/36.1.0/aapt2";

        shellHook = ''
          echo "Voice Recorder Android dev shell"
          echo "  Android SDK: $ANDROID_HOME"
          echo "  Java:        $(java --version 2>&1 | head -1)"
          echo ""
          echo "Commands:"
          echo "  ./gradlew assembleFossDebug   - Build FOSS debug APK"
          echo "  run-emulator                  - Launch API 36 emulator"
          echo "  adb devices                   - List connected devices"
          echo ""

          # Make the emulator script easily accessible
          alias run-emulator='${emulator}/bin/run-test-emulator'
        '';
      };
    };
}
