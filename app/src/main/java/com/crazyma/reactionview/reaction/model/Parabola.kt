package com.crazyma.reactionview.reaction.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

class Parabola {

    var bitmap: Bitmap? = null
    var beginX = 0
    var beginY = 0
    var endX = 0
    var endY = 0
    var controlX = 0
    var controlY = 0
    var beginSize = 0
    var endSize = 0

    var currentX = 0
    var currentY = 0
    var currentSize = 0

    private var paint = Paint().apply { isAntiAlias = true }

    init {
        initPaint()
    }

    fun draw(canvas: Canvas) {
        bitmap?.apply {
            if (!isRecycled) {
                canvas.drawBitmap(this, null, Rect(currentX, currentY, currentX + currentSize, currentY + currentSize), paint)
            }
        }
    }

    fun setupArguments(parabolaEndX: Int, parabolaEndY: Int, parabolaEndSize: Int, controlY: Int) {
        endX = parabolaEndX
        endY = parabolaEndY
        endSize = parabolaEndSize
        controlX = ((beginX + endX) / 2f).toInt()
        this.controlY = controlY
    }

    fun calculateCurrentValue(interpolatorValue: Float) {
        currentX = bezierAlg(beginX, controlX, endX, interpolatorValue).toInt()
        currentY = bezierAlg(beginY, controlY, endY, interpolatorValue).toInt()

        val changeSize = endSize - beginSize
        currentSize = beginSize + (interpolatorValue * changeSize).toInt()
    }

    private fun initPaint() {
        paint = Paint().apply {
            isAntiAlias = true
        }
    }

    private fun bezierAlg(p0: Int, p1: Int, p2: Int, t: Float) =
            (1f - t) * (1f - t) * p0 + 2f * t * (1f - t) * p1 + t * t * p2
}