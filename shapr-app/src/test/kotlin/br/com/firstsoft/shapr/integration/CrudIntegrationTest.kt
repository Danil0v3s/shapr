package br.com.firstsoft.shapr.integration

import br.com.firstsoft.shapr.ShaprApplication
import br.com.firstsoft.shapr.generated.entity.Category
import br.com.firstsoft.shapr.generated.entity.Product
import br.com.firstsoft.shapr.generated.entity.User
import br.com.firstsoft.shapr.generated.repository.CategoryRepository
import br.com.firstsoft.shapr.generated.repository.ProductRepository
import br.com.firstsoft.shapr.generated.repository.UserRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * Integration tests for CRUD operations.
 * Tests that the generated controllers, entities, and repositories work correctly together.
 */
@SpringBootTest(
    classes = [ShaprApplication::class, TestSecurityConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CrudIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private fun baseUrl(path: String) = "http://localhost:$port$path"
    
    private fun adminHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.setBasicAuth("admin", "admin")
        return headers
    }

    @AfterEach
    fun cleanup() {
        productRepository.deleteAll()
        categoryRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `GET list returns empty array when no data exists`() {
        val response = restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.GET,
            HttpEntity<Any>(adminHeaders()),
            Map::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body!!
        @Suppress("UNCHECKED_CAST")
        val docs = body["docs"] as List<Any>
        assertTrue(docs.isEmpty())
    }

    @Test
    fun `POST creates entity and returns it with generated ID`() {
        val category = mapOf("name" to "Electronics", "description" to "Electronic devices")

        val response = restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.POST,
            HttpEntity(category, adminHeaders()),
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body!!
        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as Map<String, Any>
        assertNotNull(data["id"])
        assertEquals("Electronics", data["name"])
    }

    @Test
    fun `GET by ID returns the created entity`() {
        val saved = categoryRepository.save(Category(name = "Books"))

        val response = restTemplate.exchange(
            baseUrl("/api/categories/${saved.id}"),
            HttpMethod.GET,
            HttpEntity<Any>(adminHeaders()),
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as Map<String, Any>
        assertEquals(saved.id, (data["id"] as Number).toLong())
        assertEquals("Books", data["name"])
    }

    @Test
    fun `GET by ID returns 404 for non-existent entity`() {
        val response = restTemplate.exchange(
            baseUrl("/api/categories/99999"),
            HttpMethod.GET,
            HttpEntity<Any>(adminHeaders()),
            Map::class.java
        )
        
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `PUT updates existing entity`() {
        val saved = categoryRepository.save(Category(name = "Old Name"))
        val update = mapOf("id" to saved.id, "name" to "New Name")
        
        val response = restTemplate.exchange(
            baseUrl("/api/categories/${saved.id}"),
            HttpMethod.PUT,
            HttpEntity(update, adminHeaders()),
            Map::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        
        val updated = categoryRepository.findById(saved.id).get()
        assertEquals("New Name", updated.name)
    }

    @Test
    fun `DELETE removes entity from database`() {
        val saved = categoryRepository.save(Category(name = "ToDelete"))
        assertTrue(categoryRepository.existsById(saved.id))
        
        val response = restTemplate.exchange(
            baseUrl("/api/categories/${saved.id}"),
            HttpMethod.DELETE,
            HttpEntity<Any>(adminHeaders()),
            Void::class.java
        )
        
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        assertFalse(categoryRepository.existsById(saved.id))
    }

    @Test
    fun `entity with unique constraint rejects duplicates`() {
        userRepository.save(User(username = "john", email = "john@test.com"))
        
        // Try to create another user with same email
        val duplicate = mapOf("username" to "jane", "email" to "john@test.com")
        
        val response = restTemplate.exchange(
            baseUrl("/api/users"),
            HttpMethod.POST,
            HttpEntity(duplicate, adminHeaders()),
            Map::class.java
        )
        
        // Should fail due to unique constraint
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }
}

