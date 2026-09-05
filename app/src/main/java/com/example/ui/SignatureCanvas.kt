package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun SignatureCanvas(
    modifier: Modifier = Modifier,
    clearTrigger: Int = 0,
    onDrawStateChanged: (Bitmap?) -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var androidCanvas by remember { mutableStateOf<AndroidCanvas?>(null) }
    val path = remember { Path() }
    val androidPath = remember { AndroidPath() }
    var changeTracker by remember { mutableStateOf(0) }

    val androidPaint = remember {
        AndroidPaint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 6f
            style = AndroidPaint.Style.STROKE
            strokeJoin = AndroidPaint.Join.ROUND
            strokeCap = AndroidPaint.Cap.ROUND
            isAntiAlias = true
        }
    }

    LaunchedEffect(clearTrigger) {
        if (clearTrigger > 0) {
            path.reset()
            androidPath.reset()
            androidCanvas?.drawColor(android.graphics.Color.WHITE)
            changeTracker++
            onDrawStateChanged(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFEFEF))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        path.moveTo(offset.x, offset.y)
                        androidPath.moveTo(offset.x, offset.y)
                        changeTracker++
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val current = change.position
                        path.lineTo(current.x, current.y)
                        androidPath.lineTo(current.x, current.y)
                        androidCanvas?.drawPath(androidPath, androidPaint)
                        onDrawStateChanged(bitmap)
                        changeTracker++
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            changeTracker

            if (bitmap == null && size.width > 0 && size.height > 0) {
                val b = Bitmap.createBitmap(size.width.toInt(), size.height.toInt(), Bitmap.Config.ARGB_8888)
                val c = AndroidCanvas(b)
                c.drawColor(android.graphics.Color.WHITE)
                bitmap = b
                androidCanvas = c
            }

            drawPath(
                path = path,
                color = Color.Black,
                style = Stroke(
                    width = 5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
