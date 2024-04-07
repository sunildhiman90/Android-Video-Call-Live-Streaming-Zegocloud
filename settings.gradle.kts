import java.net.URI

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // <- Add this line.
        maven {
            url = URI("https://storage.zego.im/maven")
        }
        maven {
            url = URI("https://www.jitpack.io")
        }

    }
}

rootProject.name = "ZegoCloudExample1"
include(":app")
