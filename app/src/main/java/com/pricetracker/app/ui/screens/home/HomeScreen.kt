package com.pricetracker.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pricetracker.app.R
import com.pricetracker.app.notifications.NotificationHelper
import com.pricetracker.app.ui.components.AddProductTile
import com.pricetracker.app.ui.components.ProductGridCard
import com.pricetracker.app.ui.components.ProductPreviewCard

/**
 * Home screen: a grid of tracked products (each its own photo tile), with an "Add product" tile
 * as the very first cell. Tapping it opens a bottom sheet where the user pastes a URL, tests it,
 * and sets a target price - once saved, the sheet closes and the new tile appears in the grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        if (uiState.products.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.no_tracked_products),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                AddProductTile(onClick = viewModel::openAddSheet, modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item { AddProductTile(onClick = viewModel::openAddSheet) }

                items(uiState.products, key = { it.id }) { product ->
                    ProductGridCard(
                        product = product,
                        isRefreshing = product.id in uiState.refreshingProductIds,
                        onOpen = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(product.url)))
                        },
                        onDelete = { viewModel.deleteProduct(product) },
                        onRefresh = {
                            viewModel.refreshProduct(product) { reachedProduct ->
                                NotificationHelper.createChannelIfNeeded(context)
                                NotificationHelper.sendPriceAlert(context, reachedProduct)
                            }
                        }
                    )
                }
            }
        }
    }

    if (uiState.isAddSheetOpen) {
        ModalBottomSheet(onDismissRequest = viewModel::closeAddSheet, sheetState = rememberModalBottomSheetState()) {
            AddProductSheetContent(uiState = uiState, viewModel = viewModel)
        }
    }
}

@Composable
private fun AddProductSheetContent(uiState: HomeUiState, viewModel: HomeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.add_product_sheet_title), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = viewModel::closeAddSheet) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
            }
        }

        OutlinedTextField(
            value = uiState.urlInput,
            onValueChange = viewModel::onUrlInputChanged,
            label = { Text(stringResource(R.string.url_input_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = viewModel::testLink,
            enabled = uiState.urlInput.isNotBlank() && uiState.linkTestState !is LinkTestState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.test_link))
        }

        when (val state = uiState.linkTestState) {
            is LinkTestState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.checking_product), modifier = Modifier.padding(top = 8.dp))
                }
            }
            is LinkTestState.Preview -> {
                ProductPreviewCard(
                    name = state.name,
                    imageUrl = state.imageUrl,
                    price = state.price,
                    currency = state.currency,
                    targetPriceInput = uiState.targetPriceInput,
                    onTargetPriceChanged = viewModel::onTargetPriceInputChanged,
                    saveError = uiState.saveError,
                    onSave = viewModel::saveProduct
                )
            }
            is LinkTestState.Error -> {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
            is LinkTestState.AlreadyTracked -> {
                Text(
                    text = stringResource(R.string.duplicate_product_message),
                    color = MaterialTheme.colorScheme.error
                )
            }
            LinkTestState.Idle -> Unit
        }
    }
}
