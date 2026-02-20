package br.com.firstsoft.shapr.collections

import br.com.firstsoft.shapr.dsl.schema.*
import java.time.Instant

/**
 * Blog-related collections (Posts, Categories)
 * Demonstrates: self-referencing, polymorphic relationships, blocks with multiple types
 */

@ShaprCol(name = "Post", slug = "posts")
class Post : ShaprCollection() {

    @MaxLength(200)
    var title: String = ""

    @RichText
    var content: String? = null

    var publishedAt: Instant? = null

    var views: Int = 0

    @Relationship("categories")
    var category: Long? = null

    // Polymorphic relationship - can link to posts OR pages (uses _rels table)
    @Relationship("posts", "pages")
    @HasMany
    var relatedContent: List<Long> = emptyList()

    // Blocks field with multiple block types
    @BlocksOf(HeroBlock::class, ContentBlock::class, CallToActionBlock::class)
    var layout: List<Any> = emptyList()
}

@ShaprCol(name = "Category", slug = "categories")
class Category : ShaprCollection() {

    @Unique
    var name: String = ""

    @Textarea
    var description: String? = null

    // Self-referencing relationship - category can have a parent category
    @Relationship("categories")
    var parent: Long? = null

    // HasMany self-reference - get all child categories (uses _rels table)
    @Relationship("categories")
    @HasMany
    var children: List<Long> = emptyList()
}

@ShaprCol(name = "Page", slug = "pages")
class Page : ShaprCollection() {

    var title: String = ""

    var slug: String = ""

    // Same blocks as Post - reusable block types
    @BlocksOf(HeroBlock::class, ContentBlock::class, CallToActionBlock::class)
    var layout: List<Any> = emptyList()
}

// ============================================================================
// Block Definitions - Reusable across collections
// ============================================================================

@Block(slug = "hero")
class HeroBlock : ShaprBlock() {
    var heading: String = ""
    var subheading: String? = null

    @Upload("media")
    var backgroundImage: Long? = null

    @Options("left", "center", "right")
    var alignment: String = "center"
}

@Block(slug = "content")
class ContentBlock : ShaprBlock() {
    @RichText
    var body: String = ""

    @Options("full", "narrow", "wide")
    var width: String = "full"
}

@Block(slug = "cta")
class CallToActionBlock : ShaprBlock() {
    var title: String = ""
    var buttonText: String = ""
    var buttonUrl: String = ""

    @Options("primary", "secondary", "outline")
    var style: String = "primary"
}
