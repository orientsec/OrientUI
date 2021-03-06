package com.mobile.orientui.rankinggroup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobile.orientui.pinnedrecyclerview.HeaderTouchListener
import com.mobile.orientui.pinnedrecyclerview.StickyHeadersDecoration
import kotlinx.android.synthetic.main.ranking_group_layout.view.*

/**
 * 综合行情列表
 * 支持上下左右滑动
 * 同时支持上拉刷新和静止时拉取数据
 */
class RankingGroupView : FrameLayout {

    lateinit var mScrollCallback: OnScrollCallback
    /**
     * recyclerview 刷新的起始位置，绝对位置值
     * 初始值为1
     */
    var startPosition: Int = 1
    /**
     * 列表的前值数据量，用于判断是否需要加载新数据
     */
    var previousTotal: Int = 0

    var isLoadingNewItem: Boolean = false

    /**
     * 最大可加载数据量
     */
    var maxRefreshCount: Int = 20
        private set
    var cacheCount: Int = 1
        private set

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initAttrs(context, attrs, defStyle)
        initView(context)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.RankingGroupView, defStyle, 0)
        maxRefreshCount = a.getInt(R.styleable.RankingGroupView_maxRefreshCount, 20)
        cacheCount = a.getInt(R.styleable.RankingGroupView_cacheCount, 1)
        a.recycle()
    }

    private fun initView(context: Context) {
        View.inflate(context, R.layout.ranking_group_layout, this)
        val horizontalViewSynchronize = RecyclerViewSynchronize()
        horizontalViewSynchronize.attach(recycler_view_left, recycler_view_right, scrollListener)
    }

    fun setAdapter(leftAdapter: RankingBaseAdapter<*>, rightAdapter: RankingBaseAdapter<*>) {

        recycler_view_left.apply {
            adapter = leftAdapter
            layoutManager = LinearLayoutManager(context)
            val decoration = StickyHeadersDecoration(leftAdapter)
            addItemDecoration(decoration)
            addOnItemTouchListener(HeaderTouchListener(this, decoration))
        }
        recycler_view_right.apply {
            adapter = rightAdapter
            layoutManager = LinearLayoutManager(context)
            val decoration = StickyHeadersDecoration(rightAdapter)
            addItemDecoration(decoration)
            addOnItemTouchListener(HeaderTouchListener(this, decoration))
        }
    }

    fun addItemDecoration(decor: RecyclerView.ItemDecoration) {
        recycler_view_left.apply {
            addItemDecoration(decor)
        }
        recycler_view_right.apply {
            addItemDecoration(decor)
        }
    }

    fun smoothScrollBy(dx: Int, dy: Int) {
        recycler_view_left.smoothScrollBy(dx, dy)
        recycler_view_right.smoothScrollBy(dx, dy)
    }

    fun scrollToPosition(position: Int) {
        recycler_view_left.scrollToPosition(position)
        recycler_view_right.scrollToPosition(position)
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        private var loading = true
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            val firstVisibleItem = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val lastVisibleItem = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            val visibleItemCount = lastVisibleItem - firstVisibleItem + 1
            when (newState) {
                0 -> {
                    if (!isLoadingNewItem) {
                        /*resetDataList()
                        startPoll(firstVisibleItem, visibleItemCount)*/
                        if (::mScrollCallback.isInitialized) mScrollCallback.startPollList(firstVisibleItem, visibleItemCount)
                    }
                }
                else -> /*stopPoll()*/ {
                    if (::mScrollCallback.isInitialized) mScrollCallback.stopPollList()
                }
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dx == 0 && dy == 0) return
            val totalItemCount = (recyclerView.layoutManager as LinearLayoutManager).itemCount
            val firstVisibleItem = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val lastVisibleItem = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            val visibleItemCount = lastVisibleItem - firstVisibleItem + 1
            if (totalItemCount < maxRefreshCount) return

            //上拉逻辑
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false
                    previousTotal = totalItemCount
                }
            } else if (totalItemCount - visibleItemCount <= firstVisibleItem) {
                isLoadingNewItem = true
                /*loadListData(totalItemCount - 1)*/
                if (::mScrollCallback.isInitialized) mScrollCallback.loadMoreList(totalItemCount)
                loading = true
            }

            //滑动过程中，上拉刷新
            if (dy > 0 && firstVisibleItem - startPosition >= maxRefreshCount - visibleItemCount) {
                startPosition = firstVisibleItem - cacheCount
            }

            //滑动过程中，下拉刷新
            if (dy < 0 && firstVisibleItem <= startPosition && startPosition > 1) {
                val position = firstVisibleItem - (maxRefreshCount - cacheCount - visibleItemCount)
                startPosition = if (position <= 0) 1 else position
            }
        }
    }

    /**
     * 列表滑动过程中，回调接口
     */
    interface OnScrollCallback {
        /**
         * 列表静止时，刷新页面，拉取数据
         * @param firstVisibleItem
         * @param visibleItemCount:可见item数量，拉取数据
         */
        fun startPollList(firstVisibleItem: Int, visibleItemCount: Int)

        /**
         * 列表滑动状态时，停止刷新，拉取数据
         */
        fun stopPollList()

        /**
         * 底部上拉加载
         */
        fun loadMoreList(totalItemCount: Int)
    }
}
