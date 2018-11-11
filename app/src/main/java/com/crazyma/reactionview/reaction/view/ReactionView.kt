package com.crazyma.reactionview.reaction.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import com.crazyma.reactionview.R
import com.crazyma.reactionview.reaction.model.Board
import com.crazyma.reactionview.reaction.model.Emoji
import com.crazyma.reactionview.reaction.model.Parabola
import com.crazyma.reactionview.reaction.ReactionConstants

class ReactionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ReactionFrameLayout.CustomTouchEventListener {

    companion object {
        const val HOVER_INDEX_NONE = -1

        private const val STATE_ENTRY = 0x10
        private const val STATE_EXIT = 0x20
        private const val STATE_INTERACTING = 0x30
        private const val STATE_DISABLE = 0x40
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var parabolaEndX = 0
    var parabolaEndY = 0
    var parabolaEndSize = 0

    var isLaunchFromBottom = true

    private var hoverIndex = HOVER_INDEX_NONE
    private var normalSize = 0
    private var biggerSize = 0
    private var smallerSize = 0
    private var spacing = 0
    private var blockSize = 0

    private var currentAlpha = 0
    private var beginAlpha = 0
    private var endAlpha = 0

    private val emojiList = mutableListOf<Emoji>()
    private val board = Board(context)
    private var animator: ValueAnimator? = null
    private var parabolaAnimator: ValueAnimator? = null
    private var state = STATE_DISABLE
    private var parabola = Parabola()

    private val dp = Resources.getSystem().displayMetrics.density
    private val emojiPaddingBottom = context.resources.getDimensionPixelSize(
        R.dimen.reaction_board_padding_bottom
    )

    fun addEmoji(emoji: Emoji) {
        setupTitleBitmap(context, emoji.string, emoji)
        emojiList.add(emoji)
    }

    fun cleanEmoji() {
        emojiList.clear()
    }

    fun isReady() = emojiList.isNotEmpty()

    /**
     * calculate the size of normal size, whose value would be influenced by the reaction count
     */
    fun calculateSize() {
        if (getReactionCount() > 0) {
            normalSize = (ReactionConstants.getNormalSize(getReactionCount()) * dp).toInt()
            biggerSize = (ReactionConstants.SIZE_LARGE * dp).toInt()
            smallerSize = (ReactionConstants.SIZE_SMALL * dp).toInt()
            spacing = (ReactionConstants.SPACING * dp).toInt()

            emojiList.forEach {
                it.normalSize = normalSize
            }
        }
    }

    fun runEntryAnim(afterTask: () -> Unit) {

        if (emojiList.isEmpty()) return

        state = STATE_ENTRY

        setupEmojiAnimFromEntryState()

        if (animator != null && animator!!.isRunning) {
            animator!!.cancel()
        }

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ReactionConstants.DURATION_TRANSACTION
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float

                calculateEmojiPosition(value)

                postInvalidate()
            }

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    state = STATE_INTERACTING
                    afterTask()
                }

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {}
            })
        }.apply { start() }
    }

    fun getReactionWidth() =
        (ReactionConstants.getReactionViewSize(getReactionCount()) * dp).toInt()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        board.drawBoard(canvas)

        paint.alpha = currentAlpha
        emojiList.forEach {
            it.drawEmoji(canvas, paint)
        }

        parabola.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        val x = event.x.toInt()
        val y = event.y.toInt()

        return handleTouchEvent(x, y, event)
    }

    override fun onHandleTouchEvent(event: MotionEvent) {
        val x = event.x.toInt() - x.toInt()
        val y = event.y.toInt() - y.toInt()

        handleTouchEvent(x, y, event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        blockSize = when (getReactionCount()) {
            0 -> 0
            else -> w / getReactionCount()
        }
    }

    private fun calculateCoordinateX() {
        //  the first emoji
        emojiList[0].currentX = spacing

        //  the last emoji
        emojiList.last().currentX = width - spacing - emojiList.last().currentSize

        //  the emojis before the hover index
        for (i in 1 until hoverIndex) {
            emojiList[i].currentX = emojiList[i - 1].currentX + emojiList[i - 1].currentSize + spacing
        }

        //  the emojis before after hover index
        for (i in emojiList.size - 2 downTo hoverIndex + 1) {
            emojiList[i].currentX = emojiList[i + 1].currentX - emojiList[i].currentSize - spacing
        }

        //  the hover emoji
        if (hoverIndex > 0 && hoverIndex != emojiList.size - 1) {
            if (hoverIndex <= (emojiList.size / 2 - 1)) {
                emojiList[hoverIndex].currentX = emojiList[hoverIndex - 1].currentX +
                        emojiList[hoverIndex - 1].currentSize + spacing
            } else {
                emojiList[hoverIndex].currentX = emojiList[hoverIndex + 1].currentX -
                        emojiList[hoverIndex].currentSize - spacing
            }
        }
    }

    private fun calculateEmojiPosition(interpolatedValue: Float) {

        board.calculateCurrentBottomY(interpolatedValue)
        board.calculateCurrentSize(interpolatedValue)
        board.calculateCurrentAlpha(interpolatedValue)

        currentAlpha = getEmojiAnimatedAlpha(interpolatedValue)
        for (i in 0 until emojiList.size) {
            emojiList[i].calculateCurrentSize(interpolatedValue)
            emojiList[i].calculateCurrentY(interpolatedValue)
        }

        calculateCoordinateX()
    }

    private fun calculateEmojiSize(interpolatedValue: Float) {

        board.calculateCurrentSize(interpolatedValue)

        for (i in 0 until emojiList.size) {
            emojiList[i].calculateCurrentSize(interpolatedValue)
            emojiList[i].currentY = height - emojiPaddingBottom - emojiList[i].currentSize
        }

        calculateCoordinateX()
    }

    private fun calculateProperWidthByText(text: String?): Float {
        if (text.isNullOrEmpty()) return 0f

        val paint = Paint()
        val bounds = Rect()

        paint.typeface = Typeface.DEFAULT
        paint.textSize = context.resources.getDimension(R.dimen.reaction_title_size)
        paint.getTextBounds(text!!, 0, text.length, bounds)

        return bounds.width() + 16f * dp
    }

    private fun getEmojiAnimatedAlpha(interpolatedValue: Float): Int {
        val changeAlpha = endAlpha - beginAlpha
        return (beginAlpha + interpolatedValue * changeAlpha).toInt()
    }

    private fun getReactionCount() = emojiList.size

    private fun handleTouchEvent(x: Int, y: Int, event: MotionEvent): Boolean {
        if (state == STATE_INTERACTING) {
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    if (hoverIndex != HOVER_INDEX_NONE &&
                        x in 0..width && y in 0..height
                    ) {
                        runParabolaAnim(hoverIndex)
                        runExitAnim(false)
                    } else {
                        hoverIndex
                        runExitAnim(true)
                    }

                    hoverIndex = HOVER_INDEX_NONE
                }

                MotionEvent.ACTION_CANCEL -> {
                    hoverIndex = HOVER_INDEX_NONE
                    runExitAnim(true)
                }

                MotionEvent.ACTION_MOVE -> {

                    if (x in 0..width && y in 0..height) {
                        for (i in 0 until getReactionCount()) {
                            if (x <= blockSize * (i + 1)) {
                                if (hoverIndex != i) {
                                    select(i)
                                }
                                break
                            }
                        }
                    } else {
                        if (hoverIndex != HOVER_INDEX_NONE) {
                            select(HOVER_INDEX_NONE)
                        }
                    }
                }
            }

            return true
        }
        return false
    }

    private fun onViewDisappear(selectedIndex: Int) {
        state = STATE_DISABLE
        if (parent is ReactionFrameLayout) {
            (parent as ReactionFrameLayout).run {

                selectReactionByHoverIndex(selectedIndex)

                stopInterruptTouchEvent()
            }
        }
    }

    private fun runEmojiAnim(setupEmojiSize: () -> Unit) {

        setupEmojiSize()

        if (animator != null && animator!!.isRunning) {
            animator!!.cancel()
        }

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ReactionConstants.DURATION_HOVER
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float

                calculateEmojiSize(value)

                if (hoverIndex >= 0 && hoverIndex < emojiList.size) {
                    parabola.beginX = emojiList[hoverIndex].currentX
                    parabola.beginY = emojiList[hoverIndex].currentY
                    parabola.beginSize = emojiList[hoverIndex].currentSize
                }

                postInvalidate()
            }
        }.apply { start() }
    }

    private fun runExitAnim(wouldViewActuallyGone: Boolean = true) {
        state = STATE_EXIT

        setupEmojiAnimToExitState()

        if (animator != null && animator!!.isRunning) {
            animator!!.cancel()
        }

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ReactionConstants.DURATION_TRANSACTION
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float

                calculateEmojiPosition(value)

                postInvalidate()
            }

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    if (wouldViewActuallyGone) {
                        onViewDisappear(HOVER_INDEX_NONE)
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {}
            })
        }.apply { start() }
    }

    private fun runParabolaAnim(selectedIndex: Int) {
        parabola.setupArguments(
            parabolaEndX, parabolaEndY, parabolaEndSize,
            if (isLaunchFromBottom) 0 else this.height
        )

        parabolaAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ReactionConstants.DURATION_PARABOLA
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float

                parabola.calculateCurrentValue(value)

                postInvalidate()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    parabola.currentSize = 0
                    postInvalidate()

                    onViewDisappear(selectedIndex)
                }

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {}
            })
        }.apply { start() }
    }

    private fun select(index: Int) {
        hoverIndex = index

        if (index < 0 || index > getReactionCount()) {
            runEmojiAnim { setupEmojiAnimToNormalState() }
        } else {
            runEmojiAnim {
                setupEmojiAnimToHoverState(index)
                parabola.bitmap = emojiList[index].bitmap
            }
        }
    }

    private fun setupEmojiAnimFromEntryState() {

        val offset = normalSize / 2 * if (isLaunchFromBottom) 1 else -1

        //  init value
        board.width = width.toFloat()
        board.x = 0f

        //  anim preparation
        board.beginHeight = (normalSize + spacing * 2).toFloat()
        board.endHeight = board.beginHeight

        board.beginBottomY = (height - emojiPaddingBottom + spacing + offset).toFloat()
        board.endBottomY = (height - emojiPaddingBottom + spacing).toFloat()
        board.currentBottomY = board.beginBottomY

        board.beginAlpha = 0
        board.endAlpha = 255
        board.currentAlpha = board.beginAlpha

        for (i in 0 until emojiList.size) {
            emojiList[i].beginSize = normalSize
            emojiList[i].endSize = normalSize
            emojiList[i].beginY = height - emojiPaddingBottom - normalSize + offset
            emojiList[i].endY = height - emojiPaddingBottom - normalSize
            emojiList[i].currentY = emojiList[i].beginY
        }

        beginAlpha = 0
        endAlpha = 255
        currentAlpha = beginAlpha
    }

    private fun setupEmojiAnimToExitState() {
        val offset = normalSize / 2 * if (isLaunchFromBottom) 1 else -1

        //  anim preparation
        board.beginHeight = board.currentHeight
        board.endHeight = (normalSize + spacing * 2).toFloat()

        board.beginBottomY = board.currentBottomY
        board.endBottomY = (height - emojiPaddingBottom + spacing + offset).toFloat()

        board.beginAlpha = board.currentAlpha
        board.endAlpha = 0

        for (i in 0 until emojiList.size) {

            //  anim preparation
            emojiList[i].beginSize = emojiList[i].currentSize
            emojiList[i].endSize = normalSize

            emojiList[i].beginY = emojiList[i].currentY
            emojiList[i].endY = height - emojiPaddingBottom - normalSize + offset
        }

        beginAlpha = currentAlpha
        endAlpha = 0
    }

    private fun setupEmojiAnimToNormalState() {
        //  anim preparation
        board.beginHeight = board.currentHeight
        board.endHeight = (normalSize + 2 * spacing).toFloat()

        for (i in 0 until emojiList.size) {
            //  anim preparation
            emojiList[i].beginSize = emojiList[i].currentSize
            emojiList[i].endSize = normalSize
        }
    }

    private fun setupEmojiAnimToHoverState(hoverIndex: Int) {

        if (hoverIndex < 0 || hoverIndex > emojiList.size) return

        board.beginHeight = board.currentHeight
        board.endHeight = (smallerSize + 2 * spacing).toFloat()

        for (i in 0 until emojiList.size) {
            emojiList[i].beginSize = emojiList[i].currentSize

            if (i == hoverIndex) {
                emojiList[i].endSize = biggerSize
            } else {
                emojiList[i].endSize = smallerSize
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun setupTitleBitmap(context: Context, string: String?, emoji: Emoji) {
        if (string == null) return

        val titleView = LayoutInflater.from(context).inflate(R.layout.view_reaction_title, null)

        (titleView as TextView).text = string

        val width = calculateProperWidthByText(string)
        val height = context.resources.getDimension(R.dimen.height_reaction_title)
        val w = width.toInt()
        val h = height.toInt()

        emoji.ratioWH = width / height

        emoji.titleWidth = w

        emoji.titleBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        emoji.normalSize = normalSize

        val c = Canvas(emoji.titleBitmap!!)
        titleView.layout(0, 0, w, h)
        titleView.paint.isAntiAlias = true
        titleView.draw(c)
    }
}