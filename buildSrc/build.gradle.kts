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
}

gradlePlugin {
    plugins {
        create("shaprCodegen") {
            id = "br.com.firstsoft.shapr.codegen"
            implementationClass = "br.com.firstsoft.shapr.codegen.ShaprCodegenPlugin"
        }
    }
}
