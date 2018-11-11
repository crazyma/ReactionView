package com.crazyma.reactionview.reaction.model

import android.graphics.*
import com.crazyma.reactionview.reaction.InterpolatedCalculator

class Emoji(var bitmap: Bitmap?, var string: String) : InterpolatedCalculator() {

    var normalSize: Int = 0

    var ratioWH = 0f
    var titleWidth = 0
    var titleBitmap: Bitmap? = null

    var currentX = 0
    var currentY = 0
    var currentSize = 0

    var beginY = 0
    var endY = 0
    var beginSize = 0
    var endSize = 0

    fun drawEmoji(canvas: Canvas, paint: Paint) {

        bitmap?.apply {
            if (!isRecycled) {
                canvas.drawBitmap(this, null, Rect(currentX, currentY, currentX + currentSize, currentY + currentSize), paint)
            }
        }

        titleBitmap?.run {
            if (currentSize > normalSize) {
                val width = titleWidth
                val height = (width / ratioWH).toInt()

                val x = currentX + (currentSize - titleWidth) / 2
                val y = currentY - height - 8

                canvas.drawBitmap(this, null, Rect(x, y, x + width, y + height), paint)
            }
        }
    }

    fun calculateCurrentY(fraction: Float) {
        currentY = calculateInterpolatedValue(beginY, endY, fraction).toInt()
    }

    fun calculateCurrentSize(fraction: Float) {
        currentSize = calculateInterpolatedValue(beginSize, endSize, fraction).toInt()
    }
}