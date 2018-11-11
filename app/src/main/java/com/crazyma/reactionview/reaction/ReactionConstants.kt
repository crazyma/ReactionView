package com.crazyma.reactionview.reaction

import android.support.annotation.IntDef

object ReactionConstants {

    object Reaction {
        const val ACTION_BAR = 0x10
        const val BOTTOM_BAR = 0x11

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(ACTION_BAR, BOTTOM_BAR)
        annotation class LaunchedBy
    }

    const val SPACING = 8
    const val SIZE_SMALL = 28
    const val SIZE_LARGE = 68
    const val DURATION_TRANSACTION = 120L
    const val DURATION_HOVER = 120L
    const val DURATION_PARABOLA = 250L
    const val REACTION_LIKE_ID = "286f599c-f86a-4932-82f0-f5a06f1eca03"

    fun getReactionViewSize(size: Int) =
            SIZE_LARGE + (size - 1) * SIZE_SMALL + (size + 1) * SPACING

    fun getNormalSize(size: Int) =
            (SIZE_LARGE + (size - 1) * SIZE_SMALL) / size
}