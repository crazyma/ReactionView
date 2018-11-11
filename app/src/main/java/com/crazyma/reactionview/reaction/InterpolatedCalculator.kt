package com.crazyma.reactionview.reaction

/**
 * LinearInterpolator: f = fraction
 * AccelerateDecelerateInterpolator: f = (Math.cos((fraction + 1) * Math.PI) / 2.0f).toFloat() +
 * 0.5f
 */
open class InterpolatedCalculator {

    protected fun calculateInterpolatedValue(start: Float, end: Float, fraction: Float): Float {
        return start + fraction * (end - start)
    }

    protected fun calculateInterpolatedValue(start: Int, end: Int, fraction: Float) =
            calculateInterpolatedValue(start.toFloat(), end.toFloat(), fraction)
}