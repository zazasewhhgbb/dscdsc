package com.pricetracker.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pricetracker.app.R
import com.pricetracker.app.data.parser.PriceParser

/**
 * Card shown after a successful TEST LINK, before the product is saved (project rule 6).
 *
 * The target price is entered in whatever currency the page itself reported (target alerts
 * always follow the link's own currency - there is deliberately no currency picker here, the
 * currency label next to the slider/field is always the one detected from the product page).
 * A slider gives a fast, thumb-friendly way to pick a target, while the numeric field
 * underneath stays in sync for exact values - either one can be used interchangeably.
 */
@Composable
fun ProductPreviewCard(
    name: String?,
    imageUrl: String?,
    price: Double,
    currency: String?,
    targetPriceInput: String,
    onTargetPriceChanged: (String) -> Unit,
    saveError: String?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyLabel = currency.orEmpty()
    val maxTarget = price.toFloat().coerceAtLeast(1f)
    val sliderValue = (targetPriceInput.let { PriceParser.parse(it)?.toFloat() } ?: maxTarget)
        .coerceIn(0f, maxTarget)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.height(96.dp),
                    tint = Color.LightGray
                )
            }

            Text(
                text = name ?: stringResource(R.string.unknown_product),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.current_price_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatPrice(price)} $currencyLabel".trim(),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${stringResource(R.string.target_price_label)} ($currencyLabel)".trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { onTargetPriceChanged(formatPrice(it.toDouble())) },
                    valueRange = 0f..maxTarget,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetPriceInput,
                    onValueChange = onTargetPriceChanged,
                    label = { Text("${stringResource(R.string.target_price_label)} ($currencyLabel)".trim()) },
                    isError = saveError != null,
                    supportingText = saveError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(stringResource(R.string.save_product))
            }
        }
    }
}

fun formatPrice(price: Double): String {
    return if (price == price.toLong().toDouble()) {
        price.toLong().toString()
    } else {
        String.format("%.2f", price)
    }
}
