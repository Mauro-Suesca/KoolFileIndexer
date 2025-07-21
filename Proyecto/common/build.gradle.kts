
plugins {
    application
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/com.github.jnr/jnr-unixsocket
    implementation("com.github.jnr:jnr-unixsocket:0.38.23")
    implementation("org.ow2.asm:asm:9.5")
    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.mockito:mockito-core:4.6.1")
    testImplementation("org.mockito:mockito-junit-jupiter:4.6.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass = "koolfileindexer.common.examples.ServerAPIExample"
}