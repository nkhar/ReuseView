package com.example.reuseview

import android.content.Context
import android.os.Trace
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import kotlin.math.max
import kotlin.math.min

class ReuseView : ViewGroup { //ScrollingView {

    companion object {

        private const val TAG = " ReuseView"

        private val GAP_BETWEEN_CHILDREN = 30

        const val FOREVER_NS = Long.MAX_VALUE

        const val VERBOSE_TRACING = false

    }

    private var mAdapter: Adapter<*>? = null

    private var childViewRetainers = arrayListOf<ViewRetainer>()

    private var mWidthMode = -1
    private var mHeightMode = -1
    private var mWidth = -1
    private var mHeight = -1
    private var mWidthSpec = -1
    private var mHeightSpec = -1

    private var mLayout: PlacementManager? = null


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val vc = ViewConfiguration.get(context)

        mTouchSlop = vc.scaledTouchSlop
        Log.d(TAG, "touch slop: $mTouchSlop")
    }

    fun setLayoutManager() {
        mLayout = LinearPlacementManager()
        mLayout!!.setRecyclerView(this)
    }


    fun setAdapter(adapter: Adapter<*>) {
        mAdapter = adapter

//        childViewRetainers.clear()


//        if ((mAdapter?.getItemCount() ?: 0) == 0) {
//            return
//        }

//        val tempChildrenCount = mAdapter?.getItemCount() ?: 0

//        for (i in 0 until tempChildrenCount) {
//            val tempViewRetainer = mAdapter?.onCreateViewRetainer(this)
//            val tempViewRetainer = mAdapter?.createViewRetainer(this)

//            childViewRetainers.add(tempViewRetainer!!)
//        }

//        for ((index, viewRetainer) in childViewRetainers.withIndex()) {
//            mAdapter?.onBindViewRetainer(viewRetainer, index)
//        }

//        for (viewRetainer in childViewRetainers) {
//            addView(viewRetainer.itemView)
//        }

//        requestLayout()

    }



    private var measuredOnceAlready = false

    var mInterceptRequestLayoutDepth: Int = 0
    var mLayoutWasDeferred: Boolean = false

    var mLayoutSuppressed: Boolean = false

    var mLayoutOrScrollCounter: Int = 0

    // For use in item animations
    var mItemsAddedOrRemoved: Boolean = false
    var mItemsChanged: Boolean = false

    // simple array to keep min and max child position during a layout calculation
    // preserved not to create a new one in each layout pass
    private val mMinMaxLayoutPositions = IntArray(2)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        Log.d(TAG, "onMeasure: widthMode: $widthMode, widthSize: $widthSize")
        Log.d(TAG, "onMeasure: heightMode: $heightMode, heightSize: $heightSize")

        if (mLayout == null) {
            defaultOnMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }


        if (mLayout!!.isAutoMeasureEnabled()) {

            /**
             * This specific call should be considered deprecated and replaced with
             * {@link #defaultOnMeasure(int, int)}. It can't actually be replaced as it could
             * break existing third party code but all documentation directs developers to not
             * override {@link LayoutManager#onMeasure(int, int)} when
             * {@link PlacementManager#isAutoMeasureEnabled()} returns true.
             */
            mLayout!!.onMeasure(mRecycler, mLayoutState, widthMeasureSpec, heightMeasureSpec)

            val measureSpecModeIsExactly = widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY
            if (measureSpecModeIsExactly || mAdapter == null) {
                Log.d(TAG, "onMeasure: Return because mode is EXACTLY for both")
                return
            }

            if (mLayoutState.mLayoutStep == LayoutState.STEP_START) {
                dispatchLayoutStep1()
            }



        }


//        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)

