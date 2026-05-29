package com.example.nothingplants.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed

@Composable
fun SoilBagIcon(
    levelPercent: Int,
    modifier: Modifier = Modifier
) {
    val animLevel by animateFloatAsState(
        targetValue = levelPercent.toFloat() / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "soilLevel"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val onBgColor = MaterialTheme.colorScheme.onBackground
        val surfaceColor = MaterialTheme.colorScheme.surface
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Contorno del sacchetto (stile stand-up pouch moderno)
            val bagPath = Path().apply {
                moveTo(w * 0.34f, h * 0.15f)
                lineTo(w * 0.66f, h * 0.15f)
                lineTo(w * 0.78f, h * 0.28f)
                lineTo(w * 0.78f, h * 0.85f)
                quadraticTo(w * 0.78f, h * 0.92f, w * 0.66f, h * 0.92f)
                quadraticTo(w * 0.50f, h * 0.94f, w * 0.34f, h * 0.92f)
                quadraticTo(w * 0.22f, h * 0.92f, w * 0.22f, h * 0.85f)
                lineTo(w * 0.22f, h * 0.28f)
                close()
            }

            // 2. Sfondo interno trasparente/opaco
            drawPath(
                path = bagPath,
                color = onBgColor.copy(alpha = 0.03f)
            )

            // 3. Terreno interno a matrice di punti (Dot Matrix texture)
            clipPath(bagPath) {
                val usableMinY = h * 0.92f
                val usableMaxY = h * 0.22f
                val soilY = usableMinY - (usableMinY - usableMaxY) * animLevel

                val dotSpacing = 8.dp.toPx()
                val dotRadius = 1.8f.dp.toPx()
                
                var currentY = usableMinY
                while (currentY > soilY) {
                    var currentX = w * 0.22f
                    while (currentX < w * 0.78f) {
                        drawCircle(
                            color = NothingRed,
                            radius = dotRadius,
                            center = androidx.compose.ui.geometry.Offset(currentX, currentY)
                        )
                        currentX += dotSpacing
                    }
                    currentY -= dotSpacing
                }
            }

            // 4. Bordo esterno del sacchetto
            drawPath(
                path = bagPath,
                color = onBgColor.copy(alpha = 0.8f),
                style = Stroke(width = 2.dp.toPx())
            )

            // 5. Sigillo termico superiore tratteggiato
            val sealLineY = h * 0.20f
            drawLine(
                color = onBgColor.copy(alpha = 0.5f),
                start = androidx.compose.ui.geometry.Offset(w * 0.34f, sealLineY),
                end = androidx.compose.ui.geometry.Offset(w * 0.66f, sealLineY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    intervals = floatArrayOf(3.dp.toPx(), 3.dp.toPx()),
                    phase = 0f
                )
            )
            
            // 6. Etichetta centrale decorativa (label)
            val labelWidth = w * 0.40f
            val labelHeight = h * 0.22f
            val labelX = (w - labelWidth) / 2
            val labelY = h * 0.42f
            
            drawRect(
                color = surfaceColor,
                topLeft = androidx.compose.ui.geometry.Offset(labelX, labelY),
                size = androidx.compose.ui.geometry.Size(labelWidth, labelHeight),
            )
            drawRect(
                color = onBgColor.copy(alpha = 0.3f),
                topLeft = androidx.compose.ui.geometry.Offset(labelX, labelY),
                size = androidx.compose.ui.geometry.Size(labelWidth, labelHeight),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Scritte all'interno della label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                text = "SOIL",
                fontFamily = NothingFontFamily,
                fontSize = 9.sp,
                color = onBgColor.copy(alpha = 0.4f)
            )
            Text(
                text = "${levelPercent}%",
                fontFamily = NothingFontFamily,
                fontSize = 15.sp,
                color = onBgColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
