package br.com.firstsoft.shapr.codegen

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring the Shapr code generation plugin.
 */
interface ShaprCodegenExtension {
    /**
     * The Kotlin file containing the collections DSL definitions.
     */
    val collectionsFile: RegularFileProperty
    
    /**
     * The output directory for generated sources.
     */
    val outputDir: DirectoryProperty
    
    /**
     * Base package for generated code.
     */
    val basePackage: Property<String>
}

/**
 * Gradle plugin for Shapr code generation.
 * 
 * This plugin registers a task that reads collection definitions from a Kotlin DSL file
 * and generates JPA entities, Spring Data repositories, and REST controllers.
 */
class ShaprCodegenPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        // Create the extension
        val extension = project.extensions.create(
            "shaprCodegen",
            ShaprCodegenExtension::class.java
        )
        
        // Set defaults
        extension.basePackage.convention("br.com.firstsoft.shapr.generated")
        extension.outputDir.convention(project.layout.buildDirectory.dir("generated/sources/shapr"))
        
        // Register the code generation task
        project.tasks.register("generateShaprCode", ShaprCodegenTask::class.java).configure(object : Action<ShaprCodegenTask> {
            override fun execute(task: ShaprCodegenTask) {
                task.group = "shapr"
                task.description = "Generates JPA entities, repositories, and controllers from Shapr DSL definitions"
                
                task.collectionsFile.set(extension.collectionsFile)
                task.outputDir.set(extension.outputDir)
                task.basePackage.set(extension.basePackage)
            }
        })
    }
}
