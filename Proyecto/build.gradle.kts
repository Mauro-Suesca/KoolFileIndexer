
plugins {
    java
    id("com.gradleup.shadow") version "8.3.8" apply false
    id("org.openjfx.javafxplugin") version "0.1.0" apply false
    id("com.diffplug.spotless") version "7.1.0"
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

allprojects {
    group = "koolfileindexer"
    version = "1.0.0"

    // apply a specific flavor of google-java-format
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    if (name != "common") {
        apply(plugin = "application")
    }

    if (name == "app") {
        apply(plugin = "org.openjfx.javafxplugin")
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.9.2")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.9.2")
        "testImplementation"("org.mockito:mockito-core:5.3.1")
        "testImplementation"("org.mockito:mockito-junit-jupiter:5.3.1")
    }
}

spotless {
  format("misc", {
    target("*.gradle.kts", "*/*.gradle.kts", ".gitattributes", ".gitignore")

    trimTrailingWhitespace()
    leadingTabsToSpaces(4)
    endWithNewline()
  })
  java {
    target("*/src/main/java/**/*.java")

    importOrder()
    removeUnusedImports()
    removeWildcardImports()

    cleanthat()

    googleJavaFormat("1.28.0").aosp().reflowLongStrings()
    formatAnnotations()
  }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
