pluginManagement {
    val repositoryUrl = providers.gradleProperty("repositoryUrl").get()

    fun MavenArtifactRepository.setupGithub(repositoryName: String) {
        url = uri("$repositoryUrl/$repositoryName")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
            password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
        }
    }

    repositories {
        maven { setupGithub("pluginkit-android") }
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    val repositoryUrl = providers.gradleProperty("repositoryUrl").get()
    val catalogVersion = providers.gradleProperty("catalogVersion").get()

    fun MavenArtifactRepository.setupGithub(repositoryName: String) {
        url = uri("$repositoryUrl/$repositoryName")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
            password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
        }
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { setupGithub("pluginkit-android") }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    versionCatalogs {
        create("libs") {
            from(catalogVersion)
        }
        create("deps") {
            from(files("gradle/deps.versions.toml"))
        }
    }
}

rootProject.name = "KiloMenos"
include(":app")
include(":core:ui")
include(":core:infrastructure")
include(":core:domain")
include(":core:navigation")
include(":core:monetization")
include(":core:tracking")
include(":feature:expenses")
include(":feature:auth")
include(":feature:history")
include(":feature:dashboard")
include(":feature:profile")
include(":feature:fleet")
include(":feature:overview")
include(":feature:premium")
include(":feature:projection")
