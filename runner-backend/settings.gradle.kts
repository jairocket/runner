rootProject.name = "runner-backend"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include(":app", ":domain", ":infra")
project(":app").projectDir = file("modules/app")
project(":domain").projectDir = file("modules/domain")
project(":infra").projectDir = file("modules/infra")
