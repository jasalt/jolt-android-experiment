plugins {
  id("com.android.application")
}

android {
  namespace = "net.joltlang.raylibloopprobe"
  compileSdk = 35
  ndkVersion = "29.0.14206865"

  defaultConfig {
    applicationId = "net.joltlang.raylibloopprobe"
    minSdk = 35
    targetSdk = 35
    versionCode = 1
    versionName = "0.1"

    ndk { abiFilters += "arm64-v8a" }
    externalNativeBuild {
      cmake {
        cppFlags += "-std=c17"
        arguments += "-DPLATFORM=Android"
      }
    }
  }

  sourceSets {
    getByName("main").jniLibs.srcDir("src/main/jniLibs")
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }
}
