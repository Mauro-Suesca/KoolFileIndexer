// Proyecto/indexador/build.gradle.kts

import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

plugins {
    application
    // Este plugin ya aplica 'java' por convención
    id("buildlogic.java-application-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":db"))
    implementation("org.apache.commons:commons-text")
    implementation("org.postgresql:postgresql:42.7.7")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.mockito:mockito-core:4.6.1")
    testImplementation("org.mockito:mockito-junit-jupiter:4.6.1")
}

application {
    // Define la clase principal de la aplicación.
    mainClass = "koolfileindexer.logica.MainIndexadorCLI"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
