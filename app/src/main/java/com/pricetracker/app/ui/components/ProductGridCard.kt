package com.pricetracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pricetracker.app.R
import com.pricetracker.app.data.database.ProductEntity

/**
 * One tile in the tracked-products grid: product photo, name, current vs. target price, and a
 * small status dot. Tapping the image opens the product page; refresh/delete live in the top
 * corners so the photo stays the visual focus (project rule 29: keep it simple).
 */
@Composable
fun ProductGridCard(
    product: ProductEntity,
    isRefreshing: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickOpen(onOpen)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickOpen(onOpen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = Color.Gray)
                    }
                }

                // Status dot: green once the target price has been reached, otherwise neutral.
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(10.dp)
                        .align(Alignment.TopStart)
                        .clip(CircleShape)
                        .background(
                            when {
                                product.lastErrorMessage != null -> MaterialTheme.colorScheme.error
                                product.priceReached -> Color(0xFF2E7D32)
                                else -> Color.Transparent
                            }
                        )
                )

                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.refresh),
                                tint = Color.White,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name ?: stringResource(R.string.unknown_product),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = product.currentPrice?.let { "${formatPrice(it)} ${product.currency.orEmpty()}".trim() } ?: "—",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${stringResource(R.string.target_price_label)}: ${formatPrice(product.targetPrice)} ${product.currency.orEmpty()}".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val statusText = when {
                    product.lastErrorMessage != null -> product.lastErrorMessage
                    product.priceReached -> stringResource(R.string.status_reached)
                    else -> stringResource(R.string.status_waiting)
                }
                val statusColor = when {
                    product.lastErrorMessage != null -> MaterialTheme.colorScheme.error
                    product.priceReached -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.lastChecked?.let {
                        stringResource(R.string.last_checked_prefix, com.pricetracker.app.utils.DateUtils.formatLastChecked(it) ?: "")
                    } ?: stringResource(R.string.never_checked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun Modifier.clickOpen(onOpen: () -> Unit): Modifier =
    this.clickable(onClick = onOpen)
