package com.isep.shared.domain

import com.isep.shared.domain.entities.Category

data class DetectedLabel(val text: String, val confidence: Float)

object CategorySuggester {

    const val MIN_CONFIDENCE = 0.7f

    // Keys are lowercase so the lookup ignores case.
    private val labelToCategory = mapOf(
        "book" to Category.BOOKS,
        "paper" to Category.BOOKS,
        "notebook" to Category.BOOKS,

        "mobile phone" to Category.ELECTRONICS,
        "computer" to Category.ELECTRONICS,
        "laptop" to Category.ELECTRONICS,
        "headphones" to Category.ELECTRONICS,
        "television" to Category.ELECTRONICS,
        "camera" to Category.ELECTRONICS,

        "bicycle" to Category.SPORTS_LEISURE,
        "ball" to Category.SPORTS_LEISURE,
        "sports equipment" to Category.SPORTS_LEISURE,
        "skateboard" to Category.SPORTS_LEISURE,

        "chair" to Category.FURNITURE,
        "table" to Category.FURNITURE,
        "couch" to Category.FURNITURE,
        "bed" to Category.FURNITURE,
        "shelf" to Category.FURNITURE,
        "desk" to Category.FURNITURE,

        "shirt" to Category.CLOTHING,
        "shoe" to Category.CLOTHING,
        "jacket" to Category.CLOTHING,
        "jeans" to Category.CLOTHING,
        "bag" to Category.CLOTHING,
        "sunglasses" to Category.CLOTHING
    )

    fun suggest(labels: List<DetectedLabel>): Category {
        // TODO 1: keep labels with confidence >= MIN_CONFIDENCE, sort them,
        // then return the first category found in labelToCategory, or OTHER.
        return Category.OTHER
    }
}
