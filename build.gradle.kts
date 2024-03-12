plugins {
    java
    alias(libs.plugins.blossom)
    alias(libs.plugins.runvelocity)
    alias(libs.plugins.shadow)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://mvn.exceptionflug.de/repository/exceptionflug-public/")

}

dependencies {
    implementation(libs.bstats)
    compileOnly(libs.velocity.api)
    compileOnly(libs.velocity.proxy)
    annotationProcessor(libs.velocity.api)
    compileOnly(libs.vpacketevents)
    implementation("ninja.leaping.configurate:configurate-hocon:3.7.1")
    implementation("ninja.leaping.configurate:configurate-core:3.7.1")
}

blossom {
    replaceTokenIn("src/main/java/io/github/_4drian3d/unsignedvelocity/utils/Constants.java")
    replaceToken("{version}", version)
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    build {
        dependsOn(shadowJar)
    }
    clean {
        delete("run")
    }
    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        relocate("org.bstats", "io.github._4drian3d.unsignedvelocity.libs.bstats")
        minimize()
    }
    runVelocity {
        velocityVersion(libs.versions.velocity.get())
    }
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
