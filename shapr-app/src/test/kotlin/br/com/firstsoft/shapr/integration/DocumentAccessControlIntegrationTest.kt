package br.com.firstsoft.shapr.integration

import br.com.firstsoft.shapr.ShaprApplication
import br.com.firstsoft.shapr.dsl.*
import br.com.firstsoft.shapr.generated.entity.Order
import br.com.firstsoft.shapr.generated.repository.OrderRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * Integration tests for document-level access control (row-level security).
 * Tests that users can only see documents they own, while admins can see all.
 */
@SpringBootTest(
    classes = [ShaprApplication::class, TestSecurityConfig::class, DocumentAccessControlIntegrationTest.TestAccessControlConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentAccessControlIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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

    @AfterEach
    fun cleanup() {
        orderRepository.deleteAll()
    }

    /**
     * Test configuration that provides ShaprConfig with dynamic access control.
     * Admin can see all orders, regular users can only see orders where ownerId matches their username.
     */
    @TestConfiguration
    class TestAccessControlConfig {
        @Bean
        @Primary
        fun testShaprConfig(): ShaprConfig {
            val orderCollection = CollectionDefinition(
                name = "Order",
                slug = "orders",
                fields = listOf(
                    FieldDefinition("ownerId", FieldType.Text("ownerId", required = true)),
                    FieldDefinition("description", FieldType.Text("description")),
                    FieldDefinition("total", FieldType.Number("total")),
                    FieldDefinition("status", FieldType.Text("status"))
                ),
                access = AccessControl(
                    // Allow any authenticated user to read (we'll filter dynamically)
                    read = AccessRule.Authenticated,
                    // Only admins can create
                    create = AccessRule.Roles(listOf("admin")),
                    // Dynamic read access: admin sees all, user sees only their own
                    readAccess = dynamicAccess { ctx ->
                        when {
                            ctx.isAdmin() -> AccessResult.Allow
                            !ctx.isAuthenticated() -> AccessResult.Deny
                            else -> AccessResult.Filter(whereEquals("ownerId", ctx.username))
                        }
                    }
                )
            )
            return ShaprConfig(collections = listOf(orderCollection))
        }
    }

    @Test
    fun `admin can see all orders`() {
        // Create orders with different owners
        orderRepository.save(Order(ownerId = "user", description = "User's order", total = 100.0, status = "pending"))
        orderRepository.save(Order(ownerId = "admin", description = "Admin's order", total = 200.0, status = "pending"))
        orderRepository.save(Order(ownerId = "other", description = "Other user's order", total = 300.0, status = "pending"))

        // Admin requests all orders
        val response = restTemplate.exchange(
            baseUrl("/api/orders"),
            HttpMethod.GET,
            HttpEntity<Any>(adminHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = objectMapper.readTree(response.body)
        val docs = body.get("docs")
        
        // Admin should see all 3 orders
        assertEquals(3, docs.size())
    }

    @Test
    fun `regular user can only see their own orders`() {
        // Create orders with different owners
        orderRepository.save(Order(ownerId = "user", description = "User's order 1", total = 100.0, status = "pending"))
        orderRepository.save(Order(ownerId = "user", description = "User's order 2", total = 150.0, status = "shipped"))
        orderRepository.save(Order(ownerId = "admin", description = "Admin's order", total = 200.0, status = "pending"))
        orderRepository.save(Order(ownerId = "other", description = "Other's order", total = 300.0, status = "pending"))

        // User requests all orders
        val response = restTemplate.exchange(
            baseUrl("/api/orders"),
            HttpMethod.GET,
            HttpEntity<Any>(userHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = objectMapper.readTree(response.body)
        val docs = body.get("docs")

        // User should see only their 2 orders
        assertEquals(2, docs.size())
        docs.forEach { order ->
            assertEquals("user", order.get("ownerId").asText())
        }
    }

    @Test
    fun `user with no orders sees empty list`() {
        // Create orders for other users
        orderRepository.save(Order(ownerId = "admin", description = "Admin's order", total = 200.0, status = "pending"))
        orderRepository.save(Order(ownerId = "other", description = "Other's order", total = 300.0, status = "pending"))

        // User requests orders
        val response = restTemplate.exchange(
            baseUrl("/api/orders"),
            HttpMethod.GET,
            HttpEntity<Any>(userHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = objectMapper.readTree(response.body)
        val docs = body.get("docs")

        // User should see no orders
        assertEquals(0, docs.size())
    }

    @Test
    fun `user can access their own order by ID`() {
        // Create orders
        val userOrder = orderRepository.save(Order(ownerId = "user", description = "User's order", total = 100.0, status = "pending"))
        orderRepository.save(Order(ownerId = "admin", description = "Admin's order", total = 200.0, status = "pending"))

        // User fetches their own order by ID
        val response = restTemplate.exchange(
            baseUrl("/api/orders/${userOrder.id}"),
            HttpMethod.GET,
            HttpEntity<Any>(userHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = objectMapper.readTree(response.body)
        assertEquals("user", body.get("data").get("ownerId").asText())
    }

    @Test
    fun `user cannot access another user's order by ID`() {
        // Create orders
        orderRepository.save(Order(ownerId = "user", description = "User's order", total = 100.0, status = "pending"))
        val adminOrder = orderRepository.save(Order(ownerId = "admin", description = "Admin's order", total = 200.0, status = "pending"))

        // User tries to fetch admin's order by ID
        val response = restTemplate.exchange(
            baseUrl("/api/orders/${adminOrder.id}"),
            HttpMethod.GET,
            HttpEntity<Any>(userHeaders()),
            String::class.java
        )

        // Should return 404 (not found) because access control filters it out
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `admin can access any order by ID`() {
        // Create an order owned by regular user
        val userOrder = orderRepository.save(Order(ownerId = "user", description = "User's order", total = 100.0, status = "pending"))

        // Admin fetches user's order by ID
        val response = restTemplate.exchange(
            baseUrl("/api/orders/${userOrder.id}"),
            HttpMethod.GET,
            HttpEntity<Any>(adminHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = objectMapper.readTree(response.body)
        assertEquals("user", body.get("data").get("ownerId").asText())
    }
}

