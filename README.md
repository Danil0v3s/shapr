# Shapr

Shapr is a Kotlin-based Content Management System (CMS) framework that provides a PayloadCMS-inspired API and admin interface, built on top of Spring Boot and JPA. It combines the developer experience of PayloadCMS with the power and type safety of Kotlin and the enterprise-grade infrastructure of Spring Boot.

## Overview

Shapr allows you to define content collections using a declarative Kotlin DSL, automatically generates JPA entities, repositories, and REST controllers, and provides a modern Next.js-based admin UI. It follows many of the same patterns and conventions as PayloadCMS, making it familiar to developers who have worked with Payload, while leveraging the benefits of the JVM ecosystem.

## Key Features

### Declarative Collection Definition

Define your content collections using a type-safe Kotlin DSL. Collections are defined in a single configuration file, and Shapr handles the rest:

```kotlin
val collections = shapr {
    collection("Post") {
        slug = "posts"
        
        access {
            create = public()
            read = public()
            update = roles("admin", "editor")
            delete = roles("admin")
        }
        
        fields {
            text("title") {
                required = true
                maxLength = 200
            }
            textarea("content")
            date("publishedAt")
            relationship("category") {
                relationTo = "categories"
            }
        }
        
        admin {
            useAsTitle = "title"
            defaultColumns = listOf("id", "title", "publishedAt")
        }
    }
}
```

### Automatic Code Generation

Shapr includes a Gradle plugin that automatically generates:
- JPA Entity classes with proper annotations
- Spring Data JPA Repository interfaces
- REST Controllers with full CRUD operations
- Type-safe query builders

The generated code is placed in a build directory and integrated seamlessly into your application. This means you write less boilerplate code and focus on your business logic.

### Payload-Style Query API

Shapr provides a REST API that closely matches PayloadCMS query patterns:

```
GET /api/posts?where={"title":{"equals":"My Post"}}&limit=10&page=1&sort=-createdAt
```

The query API supports:
- Complex where clauses with nested conditions
- Pagination with metadata
- Sorting (ascending and descending)
- Field selection and population
- Relationship traversal

### Access Control

Define fine-grained access control rules for each collection operation:

- **Public**: No authentication required
- **Authenticated**: Any logged-in user
- **Roles**: Specific roles required (e.g., "admin", "editor")
- **Deny**: Explicitly deny access

Access control is enforced at the controller level and can be customized per collection and per operation (create, read, update, delete).

### Hooks System

Shapr implements a comprehensive hooks system similar to PayloadCMS, allowing you to intercept and modify data at various stages of the lifecycle:

- **beforeOperation**: Called before any operation, can cancel the operation
- **beforeValidate**: Modify data before validation
- **beforeChange**: Last chance to modify data before persistence
- **afterChange**: Called after data is saved
- **beforeRead**: Modify documents before they're returned
- **afterRead**: Modify documents after they're read
- **beforeDelete**: Called before deletion
- **afterDelete**: Called after deletion

Hooks are implemented as Spring components and are automatically discovered:

```kotlin
@Component
class PostHooks : AbstractCollectionHooks<Post>() {
    override suspend fun beforeChange(args: BeforeChangeArgs<Post>): Post {
        return args.data.copy(
            slug = args.data.title.lowercase().replace(" ", "-")
        )
    }
}
```

### Admin UI

Shapr includes a Next.js-based admin interface that automatically adapts to your collection definitions. The UI provides:

- Dashboard with collection overview
- Dynamic forms based on field definitions
- List views with sorting and filtering
- Relationship management
- Schema introspection via API

The admin UI fetches collection schemas from the backend and dynamically renders forms and tables based on your field definitions.

### Field Types

Shapr supports a comprehensive set of field types:

- **Text**: Single-line text input with length constraints
- **Textarea**: Multi-line text input
- **Number**: Numeric values (integer or decimal)
- **Checkbox**: Boolean values
- **Email**: Email addresses with validation
- **Date**: Date and timestamp fields
- **Relationship**: References to other collections (one-to-one or one-to-many)

Each field type supports validation rules, default values, and admin UI configuration.

## Architecture

Shapr is organized into several modules:

### shapr-dsl

The Domain-Specific Language module that defines the collection configuration API. This module contains:

- Collection and field definition data classes
- Builder DSL for defining collections
- Access control rule definitions
- Query API data structures (Where, FindOptions, etc.)
- Hook interfaces and argument types

### shapr-runtime

The runtime module that provides the core functionality:

- REST controllers (QueryController, SchemaController)
- Query service with Payload-style query execution
- Hook executor for lifecycle hooks
- Access control utilities
- Spring Boot auto-configuration
- JPA query builders and converters

### shapr-codegen

A Gradle plugin that generates code from collection definitions:

- Entity generators (JPA entities with proper annotations)
- Repository generators (Spring Data JPA repositories)
- Controller generators (REST controllers with CRUD operations)

The plugin reads your collection definitions and generates type-safe, production-ready code.

### shapr-app

Your application module where you:

