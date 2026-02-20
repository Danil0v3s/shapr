package br.com.firstsoft.shapr.integration

import br.com.firstsoft.shapr.ShaprApplication
import br.com.firstsoft.shapr.generated.repository.CategoryRepository
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
 * Integration tests for access control.
 * Tests that authentication and authorization work correctly for protected endpoints.
 */
@SpringBootTest(
    classes = [ShaprApplication::class, TestSecurityConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessControlIntegrationTest {

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
    
    private fun userHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.setBasicAuth("user", "user")
        return headers
    }
    
    private fun noAuthHeaders() = HttpHeaders()

    @AfterEach
    fun cleanup() {
        categoryRepository.deleteAll()
    }

    @Test
    fun `unauthenticated request to protected endpoint returns 401`() {
        val response = restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.GET,
            HttpEntity<Any>(noAuthHeaders()),
            Map::class.java
        )

        // Without valid credentials, should return unauthorized
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `authenticated admin can access admin-only endpoints`() {
        val response = restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.GET,
            HttpEntity<Any>(adminHeaders()),
            Map::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `non-admin user cannot access admin-only endpoints`() {
        val response = restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.GET,
            HttpEntity<Any>(userHeaders()),
            Map::class.java
        )
        
        // User without admin role should be forbidden
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `POST to protected endpoint without auth returns 401`() {
        val category = mapOf("name" to "Test")

        val response = restTemplate.exchange(
            baseUrl("/api/categories"),
            HttpMethod.POST,
            HttpEntity(category, noAuthHeaders()),
            Map::class.java
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `DELETE to protected endpoint without auth returns 401`() {
        val response = restTemplate.exchange(
            baseUrl("/api/categories/1"),
            HttpMethod.DELETE,
            HttpEntity<Any>(noAuthHeaders()),
            Void::class.java
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}

