package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCurrency
import com.example.data.model.DefaultCategories
import com.example.viewmodel.CategorySpending
import com.example.viewmodel.DaySpending
import com.example.viewmodel.MonthSpendingTrend
import java.util.Locale

@Composable
fun DonutPieChart(
    categorySpending: List<CategorySpending>,
    modifier: Modifier = Modifier,
    centerTitle: String = "Total Spent",
    currency: AppCurrency = AppCurrency.USD
) {
    if (categorySpending.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No spending recorded for this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val totalSpent = categorySpending.sumOf { it.amount }
    var selectedCategory by remember { mutableStateOf<CategorySpending?>(null) }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(categorySpending) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 32.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                var startAngle = -90f
                val totalAngle = 360f * animationProgress.value

                for (item in categorySpending) {
                    val sweepAngle = ((item.amount / totalSpent) * totalAngle).toFloat()
                    val color = DefaultCategories.getColorForCategory(item.category)
                    val isSelected = selectedCategory?.category == item.category

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle.coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (isSelected) strokeWidth + 8f else strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )
                    startAngle += sweepAngle
                }
            }

            // Center Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                if (selectedCategory != null) {
                    Text(
                        text = selectedCategory!!.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currency.format(selectedCategory!!.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", selectedCategory!!.percentage),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = centerTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currency.format(totalSpent),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${categorySpending.size} categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Category Legend
        CategoryLegend(
            categories = categorySpending,
            selectedCategory = selectedCategory,
            currency = currency,
            onSelect = {
                selectedCategory = if (selectedCategory?.category == it.category) null else it
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryLegend(
    categories: List<CategorySpending>,
    selectedCategory: CategorySpending?,
    modifier: Modifier = Modifier,
    currency: AppCurrency = AppCurrency.USD,
    onSelect: (CategorySpending) -> Unit
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { item ->
            val color = DefaultCategories.getColorForCategory(item.category)
            val isSelected = selectedCategory?.category == item.category

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                modifier = Modifier.clickable { onSelect(item) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currency.format(item.amount, includeDecimals = false),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DailySpendingBarChart(
    dailySpending: List<DaySpending>,
    modifier: Modifier = Modifier,
    currency: AppCurrency = AppCurrency.USD
) {
    if (dailySpending.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No daily spending data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxAmount = (dailySpending.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(50.0)
    var selectedDay by remember { mutableStateOf<DaySpending?>(null) }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dailySpending) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Top info header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedDay != null) "Day ${selectedDay!!.dayOfMonth}: ${currency.format(selectedDay!!.amount)}" else "Daily Spending Trajectory",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selectedDay != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Peak: ${currency.format(maxAmount, includeDecimals = false)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dailySpending) {
                        detectTapGestures { offset ->
                            val widthPerItem = size.width / dailySpending.size
                            val index = (offset.x / widthPerItem).toInt().coerceIn(0, dailySpending.size - 1)
                            selectedDay = dailySpending.getOrNull(index)
                        }
                    }
            ) {
                val count = dailySpending.size
                val barSpacing = 3.dp.toPx()
                val totalSpacing = barSpacing * (count - 1)
                val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2.dp.toPx())
                val chartHeight = size.height - 20.dp.toPx()

                // Draw background grid lines (3 horizontal guide lines)
                val gridColor = Color.Gray.copy(alpha = 0.15f)
                for (i in 1..3) {
                    val y = chartHeight * (i / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw bars
                for (i in 0 until count) {
                    val item = dailySpending[i]
                    val x = i * (barWidth + barSpacing)
                    val barH = if (maxAmount > 0) ((item.amount / maxAmount) * chartHeight * animationProgress.value).toFloat() else 0f
                    val y = chartHeight - barH

                    val isSelected = selectedDay?.dayOfMonth == item.dayOfMonth
                    val isPeak = item.amount >= maxAmount && maxAmount > 0

                    val barColor = when {
                        isSelected -> Color(0xFF10B981)
                        isPeak -> Color(0xFFEF4444)
                        item.amount > 0 -> Color(0xFF00B4D8)
                        else -> Color.Gray.copy(alpha = 0.15f)
                    }

                    // Bar shape
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, if (barH > 0) y else chartHeight - 2.dp.toPx()),
                        size = Size(barWidth, if (barH > 0) barH else 2.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }

        // X-Axis day labels (1, 5, 10, 15, 20, 25, 30)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val keyDays = listOf(1, 5, 10, 15, 20, 25, dailySpending.size)
            keyDays.distinct().forEach { day ->
                Text(
                    text = "D$day",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MultiMonthTrendChart(
    trends: List<MonthSpendingTrend>,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) return

    val maxVal = trends.maxOfOrNull { maxOf(it.totalExpense, it.totalIncome) }?.coerceAtLeast(100.0) ?: 100.0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "6-Month Cash Flow Trend",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            trends.forEach { month ->
                val incomeHeightPct = ((month.totalIncome / maxVal)).coerceIn(0.05, 1.0).toFloat()
                val expenseHeightPct = ((month.totalExpense / maxVal)).coerceIn(0.05, 1.0).toFloat()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.height(100.dp)
                    ) {
                        // Income Bar
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height((100 * incomeHeightPct).dp)
                                .background(Color(0xFF10B981), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                        // Expense Bar
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height((100 * expenseHeightPct).dp)
                                .background(Color(0xFFEF4444), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = month.monthLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
