package com.crazyma.reactionview.reaction.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.annotation.ColorInt
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class SparkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val DURATION = 400L
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val dp = Resources.getSystem().displayMetrics.density

    private val iconOriginalSize: Int = (24 * dp).toInt()

    private val dotStartDistance = 16f * dp
    private val dotEndDistance = 24f * dp
    private val dotStartRadius = 4f * dp
    private val dotEndRadius = 0f
    private val circleStartRadius = iconOriginalSize / 4f
    private val circleEndRadius = 18f * dp
    private val circleStartStrokeWidth = iconOriginalSize / 4f
    private val circleEndStrokeWidth = 0f

    //  Current Variable
    private var currentCircleRadius = 0f
    private var currentCircleStrokeWidth = circleStartStrokeWidth
    private var dotCurrentDistance = 0f
    private var biggerDotCurrentRadius = 0f
    private var smallerDotCurrentRadius = 0f

    var centerX = 0
    var centerY = 0

    fun runSpark(@ColorInt color: Int = Color.BLACK) {
        dotPaint.color = color
        circlePaint.color = color

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURATION
            interpolator = LinearInterpolator()
            addUpdateListener {
                val fraction = it.animatedValue as Float

                calculateCircleRadius(fraction)
                calculateDotDistance(fraction)
                calculateBiggerDotAppearance(fraction)
                calculateSmallerAppearance(fraction)

                postInvalidate()
            }

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    currentCircleRadius = 0f
                    postInvalidate()
                }

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {}
            })
        }.apply { start() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2
        centerY = h / 2
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // smaller dot
        for (i in 0 until 6) {
            val angdeg = 60.0 * i
            val xx = dotCurrentDistance * Math.cos(Math.toRadians(angdeg))
            val yy = dotCurrentDistance * Math.sin(Math.toRadians(angdeg))
            canvas.drawCircle(
                centerX.toFloat() + xx.toFloat(),
                centerY.toFloat() - yy.toFloat(),
                smallerDotCurrentRadius,
                dotPaint
            )
        }

        //  bigger dot
        for (i in 0 until 6) {
            val angdeg = 60.0 * i + 30.0
            val xx = dotCurrentDistance * Math.cos(Math.toRadians(angdeg))
            val yy = dotCurrentDistance * Math.sin(Math.toRadians(angdeg))
            canvas.drawCircle(
                centerX.toFloat() + xx.toFloat(),
                centerY.toFloat() - yy.toFloat(),
                biggerDotCurrentRadius,
                dotPaint
            )
        }

        circlePaint.strokeWidth = currentCircleStrokeWidth
        if (circlePaint.strokeWidth > 0f) {
            canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), currentCircleRadius, circlePaint)
        }
    }

    private fun calculateCircleRadius(fraction: Float) {
        val v = when {
            fraction <= .5f -> {
                fraction * 2f
            }
            else -> {
                1f
            }
        }

        currentCircleRadius = calculateInterpolatedValue(circleStartRadius, circleEndRadius, v)
        currentCircleStrokeWidth = calculateInterpolatedValue(circleStartStrokeWidth, circleEndStrokeWidth, v)
    }

    private fun calculateDotDistance(fraction: Float) {
        dotCurrentDistance = calculateInterpolatedValue(dotStartDistance, dotEndDistance, fraction)
    }

    private fun calculateBiggerDotAppearance(fraction: Float) {
        biggerDotCurrentRadius = calculateInterpolatedValue(dotStartRadius, dotEndRadius, fraction)
    }

    private fun calculateSmallerAppearance(fraction: Float) {
        val v = when {
            fraction <= .5f -> {
                fraction * 2f
            }
            else -> {
                1f
            }
        }
        smallerDotCurrentRadius = calculateInterpolatedValue(dotStartRadius, dotEndRadius, v)
    }

    private fun calculateInterpolatedValue(start: Float, end: Float, fraction: Float) =
        start + fraction * (end - start)
}