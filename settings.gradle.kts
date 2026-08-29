pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "jolt-android-poc"
include(":app")
include(":raylib-android")
project(":raylib-android").projectDir = file("raylib/android")
include(":raylib-jolt-android")
project(":raylib-jolt-android").projectDir = file("raylib/jolt-android")
include(":raylib-topology-android")
project(":raylib-topology-android").projectDir = file("raylib/topology-android")
include(":raylib-frame-android")
project(":raylib-frame-android").projectDir = file("raylib/frame-android")
