package br.com.firstsoft.shapr.codegen

import br.com.firstsoft.shapr.codegen.generators.*
import br.com.firstsoft.shapr.dsl.ShaprConfig
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

/**
 * Gradle task that generates code from Shapr DSL definitions.
 */
abstract class ShaprCodegenTask : DefaultTask() {
    
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val collectionsDirectory: DirectoryProperty
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:Input
    abstract val basePackage: Property<String>
    
    @TaskAction
    fun generate() {
        val collectionsDir = collectionsDirectory.get().asFile
        val outputDirectory = outputDir.get().asFile
        val pkg = basePackage.get()
        
        if (!collectionsDir.exists() || !collectionsDir.isDirectory) {
            logger.warn("Shapr: Collections directory does not exist: ${collectionsDir.path}")
            return
        }
        
        // Discover all Kotlin files with collection definitions
        val inputFiles = collectionsDir.listFiles { file ->
            file.isFile && file.extension == "kt"
        }?.toList() ?: emptyList()

        if (inputFiles.isEmpty()) {
            logger.warn("Shapr: No collection files found in ${collectionsDir.path}")
            return
        }

        logger.lifecycle("Shapr: Reading collections from ${inputFiles.size} file(s)")

        // Parse all collection files (supports both DSL and annotated classes)
        val configs = inputFiles.mapNotNull { file ->
            val content = file.readText()
            logger.info("Shapr: Parsing ${file.path}")
            when {
                content.contains("@ShaprCol") -> ClassParser.parse(content)
                content.contains("shapr {") -> CollectionParser.parse(content)
                else -> null
            }
        }
        
        // Merge all configs and validate unique slugs
        val config = ShaprConfig.mergeAll(*configs.toTypedArray())
        
        if (config.collections.isEmpty()) {
            logger.warn("Shapr: No collections found in any of the specified files")
            return
        }
        
        logger.lifecycle("Shapr: Found ${config.collections.size} collection(s)")
        
        // Generate code for each collection
        val entityGenerator = EntityGenerator(pkg)
        val repositoryGenerator = RepositoryGenerator(pkg)
        val controllerGenerator = ControllerGenerator(pkg)
        val localesTableGenerator = LocalesTableGenerator(pkg)
        val arrayTableGenerator = ArrayTableGenerator(pkg)
        val blockTableGenerator = BlockTableGenerator(pkg)
        val relsTableGenerator = RelsTableGenerator(pkg)

        config.collections.forEach { collection ->
            logger.lifecycle("Shapr: Generating code for collection '${collection.name}'")

            // Generate main entity
            writeGeneratedFile(outputDirectory, entityGenerator.generate(collection))

            // Generate repository
            writeGeneratedFile(outputDirectory, repositoryGenerator.generate(collection))

            // Generate controller
            writeGeneratedFile(outputDirectory, controllerGenerator.generate(collection))

            // Generate _locales table (if collection has localized fields)
            localesTableGenerator.generate(collection)?.let {
                writeGeneratedFile(outputDirectory, it)
                logger.info("Shapr: Generated locales table for '${collection.name}'")
            }

            // Generate array tables (one per array field)
            arrayTableGenerator.generate(collection).forEach {
                writeGeneratedFile(outputDirectory, it)
                logger.info("Shapr: Generated array table: ${it.fileName}")
            }

            // Generate block tables (one per block type)
            blockTableGenerator.generate(collection).forEach {
                writeGeneratedFile(outputDirectory, it)
                logger.info("Shapr: Generated block table: ${it.fileName}")
            }

            // Generate _rels table (if collection has polymorphic/hasMany relationships)
            relsTableGenerator.generate(collection)?.let {
                writeGeneratedFile(outputDirectory, it)
                logger.info("Shapr: Generated rels table for '${collection.name}'")
            }
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
