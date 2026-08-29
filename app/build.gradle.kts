plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

val junitVersion = "4.13.2"

android {
  namespace = "net.joltlang.androidpoc.abiprobe"
  compileSdk = 35
  ndkVersion = "29.0.14206865"

  buildFeatures {
    buildConfig = true
  }

  defaultConfig {
    applicationId = "net.joltlang.androidpoc.abiprobe"
    minSdk = 35
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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

  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
  }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.12.01"))
  implementation("androidx.activity:activity-compose:1.10.0")
  implementation("androidx.core:core-ktx:1.15.0")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")
  testImplementation("junit:junit:$junitVersion")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test:runner:1.6.2")
  androidTestUtil("androidx.test:orchestrator:1.5.1")
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
  }
}
