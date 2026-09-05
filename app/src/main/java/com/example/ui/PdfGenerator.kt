package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.example.data.Patroli
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generatePatroliPdf(
        context: Context,
        pdfFile: File,
        headerTitle: String,
        grupName: String,
        hariTanggal: String,
        shift: String,
        petugas: String,
        records: List<Patroli>,
        pimp1Name: String,
        pimp1Title: String,
        pimp1SignPath: String?,
        pimp2Name: String,
        pimp2Title: String,
        pimp2SignPath: String?,
        pimp3Name: String,
        pimp3Title: String,
        pimp3SignPath: String?
    ): Boolean {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            var pageNumber = 1

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val textPaint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 1f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            val fillPaint = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.FILL
            }

            var currentY = 40f

            fun drawHeader() {
                // Header Title
                textPaint.textSize = 14f
                textPaint.isFakeBoldText = true
                val titleWidth = textPaint.measureText(headerTitle)
                canvas.drawText(headerTitle, (pageWidth - titleWidth) / 2, currentY, textPaint)
                currentY += 20f

                // Print date
                textPaint.textSize = 8f
                textPaint.isFakeBoldText = false
                val printDateStr = "Dicetak pada: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val dateWidth = textPaint.measureText(printDateStr)
                canvas.drawText(printDateStr, (pageWidth - dateWidth) / 2, currentY, textPaint)
                currentY += 10f

                // Header Divider Line
                canvas.drawLine(30f, currentY, pageWidth - 30f, currentY, linePaint)
                currentY += 15f
            }

            // Draw first page header
            drawHeader()

            // Draw Metadata Block
            textPaint.textSize = 9f
            val labelWidth = 100f
            val valueX = 30f + labelWidth

            fun drawMetaLine(label: String, value: String) {
                textPaint.isFakeBoldText = true
                canvas.drawText(label, 30f, currentY, textPaint)
                textPaint.isFakeBoldText = false
                canvas.drawText(": $value", valueX, currentY, textPaint)
                currentY += 15f
            }

            drawMetaLine("Hari / Tanggal", hariTanggal)
            drawMetaLine("Kategori / Grup", grupName)
            drawMetaLine("Shift Kerja", shift)
            drawMetaLine("Petugas Jaga", petugas)
            currentY += 5f

            // Table Configuration
            val colX = floatArrayOf(30f, 60f, 105f, 285f, 385f, 485f)
            val colWidths = floatArrayOf(30f, 45f, 180f, 100f, 100f, 80f)
            val colHeaders = arrayOf("No.", "Jam", "Uraian Kegiatan", "Tanda Tangan PJT", "Tanda Tangan Petugas", "Keterangan")

            // Draw Table Header
            fillPaint.color = Color.rgb(230, 230, 230)
            canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 25f, fillPaint)

            textPaint.textSize = 8f
            textPaint.isFakeBoldText = true
            for (i in colHeaders.indices) {
                val headerText = colHeaders[i]
                val textWidth = textPaint.measureText(headerText)
                val cellCenterX = colX[i] + colWidths[i] / 2
                canvas.drawText(headerText, cellCenterX - textWidth / 2, currentY + 16f, textPaint)
            }
            // Draw header grid lines
            canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 25f, linePaint)
            for (i in 1 until colX.size) {
                canvas.drawLine(colX[i], currentY, colX[i], currentY + 25f, linePaint)
            }
            currentY += 25f

            // Helper to load and scale bitmap safely
            fun drawSignatureInCell(path: String?, x: Float, y: Float, cellW: Float, cellH: Float) {
                if (path != null && File(path).exists() && File(path).length() > 0) {
                    try {
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = false
                        }
                        val bitmap = BitmapFactory.decodeFile(path, options)
                        if (bitmap != null) {
                            val maxW = cellW - 10f
                            val maxH = cellH - 6f
                            val scale = Math.min(maxW / bitmap.width, maxH / bitmap.height)
                            val destW = (bitmap.width * scale).toInt()
                            val destH = (bitmap.height * scale).toInt()
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, destW, destH, true)

                            val destX = x + (cellW - destW) / 2
                            val destY = y + (cellH - destH) / 2
                            canvas.drawBitmap(scaledBitmap, destX, destY, null)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    textPaint.textSize = 8f
                    textPaint.isFakeBoldText = false
                    val placeholderText = "-"
                    val textW = textPaint.measureText(placeholderText)
                    canvas.drawText(placeholderText, x + (cellW - textW) / 2, y + cellH / 2 + 3f, textPaint)
                }
            }

            // Draw Table Rows
            textPaint.isFakeBoldText = false
            textPaint.textSize = 8f

            for (index in records.indices) {
                val record = records[index]

                val uraianText = record.uraian
                val uraianLines = splitTextIntoLines(uraianText, textPaint, colWidths[2] - 10f)
                val textHeight = (uraianLines.size * 12f) + 10f
                val rowHeight = Math.max(45f, textHeight)

                // Page break detection
                if (currentY + rowHeight > 780f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 40f
                    drawHeader()

                    // Redraw Table Header on new page
                    fillPaint.color = Color.rgb(230, 230, 230)
                    canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 25f, fillPaint)
                    textPaint.isFakeBoldText = true
                    for (i in colHeaders.indices) {
                        val headerText = colHeaders[i]
                        val textWidth = textPaint.measureText(headerText)
                        val cellCenterX = colX[i] + colWidths[i] / 2
                        canvas.drawText(headerText, cellCenterX - textWidth / 2, currentY + 16f, textPaint)
                    }
                    canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 25f, linePaint)
                    for (i in 1 until colX.size) {
                        canvas.drawLine(colX[i], currentY, colX[i], currentY + 25f, linePaint)
                    }
                    currentY += 25f
                    textPaint.isFakeBoldText = false
                }

                // Row Box Background
                canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + rowHeight, linePaint)

                // No.
                val noStr = (index + 1).toString()
                val noW = textPaint.measureText(noStr)
                canvas.drawText(noStr, colX[0] + (colWidths[0] - noW) / 2, currentY + rowHeight / 2 + 3f, textPaint)

                // Jam
                val jamStr = record.jam
                val jamW = textPaint.measureText(jamStr)
                canvas.drawText(jamStr, colX[1] + (colWidths[1] - jamW) / 2, currentY + rowHeight / 2 + 3f, textPaint)

                // Uraian Kegiatan (Wrapped)
                var uraianY = currentY + 14f
                for (line in uraianLines) {
                    canvas.drawText(line, colX[2] + 5f, uraianY, textPaint)
                    uraianY += 12f
                }

                // TTD PJT
                drawSignatureInCell(record.ttdPjt, colX[3], currentY, colWidths[3], rowHeight)

                // TTD Petugas
                drawSignatureInCell(record.ttdPetugas, colX[4], currentY, colWidths[4], rowHeight)

                // Keterangan (Wrapped)
                val ketLines = splitTextIntoLines(record.keterangan, textPaint, colWidths[5] - 10f)
                var ketY = currentY + 14f
                for (line in ketLines) {
                    canvas.drawText(line, colX[5] + 5f, ketY, textPaint)
                    ketY += 12f
                }

                // Draw row vertical grid lines
                for (i in 1 until colX.size) {
                    canvas.drawLine(colX[i], currentY, colX[i], currentY + rowHeight, linePaint)
                }

                currentY += rowHeight
            }

            // Draw Verification Signatures at the bottom
            if (currentY + 120f > 800f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 40f
                drawHeader()
            }

            currentY += 20f

            textPaint.textSize = 9f
            textPaint.isFakeBoldText = true

            val colW = (pageWidth - 60f) / 3f
            val sigX = floatArrayOf(30f, 30f + colW, 30f + 2 * colW)

            for (i in 0 until 3) {
                val label = "Mengetahui"
                val w = textPaint.measureText(label)
                canvas.drawText(label, sigX[i] + (colW - w) / 2, currentY, textPaint)
            }
            currentY += 15f

            fun drawVerificationSign(path: String?, index: Int, y: Float) {
                if (path != null && File(path).exists() && File(path).length() > 0) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(path)
                        if (bitmap != null) {
                            val maxW = colW - 30f
                            val maxH = 40f
                            val scale = Math.min(maxW / bitmap.width, maxH / bitmap.height)
                            val destW = (bitmap.width * scale).toInt()
                            val destH = (bitmap.height * scale).toInt()
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, destW, destH, true)

                            val destX = sigX[index] + (colW - destW) / 2
                            canvas.drawBitmap(scaledBitmap, destX, y, null)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            drawVerificationSign(pimp1SignPath, 0, currentY)
            drawVerificationSign(pimp2SignPath, 1, currentY)
            drawVerificationSign(pimp3SignPath, 2, currentY)

            currentY += 45f

            textPaint.textSize = 9f
            textPaint.isFakeBoldText = true

            val name1 = if (pimp1Name.isNotEmpty()) pimp1Name else "(...........................)"
            val name2 = if (pimp2Name.isNotEmpty()) pimp2Name else "(...........................)"
            val name3 = if (pimp3Name.isNotEmpty()) pimp3Name else "(...........................)"

            val title1 = pimp1Title
            val title2 = pimp2Title
            val title3 = pimp3Title

            // Draw Name 1
            var w = textPaint.measureText(name1)
            canvas.drawText(name1, sigX[0] + (colW - w) / 2, currentY, textPaint)
            textPaint.isFakeBoldText = false
            textPaint.textSize = 8f
            w = textPaint.measureText(title1)
            canvas.drawText(title1, sigX[0] + (colW - w) / 2, currentY + 12f, textPaint)

            // Draw Name 2
            textPaint.textSize = 9f
            textPaint.isFakeBoldText = true
            w = textPaint.measureText(name2)
            canvas.drawText(name2, sigX[1] + (colW - w) / 2, currentY, textPaint)
            textPaint.isFakeBoldText = false
            textPaint.textSize = 8f
            w = textPaint.measureText(title2)
            canvas.drawText(title2, sigX[1] + (colW - w) / 2, currentY + 12f, textPaint)

            // Draw Name 3
            textPaint.textSize = 9f
            textPaint.isFakeBoldText = true
            w = textPaint.measureText(name3)
            canvas.drawText(name3, sigX[2] + (colW - w) / 2, currentY, textPaint)
            textPaint.isFakeBoldText = false
            textPaint.textSize = 8f
            w = textPaint.measureText(title3)
            canvas.drawText(title3, sigX[2] + (colW - w) / 2, currentY + 12f, textPaint)

            pdfDocument.finishPage(page)

            val fileOutputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            pdfDocument.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun splitTextIntoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
                if (paint.measureText(currentLine) > maxWidth) {
                    var tempLine = ""
                    for (char in currentLine) {
                        val tempTest = tempLine + char
                        if (paint.measureText(tempTest) <= maxWidth) {
                            tempLine = tempTest
                        } else {
                            lines.add(tempLine)
                            tempLine = char.toString()
                        }
                    }
                    currentLine = tempLine
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return if (lines.isEmpty()) listOf("-") else lines
    }
}
