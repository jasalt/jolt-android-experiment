plugins {
  id("com.android.application")
}

android {
  namespace = "net.joltlang.androidpoc.abiprobe"
  compileSdk = 35
  ndkVersion = "29.0.14206865"

  defaultConfig {
    applicationId = "net.joltlang.androidpoc.abiprobe"
    minSdk = 35
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    externalNativeBuild {
      cmake {
        cppFlags += "-std=c17"
      }
    }

    ndk {
      abiFilters += "arm64-v8a"
    }
  }

  sourceSets {
    getByName("main").jniLibs.srcDir("../native/jolt/android-arm64")
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }
}
