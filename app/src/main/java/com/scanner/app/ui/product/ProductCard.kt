package com.scanner.app.ui.product

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scanner.app.data.ProductFlags
import com.scanner.app.R

@Composable
fun ProductCard(flags: ProductFlags?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = flags != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        flags?.let {
            Card(
                modifier = modifier.fillMaxWidth().padding(16.dp).animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (it.rating > 0) stringResource(R.string.rating, it.rating)
                               else stringResource(R.string.rating, it.rating) + " (нет оценки)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))

                    FlagItem(Icons.Filled.LocalCafe, stringResource(R.string.sugar), it.sugar)
                    FlagItem(Icons.Filled.Grain, stringResource(R.string.gluten), it.gluten)
                    FlagItem(Icons.Filled.WaterDrop, stringResource(R.string.lactose), it.lactose)
                    FlagItem(Icons.Filled.Nature, stringResource(R.string.palm_oil), it.palmOil)
                    FlagItem(Icons.Filled.Warning, stringResource(R.string.hazardous_e), it.hazardousE)
                    FlagItem(Icons.Filled.Science, stringResource(R.string.gmo), it.gmo)
                    FlagItem(Icons.Filled.Blender, stringResource(R.string.milk_fat_replacer), it.milkFatReplacer)
                    FlagItem(Icons.Filled.Colorize, stringResource(R.string.artificial_colors), it.artificialColors)

                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))

                    Text(stringResource(R.string.traffic_lights), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TrafficLightCircle(
                            label = if (it.sugarLevel > 0) stringResource(R.string.sugar_level, it.sugarLevel)
                                    else stringResource(R.string.no_data),
                            level = it.sugarLevel
                        )
                        TrafficLightCircle(
                            label = if (it.saltLevel > 0) stringResource(R.string.salt_level, it.saltLevel)
                                    else stringResource(R.string.no_data),
                            level = it.saltLevel
                        )
                        TrafficLightCircle(
                            label = if (it.fatLevel > 0) stringResource(R.string.fat_level, it.fatLevel)
                                    else stringResource(R.string.no_data),
                            level = it.fatLevel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagItem(icon: ImageVector, label: String, flagged: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (flagged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, color = if (flagged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TrafficLightCircle(label: String, level: Int) {
    val fraction = if (level > 0) level / 15f else 0f
    val color = if (level > 0) lerp(Color(0xFF4CAF50), Color(0xFFF44336), fraction) else Color.Gray
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(32.dp)) {}
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun lerp(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = 1f
    )
}
