plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	war
	id("org.springframework.boot") version "4.0.0"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "br.com.firstsoft"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

// Directory for generated sources
val generatedSourceDir = layout.buildDirectory.dir("generated/sources/cms")

// Source sets with proper compilation order:
// 1. cms - @Collection annotation (no dependencies)
// 2. entity - Entity classes (depends on cms)
// 3. main - Framework + generated code (depends on cms + entity)
sourceSets {
	// CMS annotation source set - compiles first
	create("cms") {
		kotlin.srcDir("src/cms/kotlin")
	}
	
	// Entity source set - compiles second
	create("entity") {
		kotlin.srcDir("src/entity/kotlin")
		compileClasspath += sourceSets["cms"].output
		compileClasspath += configurations.compileClasspath.get()
	}
	
	// Main source set - compiles third, includes generated code
	main {
		kotlin.srcDir(generatedSourceDir)
		compileClasspath += sourceSets["cms"].output
		compileClasspath += sourceSets["entity"].output
		runtimeClasspath += sourceSets["cms"].output
		runtimeClasspath += sourceSets["entity"].output
	}
}

// Configure cms compilation
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileCmsKotlin") {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

// Configure entity compilation - depends on cms
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileEntityKotlin") {
	dependsOn("compileCmsKotlin")
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

// Code generation task - runs after entity compilation
val generateCollectionCode by tasks.registering(CollectionCodeGenerator::class) {
	description = "Generates Repository and Controller code for @Collection entities"
	group = "generation"

	dependsOn("compileEntityKotlin")
	compiledClasses = sourceSets["entity"].output.classesDirs
	outputDir.set(generatedSourceDir)
}

// Main compilation depends on code generation
tasks.named("compileKotlin") {
	dependsOn(generateCollectionCode)
}

// Include all source set outputs in JARs
tasks.named<Jar>("jar") {
	from(sourceSets["cms"].output)
	from(sourceSets["entity"].output)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	from(sourceSets["cms"].output)
	from(sourceSets["entity"].output)
}

tasks.named<War>("war") {
	from(sourceSets["cms"].output)
	from(sourceSets["entity"].output)
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	
	// Entity source set dependencies
	"entityImplementation"("org.springframework.boot:spring-boot-starter-data-jpa")
	"entityImplementation"("org.jetbrains.kotlin:kotlin-reflect")
	
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.microsoft.sqlserver:mssql-jdbc")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat-runtime")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
