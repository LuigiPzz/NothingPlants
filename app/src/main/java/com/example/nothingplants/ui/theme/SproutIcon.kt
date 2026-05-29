package com.example.nothingplants.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SproutIcon: ImageVector
    get() {
        if (_sproutIcon != null) {
            return _sproutIcon!!
        }
        _sproutIcon = ImageVector.Builder(
            name = "Sprout",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1.0f,
                strokeAlpha = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(2f, 22f)
                lineTo(2f, 20f)
                curveTo(2f, 20f, 7f, 18f, 12f, 18f)
                curveTo(17f, 18f, 22f, 20f, 22f, 20f)
                lineTo(22f, 22f)
                lineTo(2f, 22f)
                close()
                moveTo(11.3f, 9.1f)
                curveTo(10.1f, 5.2f, 4f, 6.1f, 4f, 6.1f)
                curveTo(4f, 6.1f, 4.2f, 13.9f, 9.9f, 12.7f)
                curveTo(9.5f, 9.8f, 8f, 9f, 8f, 9f)
                curveTo(10.8f, 9f, 11f, 12.4f, 11f, 12.4f)
                lineTo(11f, 17f)
                curveTo(11.3f, 17f, 11.7f, 17f, 12f, 17f)
                curveTo(12.3f, 17f, 12.7f, 17f, 13f, 17f)
                lineTo(13f, 12.8f)
                curveTo(13f, 12.8f, 13f, 8.9f, 16f, 7.9f)
                curveTo(16f, 7.9f, 14f, 10.9f, 14f, 12.9f)
                curveTo(21f, 13.6f, 21f, 4f, 21f, 4f)
                curveTo(21f, 4f, 12.1f, 3f, 11.3f, 9.1f)
                close()
            }
        }.build()
        return _sproutIcon!!
    }

private var _sproutIcon: ImageVector? = null
