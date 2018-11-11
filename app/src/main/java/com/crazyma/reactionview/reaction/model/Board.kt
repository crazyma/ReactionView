package com.crazyma.reactionview.reaction.model

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.support.v4.content.ContextCompat
import com.crazyma.reactionview.reaction.InterpolatedCalculator

class Board(context: Context) : InterpolatedCalculator() {

    var x = 0f
    var width = 0f
    var currentBottomY = 0f
    var beginBottomY = 0f
    var endBottomY = 0f

    var currentHeight = 0f
    var beginHeight = 0f
    var endHeight = 0f

    var currentAlpha = 0
    var beginAlpha = 0
    var endAlpha = 0

    private lateinit var paint: Paint
    private lateinit var shadowPaint: Paint

    init {
        initPaint(context)
    }

    fun drawBoard(canvas: Canvas) {
        val radius = currentHeight / 2

        val shadowBoard = RectF(x, currentBottomY - currentHeight + 2, x + width, currentBottomY + 2)
        shadowPaint.alpha = if (currentAlpha < 204) currentAlpha else 204
        canvas.drawRoundRect(shadowBoard, radius, radius, shadowPaint)

        val board = RectF(x, currentBottomY - currentHeight, x + width, currentBottomY)
        paint.alpha = currentAlpha
        canvas.drawRoundRect(board, radius, radius, paint)
    }

    fun calculateCurrentBottomY(fraction: Float) {
        currentBottomY = calculateInterpolatedValue(beginBottomY, endBottomY, fraction)
    }

    fun calculateCurrentSize(fraction: Float) {
        currentHeight = calculateInterpolatedValue(beginHeight, endHeight, fraction)
    }

    fun calculateCurrentAlpha(fraction: Float) {
        currentAlpha = calculateInterpolatedValue(beginAlpha, endAlpha, fraction).toInt()
    }

    private fun initPaint(context: Context) {
        paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, android.R.color.white)
        }

        shadowPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, android.R.color.darker_gray)
        }
    }
}