- Define your collections using the DSL
- Configure Spring Boot application
- Add custom business logic and hooks
- Configure database connections

### shapr-ui

A Next.js application that provides the admin interface:

- React Server Components for server-side rendering
- Dynamic form generation based on schemas
- Table views with pagination
- Schema provider for type-safe API calls

## Comparison with PayloadCMS

Shapr shares many concepts and patterns with PayloadCMS, but there are key differences:

### Similarities

- **Collection-based architecture**: Both use collections to define content types
- **DSL for configuration**: Both use declarative configuration (TypeScript config in Payload, Kotlin DSL in Shapr)
- **Query API**: Shapr implements a Payload-compatible query API with where clauses, pagination, and sorting
- **Hooks system**: Both provide lifecycle hooks for data manipulation
- **Access control**: Similar access control patterns (public, authenticated, roles)
- **Admin UI**: Both provide auto-generated admin interfaces
- **Field types**: Similar field type systems with validation

### Differences

**Language and Runtime**

- **PayloadCMS**: Built with TypeScript/JavaScript, runs on Node.js
- **Shapr**: Built with Kotlin, runs on JVM (Java/Kotlin)

**Database Layer**

- **PayloadCMS**: Uses database adapters (MongoDB, Postgres, etc.) with Drizzle ORM
- **Shapr**: Uses JPA (Java Persistence API) with Spring Data JPA, supports any JPA-compatible database

**Code Generation**

- **PayloadCMS**: Runtime schema interpretation, dynamic query building
- **Shapr**: Compile-time code generation, generates actual JPA entities and repositories

**Type Safety**

- **PayloadCMS**: TypeScript provides type safety at compile time
- **Shapr**: Kotlin provides type safety with null safety and sealed classes

**Framework Integration**

- **PayloadCMS**: Next.js native, integrates directly with Next.js app directory
- **Shapr**: Spring Boot native, integrates with Spring ecosystem (Security, Data, Web)

**Deployment**

- **PayloadCMS**: Deployed as Next.js application (serverless or traditional hosting)
- **Shapr**: Deployed as Spring Boot application (JAR or WAR, traditional JVM hosting)

**Performance**

- **PayloadCMS**: Node.js event loop, good for I/O-bound operations
- **Shapr**: JVM with mature optimization, better for CPU-intensive operations and long-running applications

**Ecosystem**

- **PayloadCMS**: NPM ecosystem, JavaScript/TypeScript libraries
- **Shapr**: Maven/Gradle ecosystem, JVM libraries (Spring, Hibernate, etc.)

## Getting Started

### Prerequisites

- Java 21 or higher
- Gradle 8.0 or higher
- Node.js 18+ and npm/yarn (for admin UI)

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd shapr
```

2. Build the project:
```bash
./gradlew build
```

3. Define your collections in `shapr-app/src/main/kotlin/br/com/firstsoft/shapr/collections/Collections.kt`

4. Run the code generator:
```bash
./gradlew generateShaprCode
```

5. Start the Spring Boot application:
```bash
./gradlew :shapr-app:bootRun
```

6. Start the admin UI (in a separate terminal):
```bash
cd shapr-ui
yarn install
yarn dev
```

### Configuration

Configure your database connection in `shapr-app/src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/shapr
    username: your-username
    password: your-password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

## Project Structure

```
shapr/
├── shapr-dsl/              # DSL definitions and builders
├── shapr-runtime/          # Runtime services and controllers
├── shapr-codegen/          # Gradle plugin for code generation
├── shapr-app/              # Your application (collections, hooks)
└── shapr-ui/               # Next.js admin interface
```

## Development

### Adding a New Collection

1. Add collection definition to `Collections.kt`:
```kotlin
collection("Product") {
    slug = "products"
    fields {
        text("name") { required = true }
        number("price") { required = true }
    }
}
```

2. Run code generation:
```bash
./gradlew generateShaprCode
```

3. The generated code will be available in `shapr-app/build/generated/sources/shapr/`

### Adding Custom Hooks

Create a Spring component that implements `CollectionHooks<T>`:

```kotlin
@Component
class ProductHooks : AbstractCollectionHooks<Product>() {
    override suspend fun beforeChange(args: BeforeChangeArgs<Product>): Product {
        // Custom logic here
        return args.data
    }
}
```

### Customizing Access Control

Access control is defined per collection in the DSL:

```kotlin
collection("Post") {
    access {
        create = roles("admin", "editor")
        read = public()
        update = authenticated()
        delete = roles("admin")
    }
}
```

## API Endpoints

### Query API

```
GET /api/{collection}?where={json}&limit=10&page=1&sort=-createdAt
```

### CRUD Operations

```
GET    /api/{collection}              # List all
GET    /api/{collection}/{id}         # Get by ID
POST   /api/{collection}              # Create
PUT    /api/{collection}/{id}         # Update
DELETE /api/{collection}/{id}         # Delete
```

### Schema API

```
GET /api/_schema                      # Get all collection schemas
GET /api/_schema/{slug}                # Get specific collection schema
```
