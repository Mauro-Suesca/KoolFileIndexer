
plugins {
    java
    id("com.gradleup.shadow") version "8.3.8" apply false
    id("org.openjfx.javafxplugin") version "0.1.0" apply false
    id("com.diffplug.spotless") version "7.1.0"
    id("checkstyle")
    id("com.github.spotbugs") version "6.0.15" apply false
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
    apply(plugin = "checkstyle")
    apply(plugin = "com.github.spotbugs")

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
    // Checkstyle
    configure<CheckstyleExtension> {
        toolVersion = "10.12.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
    }

    tasks.withType<Checkstyle> {
        reports {
            html.required.set(true)
            html.outputLocation.set(file("$buildDir/reports/checkstyle/${name}.html"))
        }
    }

    // SpotBugs
    configure<com.github.spotbugs.snom.SpotBugsExtension> {
        toolVersion.set("4.8.5")
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
        excludeFilter.set(file("$rootDir/config/spotbugs/excludeFilter.xml")) 
    }

    tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
        reports.create("html") {
            required.set(true)
            outputLocation.set(file("$buildDir/reports/spotbugs/${name}.html"))
        }
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
