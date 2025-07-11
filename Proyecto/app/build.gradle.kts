/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    application
    id("buildlogic.java-application-conventions")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

javafx {
    version = "21"
    modules("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation("org.postgresql:postgresql:42.7.7")
}

application {
    // Define the main class for the application.
    mainClass = "koolfileindexer.App"
    applicationDefaultJvmArgs =
        listOf(
            "--add-modules=javafx.controls,javafx.fxml",
        )
}
