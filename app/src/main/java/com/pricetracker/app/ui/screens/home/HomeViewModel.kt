package com.pricetracker.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricetracker.app.data.database.ProductEntity
import com.pricetracker.app.data.repository.CheckOutcome
import com.pricetracker.app.data.repository.ProductRepository
import com.pricetracker.app.data.repository.SaveProductResult
import com.pricetracker.app.domain.PriceCheckOutcome
import com.pricetracker.app.domain.PriceChecker
import com.pricetracker.app.domain.UrlNormalizer
import com.pricetracker.app.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State of the "paste a URL / test it" flow at the top of the home screen. */
sealed class LinkTestState {
    data object Idle : LinkTestState()
    data object Loading : LinkTestState()
    data class Preview(
        val url: String,
        val name: String?,
        val imageUrl: String?,
        val price: Double,
        val currency: String?
    ) : LinkTestState()
    data class Error(val message: String) : LinkTestState()
    data object AlreadyTracked : LinkTestState()
}

data class HomeUiState(
    val urlInput: String = "",
    val linkTestState: LinkTestState = LinkTestState.Idle,
    val targetPriceInput: String = "",
    val saveError: String? = null,
    val products: List<ProductEntity> = emptyList(),
    val refreshingProductIds: Set<Long> = emptySet(),
    val isAddSheetOpen: Boolean = false
)

class HomeViewModel(
    private val repository: ProductRepository,
    private val priceChecker: PriceChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeProducts().collect { products ->
                _uiState.value = _uiState.value.copy(products = products)
            }
        }
    }

    fun openAddSheet() {
        _uiState.value = _uiState.value.copy(
            isAddSheetOpen = true,
            urlInput = "",
            targetPriceInput = "",
            saveError = null,
            linkTestState = LinkTestState.Idle
        )
    }

    fun closeAddSheet() {
        _uiState.value = _uiState.value.copy(
            isAddSheetOpen = false,
            urlInput = "",
            targetPriceInput = "",
            saveError = null,
            linkTestState = LinkTestState.Idle
        )
    }

    fun onUrlInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(urlInput = value, linkTestState = LinkTestState.Idle)
    }

    fun onTargetPriceInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(targetPriceInput = value, saveError = null)
    }

    fun testLink() {
        val url = _uiState.value.urlInput.trim()
        if (!UrlNormalizer.isValid(url)) {
            _uiState.value = _uiState.value.copy(
                linkTestState = LinkTestState.Error("That doesn't look like a valid URL.")
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(linkTestState = LinkTestState.Loading)

            val existing = repository.findExistingByUrl(url)
            if (existing != null) {
                _uiState.value = _uiState.value.copy(linkTestState = LinkTestState.AlreadyTracked)
                return@launch
            }

            when (val outcome = priceChecker.checkProduct(url)) {
                is PriceCheckOutcome.Success -> {
                    _uiState.value = _uiState.value.copy(
                        linkTestState = LinkTestState.Preview(
                            url = url,
                            name = outcome.name,
                            imageUrl = outcome.imageUrl,
                            price = outcome.price,
                            currency = outcome.currency
                        ),
                        targetPriceInput = ""
                    )
                }
                is PriceCheckOutcome.Error -> {
                    _uiState.value = _uiState.value.copy(linkTestState = LinkTestState.Error(outcome.message))
                }
            }
        }
    }

    fun saveProduct() {
        val preview = _uiState.value.linkTestState as? LinkTestState.Preview ?: return
        val targetRaw = _uiState.value.targetPriceInput

        val target = com.pricetracker.app.data.parser.PriceParser.parse(targetRaw)
        if (target == null || target <= 0.0) {
            _uiState.value = _uiState.value.copy(saveError = "Enter a valid target price")
            return
        }

        viewModelScope.launch {
            val result = repository.saveNewProduct(
                url = preview.url,
                name = preview.name,
                imageUrl = preview.imageUrl,
                currentPrice = preview.price,
                currency = preview.currency,
                targetPrice = target
            )
            when (result) {
                is SaveProductResult.Saved -> {
                    _uiState.value = _uiState.value.copy(
                        urlInput = "",
                        targetPriceInput = "",
                        linkTestState = LinkTestState.Idle,
                        saveError = null,
                        isAddSheetOpen = false
                    )
                }
                SaveProductResult.DuplicateUrl -> {
                    _uiState.value = _uiState.value.copy(linkTestState = LinkTestState.AlreadyTracked)
                }
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch { repository.deleteProduct(product) }
    }

    fun refreshProduct(product: ProductEntity, onTargetReached: (ProductEntity) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                refreshingProductIds = _uiState.value.refreshingProductIds + product.id
            )
            val outcome = repository.refreshProduct(product)
            if (outcome is CheckOutcome.Updated && outcome.shouldNotify) {
                onTargetReached(outcome.product)
            }
            _uiState.value = _uiState.value.copy(
                refreshingProductIds = _uiState.value.refreshingProductIds - product.id
            )
        }
    }
}
