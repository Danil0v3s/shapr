plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("br.com.firstsoft.shapr.codegen")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Directory for generated sources
val generatedSourceDir = layout.buildDirectory.dir("generated/sources/shapr")

sourceSets {
    main {
        kotlin.srcDir(generatedSourceDir)
    }
}

dependencies {
    implementation(project(":shapr-dsl"))
    implementation(project(":shapr-runtime"))
    implementation(kotlin("reflect"))
    
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test-junit5"))
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Configure the Shapr code generation
shaprCodegen {
    collectionsFile.set(file("src/main/kotlin/br/com/firstsoft/shapr/collections/Collections.kt"))
    outputDir.set(generatedSourceDir)
    basePackage.set("br.com.firstsoft.shapr.generated")
}

// Ensure code generation runs before compilation
tasks.named("compileKotlin") {
    dependsOn("generateShaprCode")
}
