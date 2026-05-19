plugins {
    java
    application
}

group = "moe.senpai.guard"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-tree:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
    implementation("org.ow2.asm:asm-analysis:9.7")
    implementation("info.picocli:picocli:4.7.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

application {
    mainClass.set("senpai.cli.SenpaiMain")
    applicationName = "senpai"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "senpai.cli.SenpaiMain",
            "Implementation-Title" to "Senpai Guard",
            "Implementation-Version" to project.version,
            "Motto" to "It's not like I want to protect your code or anything..."
        )
    }
}
