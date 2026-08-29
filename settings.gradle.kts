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
