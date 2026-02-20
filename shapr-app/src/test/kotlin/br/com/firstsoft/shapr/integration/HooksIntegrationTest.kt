package br.com.firstsoft.shapr.integration

import br.com.firstsoft.shapr.ShaprApplication
import br.com.firstsoft.shapr.dsl.hooks.*
import br.com.firstsoft.shapr.generated.entity.Category
import br.com.firstsoft.shapr.generated.repository.CategoryRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * Integration tests for collection hooks.
 * Tests that hooks actually transform data during CRUD operations.
 */
@SpringBootTest(
    classes = [ShaprApplication::class, TestSecurityConfig::class, HooksIntegrationTest.TestHooksConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HooksIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    private fun baseUrl(path: String) = "http://localhost:$port$path"
    
    private fun adminHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.setBasicAuth("admin", "admin")
        return headers
    }

    @AfterEach
    fun cleanup() {
        categoryRepository.deleteAll()
        TestCategoryHooks.resetAll()
    }

    @Test
    fun `beforeChange hook transforms data before saving`() {
        // The hook uppercases the name before saving
        val category = mapOf("name" to "electronics", "description" to "test")
        
        val response = restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.POST,
            HttpEntity(category, adminHeaders()),
            Map::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        
        // Verify hook was called
        assertEquals(1, TestCategoryHooks.beforeChangeCallCount)
        
        // Verify the data was transformed (uppercased) in the database
        val saved = categoryRepository.findAll().first()
        assertEquals("ELECTRONICS", saved.name)
    }

    @Test
    fun `afterChange hook is called after saving`() {
        val category = mapOf("name" to "books", "description" to "test")
        
        restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.POST,
            HttpEntity(category, adminHeaders()),
            Map::class.java
        )
        
        // Verify afterChange hook was called
        assertEquals(1, TestCategoryHooks.afterChangeCallCount)
        assertNotNull(TestCategoryHooks.lastAfterChangeDoc)
    }

    @Test
    fun `afterRead hook transforms data when reading`() {
        // Save directly to bypass beforeChange transformation for this test
        val saved = categoryRepository.save(Category(name = "Music"))
        
        val response = restTemplate.exchange(
            baseUrl("/api/categories/${saved.id}"),
            HttpMethod.GET,
            HttpEntity<Any>(adminHeaders()),
            Map::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        
        // Verify afterRead hook was called
        assertTrue(TestCategoryHooks.afterReadCallCount >= 1)
        
        // The afterRead hook appends " [READ]" to the description
        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as Map<String, Any>
        val description = data["description"] as? String
        assertTrue(description?.contains("[READ]") == true, "Expected description to contain [READ], got: $description")
    }

    @Test
    fun `hooks are called during update operations`() {
        val saved = categoryRepository.save(Category(name = "Original"))
        
        val update = mapOf("id" to saved.id, "name" to "updated", "description" to "new desc")
        
        restTemplate.exchange(
            baseUrl("/api/categories/${saved.id}"),
            HttpMethod.PUT,
            HttpEntity(update, adminHeaders()),
            Map::class.java
        )
        
        // Verify beforeChange hook uppercased the name
        val updated = categoryRepository.findById(saved.id).get()
        assertEquals("UPDATED", updated.name)
    }

    @Test
    fun `multiple entities each trigger hooks independently`() {
        val cat1 = mapOf("name" to "first", "description" to "test")
        val cat2 = mapOf("name" to "second", "description" to "test")
        
        restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.POST,
            HttpEntity(cat1, adminHeaders()),
            Map::class.java
        )
        
        restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.POST,
            HttpEntity(cat2, adminHeaders()),
            Map::class.java
        )
        
        // Both should have been transformed
        val all = categoryRepository.findAll()
        assertEquals(2, all.size)
        assertTrue(all.all { it.name == it.name.uppercase() })
        assertEquals(2, TestCategoryHooks.beforeChangeCallCount)
    }

    @Test
    fun `beforeOperation hook can cancel create operation`() {
        // When name contains "BLOCKED", the hook cancels the operation
        val category = mapOf("name" to "BLOCKED_item", "description" to "test")

        val response = restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.POST,
            HttpEntity(category, adminHeaders()),
            Map::class.java
        )

        // Operation should fail because beforeOperation returned null
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)

        // Verify nothing was saved
        assertTrue(categoryRepository.findAll().isEmpty())
    }

    @Test
    fun `beforeValidate hook can modify data before validation`() {
        // The hook trims whitespace from name
        val category = mapOf("name" to "  trimmed  ", "description" to "test")

        val response = restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.POST,
            HttpEntity(category, adminHeaders()),
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, TestCategoryHooks.beforeValidateCallCount)

        // Name should be trimmed AND uppercased (beforeValidate trims, beforeChange uppercases)
        val saved = categoryRepository.findAll().first()
        assertEquals("TRIMMED", saved.name)
    }

    @Test
    fun `list endpoint triggers afterRead for each document`() {
        categoryRepository.save(Category(name = "Cat1"))
        categoryRepository.save(Category(name = "Cat2"))
        categoryRepository.save(Category(name = "Cat3"))

        val response = restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.GET,
            HttpEntity<Any>(adminHeaders()),
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        // afterRead should be called for each document (beforeRead + afterRead for each)
        assertEquals(3, TestCategoryHooks.afterReadCallCount)
    }

    /**
     * Test configuration that provides test hooks.
     */
    @TestConfiguration
    open class TestHooksConfig {
        @Bean
        open fun testCategoryHooks(): CollectionHooks<Category> = TestCategoryHooks()
    }

    /**
     * Test hook that tracks calls and transforms data.
     */
    class TestCategoryHooks : CollectionHooks<Category> {

        companion object {
            var beforeChangeCallCount = 0
            var afterChangeCallCount = 0
            var beforeReadCallCount = 0
            var afterReadCallCount = 0
            var beforeValidateCallCount = 0
            var lastBeforeChangeData: Category? = null
            var lastAfterChangeDoc: Category? = null

            fun resetAll() {
                beforeChangeCallCount = 0
                afterChangeCallCount = 0
                beforeReadCallCount = 0
                afterReadCallCount = 0
                beforeValidateCallCount = 0
                lastBeforeChangeData = null
                lastAfterChangeDoc = null
            }
        }

        override suspend fun beforeOperation(args: BeforeOperationArgs<Category>): BeforeOperationArgs<Category>? {
            // Cancel operation if name contains "BLOCKED"
            if (args.data?.name?.contains("BLOCKED") == true) {
                return null
            }
            return args
        }

        override suspend fun beforeValidate(args: BeforeValidateArgs<Category>): Category? {
            beforeValidateCallCount++
            // Transform: trim whitespace from name
            val data = args.data ?: return null
            return data.copy(name = data.name.trim())
        }

        override suspend fun beforeChange(args: BeforeChangeArgs<Category>): Category {
            beforeChangeCallCount++
            lastBeforeChangeData = args.data
            // Transform: uppercase the name
            return args.data.copy(name = args.data.name.uppercase())
        }

        override suspend fun afterChange(args: AfterChangeArgs<Category>): Category {
            afterChangeCallCount++
            lastAfterChangeDoc = args.doc
            return args.doc
        }

        override suspend fun beforeRead(args: BeforeReadArgs<Category>): Category {
            beforeReadCallCount++
            return args.doc
        }

        override suspend fun afterRead(args: AfterReadArgs<Category>): Category {
            afterReadCallCount++
            // Transform: append [READ] to description
            val desc = args.doc.description ?: ""
            return args.doc.copy(description = "$desc [READ]")
        }
    }
}

