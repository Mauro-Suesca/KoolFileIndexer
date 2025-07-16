/*
 * Modulo indexador
 */

plugins {
    application
    id("buildlogic.java-application-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation("org.postgresql:postgresql:42.7.7")
    testImplementation("junit:junit:4.13.2")
}

application {
    // Define the main class for the application.
    mainClass = "koolfileindexer.Indexador"
}
