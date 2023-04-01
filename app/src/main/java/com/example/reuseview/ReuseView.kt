package com.example.reuseview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children

class ReuseView : ViewGroup {

    private var mAdapter: Adapter<*>? = null

    private var childViewRetainers = arrayListOf<ViewRetainer>()


    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)



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

        children.forEach {
            measureChild(it, widthMeasureSpec, heightMeasureSpec)
        }

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
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
            startTop += child.measuredHeight + 30
        }

    }

    abstract class Adapter<VR : ViewRetainer> {

        abstract fun onCreateViewRetainer(parent: ViewGroup): VR

        abstract fun <VR> onBindViewRetainer(viewRetainer: VR, position: Int)

        abstract fun getItemCount(): Int

    }

    abstract class ViewRetainer {

        var itemView: View

        constructor(itemView: View) {
            this.itemView = itemView
        }

    }


}