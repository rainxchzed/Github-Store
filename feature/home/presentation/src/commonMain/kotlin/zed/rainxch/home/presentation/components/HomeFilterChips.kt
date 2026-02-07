package zed.rainxch.home.presentation.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import zed.rainxch.home.presentation.model.HomeCategory

@Composable
fun RowScope.HomeFilterChips(
    selectedCategory: HomeCategory,
    category: HomeCategory,
    onClick: () -> Unit,
) {
    FilterChip(
        label = {
            Text(
                text = category.displayText(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
            )
        },
        shape = CircleShape,
        onClick = onClick,
        selected = selectedCategory == category,
        modifier = Modifier.weight(1f),
        colors = FilterChipDefaults.elevatedFilterChipColors(
            containerColor = Color.Transparent,
            selectedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = null
    )
}