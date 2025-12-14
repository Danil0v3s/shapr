plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

// Include DSL sources in buildSrc
sourceSets {
    main {
        kotlin.srcDir("../shapr-dsl/src/main/kotlin")
    }
}

dependencies {
    implementation("com.squareup:kotlinpoet:1.18.1")
    // Jackson for DSL query types that use Jackson annotations
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
}

gradlePlugin {
    plugins {
        create("shaprCodegen") {
            id = "br.com.firstsoft.shapr.codegen"
            implementationClass = "br.com.firstsoft.shapr.codegen.ShaprCodegenPlugin"
        }
    }
}
