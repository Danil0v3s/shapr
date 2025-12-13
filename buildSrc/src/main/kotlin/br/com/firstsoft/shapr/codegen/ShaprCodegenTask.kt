package br.com.firstsoft.shapr.codegen

import br.com.firstsoft.shapr.codegen.generators.ControllerGenerator
import br.com.firstsoft.shapr.codegen.generators.EntityGenerator
import br.com.firstsoft.shapr.codegen.generators.RepositoryGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

/**
 * Gradle task that generates code from Shapr DSL definitions.
 */
abstract class ShaprCodegenTask : DefaultTask() {
    
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val collectionsFile: RegularFileProperty
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:Input
    abstract val basePackage: Property<String>
    
    @TaskAction
    fun generate() {
        val inputFile = collectionsFile.get().asFile
        val outputDirectory = outputDir.get().asFile
        val pkg = basePackage.get()
        
        logger.lifecycle("Shapr: Reading collections from ${inputFile.path}")
        
        // Parse the DSL file to extract collection definitions
        val config = CollectionParser.parse(inputFile.readText())
        
        if (config.collections.isEmpty()) {
            logger.warn("Shapr: No collections found in ${inputFile.path}")
            return
        }
        
        logger.lifecycle("Shapr: Found ${config.collections.size} collection(s)")
        
        // Generate code for each collection
        val entityGenerator = EntityGenerator(pkg)
        val repositoryGenerator = RepositoryGenerator(pkg)
        val controllerGenerator = ControllerGenerator(pkg)
        
        config.collections.forEach { collection ->
            logger.lifecycle("Shapr: Generating code for collection '${collection.name}'")
            
            // Generate entity
            writeGeneratedFile(outputDirectory, entityGenerator.generate(collection))
            
            // Generate repository
            writeGeneratedFile(outputDirectory, repositoryGenerator.generate(collection))
            
            // Generate controller
            writeGeneratedFile(outputDirectory, controllerGenerator.generate(collection))
        }
        
        logger.lifecycle("Shapr: Code generation complete")
    }
    
    private fun writeGeneratedFile(outputDir: File, generatedFile: GeneratedFile) {
        val packageDir = File(outputDir, generatedFile.packageName.replace('.', '/'))
        packageDir.mkdirs()
        
        val file = File(packageDir, "${generatedFile.fileName}.kt")
        file.writeText(generatedFile.content)
        
        logger.info("Shapr: Generated ${file.path}")
    }
}

/**
 * Represents a generated source file.
 */
data class GeneratedFile(
    val packageName: String,
    val fileName: String,
    val content: String
)
