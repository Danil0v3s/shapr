package br.com.firstsoft.shapr.runtime.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Jackson configuration for flexible date/time parsing.
 * Handles both ISO-8601 formats and datetime-local formats from HTML inputs.
 */
@Configuration
class JacksonConfig {

    @Bean
    @Primary
    fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper {
        val module = SimpleModule()
        module.addDeserializer(Instant::class.java, FlexibleInstantDeserializer())
        
        return builder
            .modules(JavaTimeModule(), KotlinModule.Builder().build(), module)
            .build()
    }
}

/**
 * Custom deserializer for Instant that handles multiple date formats:
 * - ISO-8601 with timezone: "2025-12-14T00:18:00Z" or "2025-12-14T00:18:00+00:00"
 * - ISO-8601 without timezone: "2025-12-14T00:18:00"
 * - datetime-local format: "2025-12-14T00:18" (assumes local timezone)
 */
class FlexibleInstantDeserializer : JsonDeserializer<Instant>() {
    
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant {
        val text = p.text.trim()
        
        // Try parsing as ISO-8601 Instant first (handles "2025-12-14T00:18:00Z")
        try {
            return Instant.parse(text)
        } catch (e: DateTimeParseException) {
            // Continue to try other formats
        }
        
        // Try parsing as ISO-8601 with offset (handles "2025-12-14T00:18:00+00:00")
        try {
            return java.time.OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        } catch (e: DateTimeParseException) {
            // Continue
        }
        
        // Try parsing as LocalDateTime with seconds (handles "2025-12-14T00:18:00")
        try {
            val localDateTime = LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant()
        } catch (e: DateTimeParseException) {
            // Continue
        }
        
        // Try parsing as datetime-local format without seconds (handles "2025-12-14T00:18")
        try {
            val localDateTime = LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant()
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Cannot deserialize value of type `java.time.Instant` from String \"$text\": ${e.message}")
        }
    }
}
