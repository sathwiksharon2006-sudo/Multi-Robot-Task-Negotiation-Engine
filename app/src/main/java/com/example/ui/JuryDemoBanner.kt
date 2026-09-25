package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.JuryDemoState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.IndustrialBorder
import com.example.ui.theme.PurpleNegotiating
import com.example.ui.theme.RedEmergency

@Composable
fun JuryDemoBanner(
    demoState: JuryDemoState,
    onStopDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = demoState.isActive,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1035), Color(0xFF0F081C))
                    ),
                    RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                )
                .border(1.dp, PurpleNegotiating, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "MISSION STORY: JURY DEMONSTRATION",
                        fontSize = 11.sp,
                        color = Color(0xFFB388FF),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "[STEP ${demoState.currentStep} / ${demoState.totalSteps}]",
                        fontSize = 10.sp,
                        color = ElectricGoldColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onStopDemo,
                    colors = ButtonDefaults.buttonColors(containerColor = RedEmergency),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("ABORT DEMO", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Text(
                text = demoState.stepTitle,
                fontSize = 13.sp,
                color = CyanAccent,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )

            // Two-column explanation cards: "WHAT JUST HAPPENED?" and "WHY DID THE SYSTEM DO THAT?"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // What Just Happened
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF131D32), RoundedCornerShape(4.dp))
                        .border(0.5.dp, IndustrialBorder, RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "WHAT JUST HAPPENED?",
                        fontSize = 9.sp,
                        color = AmberWarning,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = demoState.whatJustHappened,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }

                // Why Did The System Do That
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF131D32), RoundedCornerShape(4.dp))
                        .border(0.5.dp, IndustrialBorder, RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "WHY DID THE SYSTEM DO THAT?",
                        fontSize = 9.sp,
                        color = GreenSuccess,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = demoState.whyDidSystemDoThat,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }
            }

            if (demoState.beforeAfterComparison.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = demoState.beforeAfterComparison,
                    fontSize = 9.5.sp,
                    color = Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1220), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { demoState.progressFraction },
                color = CyanAccent,
                trackColor = Color(0xFF311B92),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        }
    }
}

private val ElectricGoldColor = Color(0xFFFFD600)
