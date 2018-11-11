package com.crazyma.reactionview.reaction.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.crazyma.reactionview.R
import com.crazyma.reactionview.reaction.model.Emoji
import com.crazyma.reactionview.reaction.ReactionConstants

class ReactionFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val REACTION_LEFT_DISPLAY_BOUND = 0

        private const val RIGHT_BOUND_RATIO = .5f
        private const val BOTTOM_BOUND_RATIO = .2f
    }

    interface CustomTouchEventListener {
        fun onHandleTouchEvent(event: MotionEvent)
    }

    interface ReactionViewDisappearListener {
        fun onReactionViewDisappear(
            @ReactionConstants.Reaction.LaunchedBy launchedBy: Int, selectedIndex: Int
        )
    }

    var reactionViewDisappearListener: ReactionViewDisappearListener? = null
    private var customTouchEventListener: CustomTouchEventListener? = null

    /**
     *   If the touch area is on the right side of this bound, which means that touch.X >
     *   reactionRightBound, then display the [ReactionView] on the left side of the touch.X.
     *   Otherwise, display it on the right side.
     */
    private var reactionRightBound = 0

    /**
     *   If the touch area is below this bound, which means that touch.Y >
     *   reactionTopBound, then display the [ReactionView] above the touch.X.
     *   Otherwise, display it below side.
     */
    private var reactionBottomBound = 0

    private var reactionViewWidth = 0
    private var reactionViewHeight = 0

    /**
     * If the left margin is too left, which is over the [REACTION_LEFT_DISPLAY_BOUND], then
     * adjust the display left margin and modified the end position of parabola animation
     */
    private var adjustOffsetOfScreen = 0

    private var interruptingTouchEvent = false
    private var reactionView: ReactionView? = null
    @ReactionConstants.Reaction.LaunchedBy
    private var launchedBy: Int = 0
    private val dp = Resources.getSystem().displayMetrics.density

    init {
        reactionViewWidth = context.resources.getDimensionPixelSize(R.dimen.width_reaction)
        reactionViewHeight = context.resources.getDimensionPixelSize(R.dimen.height_reaction)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        reactionRightBound = (w * RIGHT_BOUND_RATIO).toInt()
        reactionBottomBound = (h * BOTTOM_BOUND_RATIO).toInt()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {

        if (interruptingTouchEvent) {
            return true
        }

        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        customTouchEventListener?.onHandleTouchEvent(event)

        return true
    }

    fun calculateReactionViewSize() {
        reactionView?.run {
            calculateSize()
        }
    }

    fun addEmoji(emoji: Emoji) {
        reactionView?.run {
            addEmoji(emoji)
        }
    }

    fun cleanEmoji() {
        reactionView?.run {
            cleanEmoji()
        }
    }

    fun isReactionViewReady() = reactionView?.isReady() == true

    fun stopInterruptTouchEvent() {
        interruptingTouchEvent = false
    }

    fun selectReactionByHoverIndex(selectedIndex: Int) {
        reactionViewDisappearListener?.onReactionViewDisappear(launchedBy, selectedIndex)
    }

    fun setupReactionView() {
        reactionView = ReactionView(context).apply {
            //  TODO: it no need? check the code in Dcard Project
            //id = R.id.reactionView
        }
        addView(reactionView)
        customTouchEventListener = reactionView
    }

    fun showReactionView(
        @ReactionConstants.Reaction.LaunchedBy launchedBy: Int,
        clickedViewX: Int, clickedViewY: Int, clickedViewWidth: Int,
        clickedViewHeight: Int
    ) {

        this.launchedBy = launchedBy

        reactionView?.run {
            layoutParams =
                    createParams(clickedViewX, clickedViewY, clickedViewWidth, clickedViewHeight)

            isLaunchFromBottom = isLaunchAnimationBeginFromBottom(clickedViewY)

            calculateParabolaEndPosition(
                clickedViewX, clickedViewY, clickedViewWidth, clickedViewHeight
            ).also {
                parabolaEndX = it[0]
                parabolaEndY = it[1]
            }
            parabolaEndSize = (16 * dp).toInt() //  reference to the size of drawable of
        }

        post {
            runEntryAnim()
        }
    }

    private fun runEntryAnim() {
        reactionView?.runEntryAnim {
            interruptingTouchEvent = true
        }
    }

    /**
     * Create proper LayoutParams to show the [ReactionView] in right position
     */
    private fun createParams(childX: Int, childY: Int, childWidth: Int, childHeight: Int):
            FrameLayout.LayoutParams {

        reactionViewWidth = reactionView!!.getReactionWidth()

        return FrameLayout.LayoutParams(reactionViewWidth, reactionViewHeight).apply {

            var leftMargin = 0
            var topMargin = 0

            when {
                childY <= reactionBottomBound && childX < reactionRightBound -> {
                    //  left & top
                    leftMargin = childX
                    topMargin = childY
                }

                childY <= reactionBottomBound && childX >= reactionRightBound -> {
                    //  right & top
                    leftMargin = childX + childWidth - reactionViewWidth
                    topMargin = childY
                }

                childY > reactionBottomBound && childX < reactionRightBound -> {
                    //  left & bottom
                    leftMargin = childX
                    topMargin = childY + childHeight - reactionViewHeight
                }

                childY > reactionBottomBound && childX >= reactionRightBound -> {
                    //  right & bottom
                    leftMargin = childX + childWidth - reactionViewWidth
                    topMargin = childY + childHeight - reactionViewHeight
                }
            }

            adjustOffsetOfScreen = leftMargin - REACTION_LEFT_DISPLAY_BOUND
            if (adjustOffsetOfScreen < 0) {
                leftMargin = 0
            } else {
                adjustOffsetOfScreen = 0
            }

            setMargins(leftMargin, topMargin, 0, 0)
        }
    }

    /**
     * Decide the parabola end position according where the [ReactionView] display
     */
    private fun calculateParabolaEndPosition(
        childX: Int, childY: Int,
        childWidth: Int, childHeight: Int
    ): IntArray {
        reactionViewWidth = reactionView!!.getReactionWidth()

        return when {
            childY <= reactionBottomBound && childX < reactionRightBound -> {
                //  left & top
                intArrayOf(0, 0)
            }

            childY <= reactionBottomBound && childX >= reactionRightBound -> {
                //  right & top
                intArrayOf(reactionViewWidth - childWidth, 0)
            }

            childY > reactionBottomBound && childX < reactionRightBound -> {
                //  left & bottom
                intArrayOf(0, reactionViewHeight - childHeight)
            }

            childY > reactionBottomBound && childX >= reactionRightBound -> {
                //  right & bottom
                intArrayOf(reactionViewWidth - childWidth, reactionViewHeight - childHeight)
            }

            else -> {
                intArrayOf(0, 0)
            }
        }.apply {
            val offset = (15 * dp).toInt()
            this[0] += offset + adjustOffsetOfScreen
            this[1] += offset
        }
    }

    /**
     * The launch animation of ReactionView could begin from the bottom of [ReactionView]
     * if touch position is not too high
     */
    private fun isLaunchAnimationBeginFromBottom(touchY: Int) =
        touchY > reactionBottomBound
}