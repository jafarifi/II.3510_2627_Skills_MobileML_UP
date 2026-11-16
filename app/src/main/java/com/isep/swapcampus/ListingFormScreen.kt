package com.isep.swapcampus

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isep.shared.domain.BarcodeText
import com.isep.shared.domain.CategorySuggester
import com.isep.shared.domain.entities.Category
import com.isep.shared.domain.entities.Listing
import com.isep.shared.domain.entities.label
import com.isep.shared.repository.AuthRepository
import com.isep.shared.repository.ListingRepository
import com.isep.swapcampus.ml.BarcodeHelper
import com.isep.swapcampus.ml.ImageLabelerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel shared by the Post (exercise 5) and Edit (exercise 6) forms.
 * The business rules are NOT checked here: the repository checks them
 * and this ViewModel only displays the message it receives.
 */
class ListingFormViewModel(
    private val listingRepository: ListingRepository = AppContainer.listingRepository,
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** The listing to edit, or null if it does not exist or belongs to someone else. */
    fun findOwnListing(id: Int): Listing? {
        val user = authRepository.currentUser.value ?: return null
        return listingRepository.allListings()
            .firstOrNull { it.id == id && it.author.id == user.id }
    }

    /** Returns true when the listing was saved. */
    fun save(
        listingId: Int?,
        title: String,
        description: String,
        priceText: String,
        category: Category,
        photoUrl: String
    ): Boolean {
        val user = authRepository.currentUser.value
        if (user == null) {
            _errorMessage.value = "You must be logged in."
            return false
        }
        if (listingId != null && findOwnListing(listingId) == null) {
            _errorMessage.value = "Only the owner can edit this listing."
            return false
        }
        val price = priceText.replace(',', '.').toDoubleOrNull()
        if (price == null) {
            _errorMessage.value = "The price must be a number."
            return false
        }
        val photo = photoUrl.trim().ifBlank { null }

        return try {
            if (listingId == null) {
                listingRepository.postListing(title, description, price, category, user, photo)
            } else {
                listingRepository.updateListing(listingId, title, description, price, category, photo)
            }
            _errorMessage.value = null
            true
        } catch (e: IllegalArgumentException) {
            // Message produced by the business rules in :shared.
            _errorMessage.value = e.message ?: "Invalid listing."
            false
        }
    }
}

/**
 * Form screen. listingId == null: Post a new listing.
 * Otherwise: Edit, pre-filled from the existing listing (owner only).
 */
@Composable
fun ListingFormScreen(
    listingId: Int?,
    onDone: () -> Unit,
    viewModel: ListingFormViewModel = viewModel()
) {
    val existing = remember(listingId) { listingId?.let { viewModel.findOwnListing(it) } }
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    if (listingId != null && existing == null) {
        Text(
            text = "This listing does not exist or you are not its owner.",
            modifier = Modifier.padding(16.dp)
        )
    } else {
        ListingForm(
            existing = existing,
            errorMessage = errorMessage,
            onSave = { title, description, priceText, category, photoUrl ->
                val saved = viewModel.save(listingId, title, description, priceText, category, photoUrl)
                if (saved) onDone()
            },
            onCancel = onDone
        )
    }
}

@Composable
private fun ListingForm(
    existing: Listing?,
    errorMessage: String?,
    onSave: (String, String, String, Category, String) -> Unit,
    onCancel: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(existing?.description ?: "") }
    var priceText by rememberSaveable { mutableStateOf(existing?.price?.toString() ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: Category.OTHER) }
    var photoUrl by rememberSaveable { mutableStateOf(existing?.photoUrl ?: "") }
    var suggestionText by remember { mutableStateOf<String?>(null) }
    var isAnalysing by remember { mutableStateOf(false) }
    var barcodeValues by remember { mutableStateOf<List<String>>(emptyList()) }
    var barcodeValue by rememberSaveable { mutableStateOf("") }
    var barcodeMessage by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photoUrl = uri.toString()
            isAnalysing = true
            ImageLabelerHelper.labelImage(
                context = context,
                uri = uri,
                onResult = { labels ->
                    val suggested = CategorySuggester.suggest(labels)
                    val best = labels
                        .filter { CategorySuggester.suggest(listOf(it)) == suggested }
                        .maxByOrNull { it.confidence }
                    if (suggested == Category.OTHER || best == null) {
                        suggestionText = "No confident suggestion"
                    } else {
                        category = suggested
                        suggestionText = "Suggested: ${suggested.label()} (${best.text} ${(best.confidence * 100).toInt()}%)"
                    }
                    isAnalysing = false
                },
                onError = {
                    suggestionText = "Could not analyse the image"
                    isAnalysing = false
                }
            )
        }
    }

    val pickBarcode = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            barcodeValues = emptyList()
            barcodeValue = ""
            barcodeMessage = null
            isScanning = true
            BarcodeHelper.scanImage(context, uri,
                onResult = { values ->
                    barcodeValues = values
                    barcodeValue = values.singleOrNull().orEmpty()
                    barcodeMessage = when {
                        values.isEmpty() -> "No readable barcode found"
                        values.size > 1 -> "Several codes found. Choose one."
                        else -> null
                    }
                    isScanning = false
                },
                onError = {
                    barcodeMessage = "Could not read this image. Try another photo."
                    isScanning = false
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (existing == null) "Post a listing" else "Edit listing",
            style = MaterialTheme.typography.headlineSmall
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Price (€)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Category", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Category.entries) { item ->
                FilterChip(
                    selected = category == item,
                    onClick = { category = item },
                    label = { Text(item.label()) }
                )
            }
        }
        Button(
            onClick = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            enabled = !isAnalysing && !isScanning
        ) {
            Text("Suggest category from photo")
        }
        if (isAnalysing) {
            Text("Analysing...", style = MaterialTheme.typography.bodySmall)
        } else {
            suggestionText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Button(
            onClick = { pickBarcode.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !isScanning && !isAnalysing
        ) { Text("Scan barcode from photo") }
        if (isScanning) Text("Scanning...", style = MaterialTheme.typography.bodySmall)
        barcodeMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (barcodeValues.size > 1) {
            barcodeValues.forEach { value ->
                TextButton(onClick = { barcodeValue = value }) { Text(value) }
            }
        }
        if (barcodeValue.isNotBlank()) {
            OutlinedTextField(
                value = barcodeValue,
                onValueChange = {},
                readOnly = true,
                label = { Text("Barcode / ISBN") },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { description = BarcodeText.appendToDescription(description, barcodeValue) }) {
                Text("Add to description")
            }
        }
        OutlinedTextField(
            value = photoUrl,
            onValueChange = { photoUrl = it },
            label = { Text("Photo URL (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(title, description, priceText, category, photoUrl) }) {
                Text("Save")
            }
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}