//        layoutChunk(mRecycler, mLayoutState, 0)

    }

    fun defaultOnMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val tempWidth = PlacementManager.chooseSize(
            widthMeasureSpec,
            paddingLeft + paddingRight,
            ViewCompat.getMinimumWidth(this)
        )
        val tempHeight = PlacementManager.chooseSize(
            heightMeasureSpec,
            paddingTop + paddingBottom,
            ViewCompat.getMinimumHeight(this)
        )
        setMeasuredDimension(tempWidth, tempHeight)
    }

    class ViewInformationStore {
        fun clear() {

        }
    }

    val mViewInfoStore: ViewInformationStore = ViewInformationStore()
    private fun dispatchLayoutStep1() {
        mLayoutState.assertLayoutStep(LayoutState.STEP_START)
        fillRemainingScrollValues(mLayoutState)
        mLayoutState.mIsMeasuring = false
        startInterceptRequestLayout()
        mViewInfoStore.clear()
        onEnterLayoutOrScroll()
        processAdapterUpdatesAndSetAnimationFlags()
        saveFocusInfo()
        mLayoutState.mTrackOldChangeHolders = mLayoutState.mRunSimpleAnimations && mItemsChanged
        mItemsAddedOrRemoved = false.also { mItemsChanged = it }
        mLayoutState.mInPreLayout = mLayoutState.mRunPredictiveAnimations
        mLayoutState.mItemCount = mAdapter!!.getItemCount()
        findMinMaxChildLayoutPositions(mMinMaxLayoutPositions)

        if (mLayoutState.mRunSimpleAnimations) {
            // Step 0: Find out where all non-removed items are, pre-layout
        }
        if (mLayoutState.mRunPredictiveAnimations) {
            // Step 1: run prelayout: This will use the old positions of items. The layout manager
            // is expected to layout everything, even removed items (though not to add removed
            // items back to the container). This gives the pre-layout position of APPEARING views
            // which come into existence as part of the real layout.

            // Save old positions so that LayoutManager can run its mapping logic.

        } else {
            clearOldPositions()
        }


    }

    private fun findMinMaxChildLayoutPositions(into: IntArray) {
        //TODO have to implement this
    }

    fun onEnterLayoutOrScroll() {
        mLayoutOrScrollCounter++
    }
    private fun startInterceptRequestLayout() {
        mInterceptRequestLayoutDepth++
        if (mInterceptRequestLayoutDepth == 1 && !mLayoutSuppressed) {
            mLayoutWasDeferred = false
        }
    }

    private fun saveFocusInfo() {
        // TODO have no idea what this does
    }

    private fun fillRemainingScrollValues(state: LayoutState) {

    }

    private fun processAdapterUpdatesAndSetAnimationFlags() {
        // TODO have to understand and implement animation stuff
    }

    fun clearOldPositions() {
        // TODO have to implement this
    }


    private fun previousOnMeasureCode(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        if ((mAdapter?.getItemCount() ?: 0) == 0) {
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
            return
        }

        setMeasureSpecs(widthMeasureSpec, heightMeasureSpec)

//        var totalChildHeight = 0

//        children.forEach {
//            measureChild(it, widthMeasureSpec, heightMeasureSpec)
//        }

//        children.forEach {
//            totalChildHeight += it.measuredHeight + GAP_BETWEEN_CHILDREN
//        }

//        for (i in 0 until (mAdapter?.getItemCount() ?: 0)) {
//            layoutChunk(mRecycler, mLayoutState, i)
//        }

        if (measuredOnceAlready) {
            mLayoutState.mOffset = 109
            mLayoutState.mAvailable = 654
            fill(mRecycler, mLayoutState)
        } else {
            measuredOnceAlready = true
        }
        setMeasuredDimension(widthMeasureSpec, 400)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

//        for (i in 0 until (mAdapter?.getItemCount() ?: 0)) {
//            layoutChunk(mRecycler, mLayoutState, i)
//        }

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


    private val mLayoutChunkResult = LLM.LayoutChunkResult()
    fun fill(recycler: ReuseView.Recycler, layoutState: ReuseView.LayoutState): Int {
        val start = layoutState.mAvailable

        var remainingSpace = layoutState.mAvailable

        val layoutChunkResult = mLayoutChunkResult

        while (remainingSpace > 0) {
            Log.d(TAG, "fill: remainingSpace = $remainingSpace")
            layoutChunkResult.resetInternal()
            if (VERBOSE_TRACING) {
                Trace.beginSection("LLM layoutChunk")
            }

            layoutChunk(recycler, layoutState, layoutChunkResult)

            if (VERBOSE_TRACING) {
                Trace.endSection()
            }

            if (layoutChunkResult.mFinished) {
                break
            }
            layoutState.mOffset += layoutChunkResult.mConsumed
            layoutState.mAvailable -= layoutChunkResult.mConsumed
            // we keep a separate remaining space because mAvailable is important for recycling
            remainingSpace -= layoutChunkResult.mConsumed

        }

        return start - layoutState.mAvailable
    }

    fun layoutChunk(recycler: ReuseView.Recycler, layoutState: ReuseView.LayoutState, result: LLM.LayoutChunkResult) {
        val view: View = layoutState.next(recycler)

        addView(view)

//        measureChildWithMargins(view, 0, 0)
//        view.measure(mWidth, mHeight)
        view.measure(mWidthSpec, mHeightSpec)

        result.mConsumed = view.measuredHeight

        var left: Int
        var top: Int
        var right: Int
        var bottom: Int

        left = paddingLeft
        right = left + view.measuredWidth

        bottom = layoutState.mOffset
        top = layoutState.mOffset - view.measuredHeight

        Log.d(TAG, "layoutChunk: $left $top $right $bottom")

        view.layout(left, top, right, bottom)

    }

    fun setMeasureSpecs(wSpec: Int, hSpec: Int) {
        mWidthSpec = wSpec
        mHeightSpec = hSpec

        mWidth = MeasureSpec.getSize(wSpec)
        mWidthMode = MeasureSpec.getMode(wSpec)

        mHeight = MeasureSpec.getSize(hSpec)
        mHeightMode = MeasureSpec.getMode(hSpec)
    }

    var mLayoutState = LayoutState()
    class LayoutState {

        companion object {
            const val STEP_START = 1
        }

        fun assertLayoutStep(accepted: Int) {
            check(accepted and mLayoutStep != 0) {
                ("Layout state should be one of "
                        + Integer.toBinaryString(accepted) + " but it is "
                        + Integer.toBinaryString(mLayoutStep))
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////
        // Fields below must be updated or cleared before they are used (generally before a pass)
        ////////////////////////////////////////////////////////////////////////////////////////////

        var mLayoutStep = STEP_START

        /**
         * Number of items adapter has.
         */
        var mItemCount = 0

        /**
         * True if the associated [ReuseView] is in the pre-layout step where it is having
         * its [PlacementManager] layout items where they will be at the beginning of a set of
         * predictive item animations.
         */
        var mInPreLayout = false

        var mTrackOldChangeHolders: Boolean = false

        var mIsMeasuring = false

        ////////////////////////////////////////////////////////////////////////////////////////////
        // Fields below are always reset outside of the pass (or passes) that use them
        ////////////////////////////////////////////////////////////////////////////////////////////

        var mRunSimpleAnimations: Boolean = false

        var mRunPredictiveAnimations: Boolean = false

        /**
         *  Pixel offset where layout should start
         */
        var mOffset: Int = 0

        /**
         * Number of pixels that we should fill, in the layout direction.
         */
        var mAvailable: Int = 0

        /**
         * Current position on the adapter to get the next item.
         */
        var mCurrentPosition: Int = 0

        fun next(recycler: ReuseView.Recycler): View {
            val view = recycler.getViewForPosition(mCurrentPosition)
            mCurrentPosition += 1

            return view
        }

    }

    var mRecycler = Recycler()
    inner class Recycler {
        fun getViewForPosition(position: Int): View {
            return getViewForPosition(position, true)
        }

        fun getViewForPosition(position: Int, dryRun: Boolean): View {
            return tryGetViewRetainerForPositionByDeadline(position, dryRun, FOREVER_NS).itemView
        }

        fun tryGetViewRetainerForPositionByDeadline(
            position: Int,
            dryRun: Boolean,
            deadlineNs: Long,
        ): ViewRetainer {
            var retainer: ViewRetainer? = null

            if (retainer == null) {
                retainer = mAdapter?.createViewRetainer(this@ReuseView)
            }

            mAdapter?.onBindViewRetainer(retainer, position)

            return retainer!!
        }
    }

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

    abstract class PlacementManager {

        var mAutoMeasure = false

        var mRecyclerView: ReuseView? = null

        open fun isAutoMeasureEnabled(): Boolean {
            return mAutoMeasure
        }

        fun setRecyclerView(reuseView: ReuseView) {
            mRecyclerView = reuseView
        }

        open fun onMeasure(
            recycler: Recycler, state: LayoutState, widthSpec: Int,
            heightSpec: Int,
        ) {
            mRecyclerView!!.defaultOnMeasure(widthSpec, heightSpec)
        }

        companion object {
            fun chooseSize(spec: Int, desired: Int, min: Int): Int {
                val mode = MeasureSpec.getMode(spec)
                val size = MeasureSpec.getSize(spec)
                Log.d(TAG, "chooseSize: mode: $mode size: $size")
                return when (mode) {
                    MeasureSpec.EXACTLY -> size
                    MeasureSpec.AT_MOST -> {
                        // height set to wrap_content should result this mode, size will be 1984 or
                        // 2150 maybe something to do with status/navigation bars. We have not
                        // assigned any padding or minHeight to ReuseView in the activity_main.xml
                        // therefore max(desired, min) is 0 and min of (size, 0) will be 0. ReuseView
                        // height will be 0, thus it will not be shown.
                        Log.d(TAG, "chooseSize: AT_MOST for height desired: $desired, min: $min, max: ${max(desired, min)}")
                        min(size, max(desired, min))
                    }
                    MeasureSpec.UNSPECIFIED -> max(desired, min)
                    else -> max(desired, min)
                }
            }
        }
    }

    class LinearPlacementManager: PlacementManager() {
        override fun isAutoMeasureEnabled(): Boolean {
            return true
        }
    }

    object LLM {
        class LayoutChunkResult {

            var mConsumed = 0
            var mFinished = false
            var mIgnoreConsumed = false
            var mFocusable = false
            fun resetInternal() {
                mConsumed = 0
                mFinished = false
                mIgnoreConsumed = false
                mFocusable = false
            }

        }

    }

}