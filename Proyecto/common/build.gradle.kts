
plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.mockito:mockito-core:4.6.1")
    testImplementation("org.mockito:mockito-junit-jupiter:4.6.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
