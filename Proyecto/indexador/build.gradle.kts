/*
 * Modulo indexador
 */

plugins {
    application
    // Este plugin ya aplica 'java' por convención
    id("buildlogic.java-application-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation("org.postgresql:postgresql:42.7.7")

    // ——> Para los tests:
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

application {
    // Define la clase principal de la aplicación.
    mainClass = "koolfileindexer.Indexador"
}

// Configura el task 'test' para que use JUnit Platform (JUnit 5)
tasks.test {
    useJUnitPlatform()
}
