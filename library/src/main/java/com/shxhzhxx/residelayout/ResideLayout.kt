package com.shxhzhxx.residelayout

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.core.content.res.use
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.tan

private fun <T : Comparable<T>> middle(vararg i: T) = i.sorted()[i.size / 2]
private fun DisplayMetrics.dpToPx(dp: Float) = dp * density + 0.5f

class ResideLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), NestedScrollingParent2 {
    private val maxTransition get() = width * residePercentage
    private var minFlingVelocity = 0f
    private var minDragVelocity = 0f
    private var maxDuration = 0
    private var residePercentage = 0f
    private var resideScale = 0f
    private var resideElevation = 0f
    private var dragAngleTolerance = 0f

    init {
        val dm = resources.displayMetrics
        context.theme.obtainStyledAttributes(attrs, R.styleable.ResideLayout, 0, 0).use {
            minFlingVelocity = it.getDimension(R.styleable.ResideLayout_minFlingVelocity, dm.dpToPx(50f))
            minDragVelocity = it.getDimension(R.styleable.ResideLayout_minDragVelocity, dm.dpToPx(50f))
            maxDuration = it.getInteger(R.styleable.ResideLayout_maxDuration, 600)
            dragAngleTolerance = it.getFloat(R.styleable.ResideLayout_dragAngleTolerance, 30f)
            residePercentage = it.getFloat(R.styleable.ResideLayout_residePercentage, 0.6f)
            resideScale = it.getFloat(R.styleable.ResideLayout_resideScale, 0.6f)
            resideElevation = it.getDimension(R.styleable.ResideLayout_resideElevation, dm.dpToPx(8f))
        }
    }

    private var nestedFlingTarget: View? = null
    private val helper = NestedScrollingParentHelper(this)
    override fun getNestedScrollAxes() = helper.nestedScrollAxes

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        if (type == TYPE_TOUCH)
            setViewTransition(slideView.translationX - dxUnconsumed)
        else if (!scroller.computeScrollOffset())
            settle(-dxUnconsumed.toFloat() * 60)  //60 frame per second
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (type != TYPE_TOUCH)
            return
        consumed[0] = when {
            dx > 0 -> middle(0, dx, (slideView.translationX).toInt())
            slideView.translationX == 0f -> 0
            else -> middle(0, dx, (slideView.translationX - maxTransition).toInt())
        }
        setViewTransition(slideView.translationX - consumed[0])
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        if (type != TYPE_TOUCH) {
            isNestedFling = false
            nestedFlingTarget = null
        } else {
            helper.onStopNestedScroll(target, type)
            if (!isNestedFling)
                settle(0f)
        }
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return isEnabled && axes == ViewCompat.SCROLL_AXIS_HORIZONTAL && child == slideView
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        if (type != TYPE_TOUCH) {
            isNestedFling = true
            nestedFlingTarget = target
        } else
            helper.onNestedScrollAccepted(child, target, axes, type)
    }

    private var isNestedFling = false
    private var isBeingDragged = false
    private val scroller = OverScroller(context) {
        val i = it - 1f
        i * i * i * i * i + 1f
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            setViewTransition(scroller.currX.toFloat())
            postInvalidateOnAnimation(this)
        }
    }

    private val dragAngleToleranceRatio by lazy { tan(0.01745 * dragAngleTolerance) }
    private val minDragVelocityRatio by lazy { minDragVelocity / 60 }
    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            settle(velocityX)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return (isOpen).also { if (it) closePane() }
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (!isBeingDragged && (abs(distanceX) * dragAngleToleranceRatio < abs(distanceY) || abs(distanceX) < minDragVelocityRatio)) {
                return false
            }
            isBeingDragged = true
            setViewTransition(slideView.translationX - distanceX)
            return true
        }

        override fun onDown(e: MotionEvent?): Boolean {
            nestedFlingTarget?.let { stopNestedScroll(it, TYPE_NON_TOUCH) }
            scroller.forceFinished(true)
            return false
        }
    })
    private val slideView by lazy { getChildAt(1) }

    fun closePane() {
        if (isEnabled) settle(-minFlingVelocity)
    }

    fun openPane() {
        if (isEnabled) settle(minFlingVelocity)
    }

    val isOpen get() = slideView.translationX != 0f

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled)
            return false
        if (helper.nestedScrollAxes == View.SCROLL_AXIS_HORIZONTAL)
            return false
        if (!isBeingDragged && !checkArea(ev))
            return false
        if (ev.action in listOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL)) {
            isBeingDragged = false
            settle(0f)
        }
        detector.onTouchEvent(ev)
        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled)
            return false
        if (helper.nestedScrollAxes == View.SCROLL_AXIS_HORIZONTAL)
            return false
        if (isBeingDragged)
            return true
        if (isOpen && checkArea(ev))
            return true
        if (super.onInterceptTouchEvent(ev))
            return true
        return checkArea(ev) && detector.onTouchEvent(ev)
    }

    private fun checkArea(ev: MotionEvent): Boolean {
        val scaleX = (1 - slideView.scaleX) * slideView.width / 2
        val scaleY = (1 - slideView.scaleY) * slideView.height / 2
        return ev.x > slideView.x + scaleX && ev.x < slideView.x + slideView.width - scaleX && ev.y > slideView.y + scaleY && ev.y < slideView.y + slideView.height - scaleY
    }

    private fun settle(velocity: Float) {
        val distance = when {
            velocity >= minFlingVelocity -> (maxTransition - slideView.translationX).toInt()
            velocity <= -minFlingVelocity || slideView.translationX < maxTransition / 2 -> -slideView.translationX.toInt()
            else -> (maxTransition - slideView.translationX).toInt()
        }
        scroller.forceFinished(true)
        scroller.startScroll(
            slideView.translationX.toInt(), 0, distance, 0,
            min(maxDuration, abs(distance / velocity * 1000 * 4).toInt())
        )
        postInvalidateOnAnimation(this)
    }

    var onOpenListener: (() -> Unit)? = null
    var onCloseListener: (() -> Unit)? = null

    private fun setViewTransition(transition: Float) {
        if (slideView.translationX == 0f && transition > 0f) onOpenListener?.invoke()
        if (slideView.translationX > 0f && transition == 0f) onCloseListener?.invoke()
        slideView.translationX = middle(0f, maxTransition, transition)
        slideView.scaleX = 1 - (1 - resideScale) * slideView.translationX / maxTransition
        slideView.scaleY = slideView.scaleX
        setElevation(slideView, slideView.translationX / maxTransition * resideElevation)
    }
}