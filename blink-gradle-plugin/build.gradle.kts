plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-commons:9.7.1")
    compileOnly(gradleApi())
}

gradlePlugin {
    plugins {
        create("blink") {
            id = "priv.seventeen.artist.blink"
            implementationClass = "priv.seventeen.artist.blink.gradle.BlinkPlugin"
        }
    }
}

val repoPassword = System.getenv("repo") ?: ""

publishing {
    repositories {
        maven {
            url = uri(property("mavenRepoUrl") as String)
            isAllowInsecureProtocol = true
            credentials {
                username = property("mavenRepoUser") as String
                password = repoPassword
            }
        }
    }
}
