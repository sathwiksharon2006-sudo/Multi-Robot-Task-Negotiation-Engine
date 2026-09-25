package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EventCategory
import com.example.model.EventLevel
import com.example.model.SimulationEvent
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndustrialBorder
import com.example.ui.theme.IndustrialSurface
import com.example.ui.theme.PurpleNegotiating
import com.example.ui.theme.RedEmergency

@Composable
fun EventLogView(
    events: List<SimulationEvent>,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(EventCategory.ALL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IndustrialSurface)
            .border(1.dp, IndustrialBorder)
            .padding(8.dp)
    ) {
        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EventCategory.values().forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category.name,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                        selectedLabelColor = CyanAccent,
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val filteredEvents = if (selectedCategory == EventCategory.ALL) {
            events
        } else {
            events.filter { it.category == selectedCategory }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredEvents, key = { it.id }) { event ->
                EventRow(event = event)
            }
        }
    }
}

@Composable
private fun EventRow(event: SimulationEvent) {
    val levelColor = when (event.level) {
        EventLevel.INFO -> CyanAccent
        EventLevel.WARNING -> AmberWarning
        EventLevel.ERROR -> RedEmergency
        EventLevel.SUCCESS -> GreenSuccess
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0F1D), RoundedCornerShape(3.dp))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = "[${event.formattedTime}]",
            fontSize = 10.sp,
            color = Color(0xFF64748B),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(62.dp)
        )

        // Status indicator dot
        Box(
            modifier = Modifier
                .padding(top = 4.dp, end = 6.dp)
                .size(6.dp)
                .background(levelColor, CircleShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = event.category.name,
                    fontSize = 8.sp,
                    color = Color(0xFF475569),
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = event.description,
                fontSize = 10.sp,
                color = Color(0xFFCBD5E1),
                fontFamily = FontFamily.Monospace,
                lineHeight = 13.sp
            )
        }
    }
}
