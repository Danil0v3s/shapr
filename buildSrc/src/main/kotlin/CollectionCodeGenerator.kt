import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import java.io.File

abstract class CollectionCodeGenerator : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract var compiledClasses: FileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputDirectory = outputDir.get().asFile
        outputDirectory.mkdirs()

        val entities = scanForEntities()

        entities.forEach { entity ->
            generateRepository(entity, outputDirectory)
            generateController(entity, outputDirectory)
        }

        if (entities.isNotEmpty()) {
            println("Generated ${entities.size} repository/controller pairs")
        }
    }

    private fun scanForEntities(): List<EntityMetadata> {
        val entities = mutableListOf<EntityMetadata>()

        compiledClasses.files.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown()
                    .filter { it.isFile && it.extension == "class" }
                    .forEach { classFile ->
                        scanClassFile(classFile)?.let { entities.add(it) }
                    }
            }
        }

        return entities
    }

    private fun scanClassFile(classFile: File): EntityMetadata? {
        val classReader = ClassReader(classFile.readBytes())
        var hasEntityAnnotation = false
        var hasCollectionAnnotation = false
        var collectionPath = ""
        var collectionAuth = true
        var className = ""
        var packageName = ""
        var idFieldName = ""
        var idFieldType = "Long"

        val visitor = object : ClassVisitor(Opcodes.ASM9) {
            override fun visit(
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                val fullName = name.replace('/', '.')
                val lastDot = fullName.lastIndexOf('.')
                if (lastDot > 0) {
                    packageName = fullName.substring(0, lastDot)
                    className = fullName.substring(lastDot + 1)
                } else {
                    className = fullName
                }
            }

            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                when (descriptor) {
                    "Ljakarta/persistence/Entity;" -> hasEntityAnnotation = true
                    "Lbr/com/firstsoft/shapr/cms/annotation/Collection;" -> {
                        hasCollectionAnnotation = true
                        return object : AnnotationVisitor(Opcodes.ASM9) {
                            override fun visit(name: String?, value: Any?) {
                                when (name) {
                                    "path" -> collectionPath = value as? String ?: ""
                                    "auth" -> collectionAuth = value as? Boolean ?: true
                                }
                            }
                        }
                    }
                }
                return null
            }

            override fun visitField(
                access: Int,
                name: String,
                descriptor: String,
                signature: String?,
                value: Any?
            ): FieldVisitor {
                return object : FieldVisitor(Opcodes.ASM9) {
                    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                        if (desc == "Ljakarta/persistence/Id;") {
                            idFieldName = name
                            idFieldType = when (descriptor) {
                                "J" -> "Long"
                                "I" -> "Integer"
                                "Ljava/lang/Long;" -> "Long"
                                "Ljava/lang/Integer;" -> "Integer"
                                "Ljava/lang/String;" -> "String"
                                "Ljava/util/UUID;" -> "UUID"
                                else -> "Long"
                            }
                        }
                        return null
                    }
                }
            }
        }

        classReader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

        return if (hasEntityAnnotation && hasCollectionAnnotation) {
            EntityMetadata(
                packageName = packageName,
                className = className,
                idType = idFieldType,
                path = collectionPath.ifEmpty { pluralize(className).lowercase() },
                auth = collectionAuth
            )
        } else null
    }

    private fun generateRepository(entity: EntityMetadata, outputDir: File) {
        val repoPackage = "br.com.firstsoft.shapr.generated.repository"
        val repoDir = File(outputDir, repoPackage.replace('.', '/'))
        repoDir.mkdirs()

        val idImport = when (entity.idType) {
            "UUID" -> "import java.util.UUID"
            else -> ""
        }

        val content = """
            |// Generated by Shapr CMS - DO NOT EDIT
            |package $repoPackage
            |
            |import ${entity.packageName}.${entity.className}
            |import org.springframework.data.jpa.repository.JpaRepository
            |import org.springframework.stereotype.Repository
            |$idImport
            |
            |@Repository
            |interface ${entity.className}Repository : JpaRepository<${entity.className}, ${entity.idType}>
        """.trimMargin()

        File(repoDir, "${entity.className}Repository.kt").writeText(content)
    }

    private fun generateController(entity: EntityMetadata, outputDir: File) {
        val ctrlPackage = "br.com.firstsoft.shapr.generated.controller"
        val repoPackage = "br.com.firstsoft.shapr.generated.repository"
        val ctrlDir = File(outputDir, ctrlPackage.replace('.', '/'))
        ctrlDir.mkdirs()

        val idImport = when (entity.idType) {
            "UUID" -> "import java.util.UUID"
            else -> ""
        }

        val content = """
            |// Generated by Shapr CMS - DO NOT EDIT
            |package $ctrlPackage
            |
            |import ${entity.packageName}.${entity.className}
            |import $repoPackage.${entity.className}Repository
            |import org.springframework.http.ResponseEntity
            |import org.springframework.web.bind.annotation.*
            |$idImport
            |
            |@RestController
            |@RequestMapping("/api/${entity.path}")
            |class ${entity.className}Controller(
            |    private val repository: ${entity.className}Repository
            |) {
            |    @GetMapping
            |    fun list(): List<${entity.className}> = repository.findAll()
            |
            |    @GetMapping("/{id}")
            |    fun getById(@PathVariable id: ${entity.idType}): ResponseEntity<${entity.className}> =
            |        repository.findById(id)
            |            .map { ResponseEntity.ok(it) }
            |            .orElse(ResponseEntity.notFound().build())
            |
            |    @PostMapping
            |    fun create(@RequestBody entity: ${entity.className}): ${entity.className} =
            |        repository.save(entity)
            |
            |    @PutMapping("/{id}")
            |    fun update(
            |        @PathVariable id: ${entity.idType},
            |        @RequestBody entity: ${entity.className}
            |    ): ResponseEntity<${entity.className}> =
            |        if (repository.existsById(id)) {
            |            ResponseEntity.ok(repository.save(entity))
            |        } else {
            |            ResponseEntity.notFound().build()
            |        }
            |
            |    @DeleteMapping("/{id}")
            |    fun delete(@PathVariable id: ${entity.idType}): ResponseEntity<Void> =
            |        if (repository.existsById(id)) {
            |            repository.deleteById(id)
            |            ResponseEntity.noContent().build()
            |        } else {
            |            ResponseEntity.notFound().build()
            |        }
            |}
        """.trimMargin()

        File(ctrlDir, "${entity.className}Controller.kt").writeText(content)
    }

    private fun pluralize(name: String): String = when {
        name.endsWith("y") && !name.endsWith("ay") && !name.endsWith("ey") &&
                !name.endsWith("oy") && !name.endsWith("uy") -> name.dropLast(1) + "ies"
        name.endsWith("s") || name.endsWith("x") || name.endsWith("z") ||
                name.endsWith("ch") || name.endsWith("sh") -> name + "es"
        else -> name + "s"
    }

    data class EntityMetadata(
        val packageName: String,
        val className: String,
        val idType: String,
        val path: String,
        val auth: Boolean
    )
}
