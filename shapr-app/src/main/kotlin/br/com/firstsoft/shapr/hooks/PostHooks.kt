package br.com.firstsoft.shapr.hooks

import br.com.firstsoft.shapr.dsl.hooks.*
import br.com.firstsoft.shapr.generated.entity.Post
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Example hook implementation for the Post collection.
 * This demonstrates how to use the hooks system with dependency injection.
 * 
 * The hook is automatically discovered by HookRegistry at application startup
 * and will be used for all Post collection operations.
 */
@Component
class PostHooks : AbstractCollectionHooks<Post>() {
    
    /**
     * Example: Automatically set updatedAt timestamp before saving
     */
    override suspend fun beforeChange(args: BeforeChangeArgs<Post>): Post {
        val now = Instant.now()
        return args.data.copy(
            updatedAt = now,
            // Also set createdAt if this is a new post (create operation)
            createdAt = if (args.operation == HookOperationType.CREATE && args.data.createdAt == null) {
                now
            } else {
                args.data.createdAt
            }
        )
    }
    
    /**
     * Example: Uppercase the title before saving
     */
    override suspend fun beforeValidate(args: BeforeValidateArgs<Post>): Post? {
        val data = args.data ?: return null
        return data.copy(
            title = data.title.uppercase()
        )
    }
    
    /**
     * Example: Log after a post is created or updated
     */
    override suspend fun afterChange(args: AfterChangeArgs<Post>): Post {
        println("Post ${args.doc.id} was ${args.operation.name.lowercase()}d: ${args.doc.title}")
        return args.doc
    }
    
    /**
     * Example: Increment view count when reading a post
     */
    override suspend fun afterRead(args: AfterReadArgs<Post>): Post {
        // In a real application, you might want to update the view count in the database
        // For this example, we just return the document as-is
        return args.doc
    }
    
    /**
     * Example: Prevent deletion of posts with high view count
     */
    override suspend fun beforeDelete(args: BeforeDeleteArgs) {
        // This is just an example - in a real app you'd fetch the document first
        // and check its properties
        println("Attempting to delete post with id: ${args.id}")
    }
    
    /**
     * Example: Log after deletion
     */
    override suspend fun afterDelete(args: AfterDeleteArgs<Post>) {
        println("Post ${args.id} (${args.doc.title}) was deleted")
    }
}
