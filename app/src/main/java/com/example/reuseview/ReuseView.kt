package com.example.reuseview

import android.content.Context
import android.os.Trace
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.view.children
import kotlin.math.max
import kotlin.math.min

class ReuseView : ViewGroup { //ScrollingView {

    companion object {

        private const val TAG = " ReuseView"

        private val GAP_BETWEEN_CHILDREN = 30

    }

    private var mAdapter: Adapter<*>? = null

    private var childViewRetainers = arrayListOf<ViewRetainer>()


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val vc = ViewConfiguration.get(context)

        mTouchSlop = vc.scaledTouchSlop
    }


    fun setAdapter(adapter: Adapter<*>) {
        mAdapter = adapter

        childViewRetainers.clear()


        if ((mAdapter?.getItemCount() ?: 0) == 0) {
            return
        }

        val tempChildrenCount = mAdapter?.getItemCount() ?: 0

        for (i in 0 until tempChildrenCount) {
            val tempViewRetainer = mAdapter?.onCreateViewRetainer(this)
            childViewRetainers.add(tempViewRetainer!!)
        }

        for ((index, viewRetainer) in childViewRetainers.withIndex()) {
            mAdapter?.onBindViewRetainer(viewRetainer, index)
        }

        for (viewRetainer in childViewRetainers) {
            addView(viewRetainer.itemView)
        }

//        requestLayout()

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        if ((mAdapter?.getItemCount() ?: 0) == 0) {
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
            return
        }

        var totalChildHeight = 0

        children.forEach {
            measureChild(it, widthMeasureSpec, heightMeasureSpec)
        }

        children.forEach {
            totalChildHeight += it.measuredHeight + GAP_BETWEEN_CHILDREN
        }

        setMeasuredDimension(widthMeasureSpec, 400)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        var startTop = 0

        children.forEachIndexed { index, child ->
            child.layout(
                0,
                startTop,
                child.measuredWidth,
                startTop + child.measuredHeight
            )
            startTop += child.measuredHeight + GAP_BETWEEN_CHILDREN
        }

    }

    private var mInitialTouchX = -1
    private var mInitialTouchY = -1

    private var mLastTouchX = -1
    private var mLastTouchY = -1

    private var mScrollPointerId = -1

    private var mTouchSlop = 20
    override fun onTouchEvent(event: MotionEvent): Boolean {


        val action: Int = event.actionMasked
        val actionIndex: Int = event.actionIndex

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mScrollPointerId = event.getPointerId(0)
                mInitialTouchX = (event.x + 0.5f).toInt().also { mLastTouchX = it }
                mInitialTouchY = (event.y + 0.5f).toInt().also { mLastTouchY = it }
            }

            MotionEvent.ACTION_MOVE -> {
                val index: Int = event.findPointerIndex(mScrollPointerId)

                if (index < 0) {
                    Log.e(
                        TAG, "Error processing scroll; pointer index for id "
                                + mScrollPointerId + " not found. Did any MotionEvents get skipped?"
                    )
                    return false
                }

                val x: Int = (event.getX(index) + 0.5f).toInt()
                val y: Int = (event.getY(index) + 0.5f).toInt()
                val dx = mLastTouchX - x
                var dy = mLastTouchY - y


                dy = if (dy > 0) {
                    max(0, dy - mTouchSlop)
                } else {
                    min(0, dy + mTouchSlop)
                }

                scrollBy(dy)
            }
        }
        return true

    }


    fun scrollBy(y: Int) {
        scrollBy(0, y)
    }
//    override fun computeHorizontalScrollOffset(): Int {
//        return 10
//    }
//
//    override fun computeHorizontalScrollExtent(): Int {
//        return 10
//    }
//
//    override fun computeHorizontalScrollRange(): Int {
//        return 10
//    }
//
//    override fun computeVerticalScrollOffset(): Int {
//        return 10
//    }
//
//    override fun computeVerticalScrollExtent(): Int {
//        return 10
//    }
//
//    override fun computeVerticalScrollRange(): Int {
//        return 10
//    }

    abstract class Adapter<VR : ViewRetainer> {

        abstract fun onCreateViewRetainer(parent: ViewGroup): VR

        abstract fun <VR> onBindViewRetainer(viewRetainer: VR, position: Int)

        fun createViewRetainer(parent: ViewGroup): VR {
            Trace.beginSection("Create View Retainer")
            val retainer: VR = onCreateViewRetainer(parent)
            try {
                if (retainer.itemView.parent != null) {
                    throw IllegalStateException(
                        "ViewRetainer views must not be attached when created." +
                                " Ensure that you are not passing 'true' to the attachToRoot parameter" +
                                "of LayoutInflater.inflate(..., boolean attachToRoot)"
                    )
                }
                return retainer
            } finally {
                Trace.endSection()
            }
        }

        abstract fun getItemCount(): Int

    }

    abstract class ViewRetainer {

        var itemView: View

        constructor(itemView: View) {
            this.itemView = itemView
        }

    }


}