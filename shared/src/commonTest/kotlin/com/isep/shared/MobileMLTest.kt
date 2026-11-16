package com.isep.shared

import com.isep.shared.domain.BarcodeText
import com.isep.shared.domain.CategorySuggester
import com.isep.shared.domain.DetectedLabel
import com.isep.shared.domain.entities.Category
import kotlin.test.Test
import kotlin.test.assertEquals

class MobileMLTest {
    @Test fun confidentBookLabelSelectsBooks() {
        assertEquals(Category.BOOKS, CategorySuggester.suggest(listOf(DetectedLabel("Paper", 0.81f))))
    }
    @Test fun weakOrUnknownLabelsFallBackToOther() {
        assertEquals(Category.OTHER, CategorySuggester.suggest(listOf(DetectedLabel("Book", 0.69f))))
        assertEquals(Category.OTHER, CategorySuggester.suggest(listOf(DetectedLabel("Unknown", 0.99f))))
    }
    @Test fun strongestRecognizedLabelWins() {
        assertEquals(Category.ELECTRONICS, CategorySuggester.suggest(listOf(DetectedLabel("Book", 0.75f), DetectedLabel("Laptop", 0.95f))))
    }
    @Test fun emptyDescriptionReceivesTheCode() {
        assertEquals("Barcode: 9780134685991", BarcodeText.appendToDescription("", "9780134685991"))
    }
    @Test fun addingACodePreservesTheDescription() {
        assertEquals("A Kotlin book\nBarcode: 9780134685991", BarcodeText.appendToDescription("A Kotlin book", "9780134685991"))
    }
    @Test fun theSameCodeIsNotAddedTwice() {
        val once = BarcodeText.appendToDescription("Book", "9780134685991")
        assertEquals(once, BarcodeText.appendToDescription(once, "9780134685991"))
    }
    @Test fun blankCodeDoesNotChangeTheDescription() {
        assertEquals("Book", BarcodeText.appendToDescription("Book", "  "))
    }
}
