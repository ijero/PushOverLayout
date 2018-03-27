package cn.ijero.pushover

import android.content.Context
import android.graphics.Color
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.dip
import org.jetbrains.anko.info
import kotlin.math.absoluteValue

/**
 *
 * @author Jero . Created on 2018/3/1.
 */
class PushOverLayout
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs), AnkoLogger {

    private var mTopHeight = DEF_INT_VALUE
    private var mOverBackgroundHeight = DEF_INT_VALUE
    private var mSnapHeight = DEF_INT_VALUE
    private var mContentHeight = DEF_INT_VALUE
    private lateinit var mTopView: View
    private lateinit var mSnapView: View
    private lateinit var mContentView: View

    private var isSnapped = false
    /**
     * 是否锁定状态
     *
     * @author Jero
     */
    var lockCurrentState = false

    /**
     * 阴影是否显示
     *
     * @author Jero
     */
    var shadowEnable = true

    /**
     * 覆盖背景层是否显示
     *
     * @author Jero
     */
    var overBackgroundEnable = true
    /**
     * 监听器
     *
     * @author Jero
     */
    var listenPushChanged: OnPushChangedListener? = null
    /**
     * 设置滑动时顶部位移的视差变化率
     *
     * 范围：0.1F~1.0F
     *
     * @author Jero
     */
    var topParallax: Float = DEF_PARALLAX
        set(value) {
            field = when {
                (value < 0.1F) -> 0.1F
                (value > 1.0F) -> 1.0F
                else -> value
            }
        }
    private var mLastDragTop = 0
    private val mShadowHeight: Int by lazy {
        dip(10)
    }

    private val mDragGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            return distanceY > dip(10)
        }
    }
    private val mDragGestureDetector: GestureDetector by lazy {
        GestureDetector(context, mDragGestureListener)
    }
    private val mOverDragView: View by lazy {
        View(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private val mOverBackgroundView: View by lazy {
        View(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.BLACK)
            alpha = 0F
        }
    }

    private val mShadowView: View by lazy {
        View(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mShadowHeight)
            setBackgroundResource(R.drawable.shape_shadow)
        }
    }

    companion object {
        private const val DEF_INT_VALUE = 0

        /**
         * 默认的视差值
         *
         * @author Jero
         */
        const val DEF_PARALLAX = 0.5F

        /**
         * childCount不合法异常信息描述
         *
         * @author Jero
         */
        const val ERROR_CHILD_COUNT = "PushOverLayout 的子控件数量不能少于3个！"

    }

    init {
        applyStyle(attrs)
    }

    private fun applyStyle(attrs: AttributeSet?) {
        attrs ?: return
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PushOverLayout)
        topParallax = ta.getFloat(R.styleable.PushOverLayout_pol_topParallax, topParallax)
        shadowEnable = ta.getBoolean(R.styleable.PushOverLayout_pol_shadowEnable, shadowEnable)
        overBackgroundEnable = ta.getBoolean(R.styleable.PushOverLayout_pol_overBackgroundEnable, overBackgroundEnable)
        ta.recycle()
    }

    private val mDragCallback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return child == mOverDragView && !lockCurrentState
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            if (child == mOverDragView) {
                if (top >= 0) {
                    return 0
                }

                if (top <= mSnapHeight - mTopHeight) {
                    return mSnapHeight - mTopHeight
                }
            }
            info {
                "clampViewPositionVertical : top = $top , dy = $dy"
            }
            return top
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            if (releasedChild == mOverDragView) {
                if (yvel < -1000) {
                    // 上滑
                    mLastDragTop = mSnapHeight - mTopHeight
                    isSnapped = true
                } else if (yvel > 1000) {
                    // 下滑
                    mLastDragTop = 0
                    isSnapped = false
                } else {
                    // 基于中心线判断
                    if (releasedChild.top <= -mTopHeight * 0.5F) {
                        mLastDragTop = mSnapHeight - mTopHeight
                        isSnapped = true
                    } else {
                        mLastDragTop = 0
                        isSnapped = false
                    }
                }
                mDragHelper.settleCapturedViewAt(0, mLastDragTop)
                invalidate()
            }
            info {
                "onViewReleased : yvel = $yvel , top = ${releasedChild.top}"
            }
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return mTopHeight
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            // 计算向上滑动的比例
            val percentage = top.absoluteValue.toFloat() / (mTopHeight - mSnapHeight)
            listenPushChanged?.onPushChanged(top.toFloat(), percentage)

            if (changedView == mOverDragView) {
                // top offset
                val topOffset = top.toFloat() * topParallax
                mTopView.translationY = topOffset
                listenPushChanged?.onTopOffsetChanged(topOffset)

                // snap offset
                val snapOffset = top + top.toFloat() / (mTopHeight - mSnapHeight) * mSnapHeight
                mSnapView.translationY = snapOffset
                listenPushChanged?.onSnapOffsetChanged(snapOffset)

                // content offset
                mContentView.translationY = top.toFloat()

                // shadow offset
                if (shadowEnable) {
                    mShadowView.translationY = top + top.toFloat() / (mTopHeight - mSnapHeight) * (mSnapHeight + mShadowHeight)
                }

                // top over background offset and alpha
                if (overBackgroundEnable) {
                    mOverBackgroundView.translationY = topOffset
                    mOverBackgroundView.alpha = percentage * 60 / 255
                }
            }

            info {
                "onViewPositionChanged : top = $top, dy = $dy"
            }
        }

    }

    private val mDragHelper: ViewDragHelper by lazy {
        ViewDragHelper.create(this, mDragCallback)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount < 3) {
            throw IllegalArgumentException(ERROR_CHILD_COUNT)
        }

        val views = (0 until childCount).map { getChildAt(it) }
        mTopView = views[0]
        mSnapView = views[1]
        mContentView = views[2]
        removeAllViews()
        addView(mTopView)
        addView(mOverBackgroundView)
        addView(mShadowView)
        addView(mSnapView)
        addView(mContentView)
        if (views.size > 3) {
            for (i in 3 until views.size) {
                addView(views[i])
            }
        }
        addView(mOverDragView)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (!lockCurrentState) {
            mDragHelper.shouldInterceptTouchEvent(ev)
        } else {
            super.onInterceptTouchEvent(ev)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (!lockCurrentState) {
            mDragHelper.processTouchEvent(event)
            !lockCurrentState
        } else {
            super.onTouchEvent(event)
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val heightSpec = MeasureSpec.makeMeasureSpec(height - mSnapHeight, MeasureSpec.EXACTLY)
        mContentView.measure(widthMeasureSpec, heightSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (childCount < 3) {
            throw IllegalArgumentException(ERROR_CHILD_COUNT)
        }
        mTopHeight = mTopView.measuredHeight
        mOverBackgroundHeight = mOverBackgroundView.measuredHeight
        mSnapHeight = mSnapView.measuredHeight
        mContentHeight = mContentView.measuredHeight
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (childCount < 3) {
            throw IllegalArgumentException(ERROR_CHILD_COUNT)
        }
        mContentView.layout(0, mTopHeight, mContentView.measuredWidth, mContentHeight + mTopHeight)
        mOverBackgroundView.layout(0, 0, mOverBackgroundView.measuredWidth, mTopHeight)
        mShadowView.layout(0, mTopHeight, mShadowView.measuredWidth, mShadowHeight + mTopHeight)
        mSnapView.layout(0, mTopHeight, mSnapView.measuredWidth, mSnapView.measuredHeight + mTopHeight)
        mOverDragView.layout(0, mLastDragTop, mOverDragView.measuredWidth, mOverDragView.measuredHeight + mTopHeight)
    }

    override fun computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            invalidate()
            return
        }

        if (isSnapped) {
            // 回调最终位置
            listenPushChanged?.onPushStateChanged(SnapState.SNAP)
        } else {
            listenPushChanged?.onPushStateChanged(SnapState.NORMAL)
        }
    }

    /**
     * 手动进行设置停靠位置
     *
     * @author Jero
     */
    fun snapTo(state: SnapState) {
        isSnapped = state == SnapState.SNAP
        mLastDragTop = when (state) {
            PushOverLayout.SnapState.SNAP -> {
                mSnapHeight - mTopHeight
            }
            PushOverLayout.SnapState.NORMAL -> {
                0
            }
        }
        mDragHelper.smoothSlideViewTo(mOverDragView, 0, mLastDragTop)
        invalidate()
    }

    /**
     * 获取停靠状态
     *
     * @return true 处于停靠状态 ， false 非停靠状态。
     *
     * @author Jero
     */
    fun isSnaped() = isSnapped

    /**
     * 改变相反的停靠位置
     *
     * @author Jero
     */
    fun toggle() {
        if (isSnapped) {
            snapTo(SnapState.NORMAL)
        } else {
            snapTo(SnapState.SNAP)
        }
    }

    /**
     * 锁定当前状态，同[lockCurrentState] = true
     *
     * @author Jero
     */
    fun lock() {
        lockCurrentState = true
    }

    /**
     * 解锁状态，同[lockCurrentState] = false
     *
     * @author Jero
     */
    fun unlock() {
        lockCurrentState = false
    }

    /**
     * 获取锁定状态的值，同[lockCurrentState]
     *
     * @author Jero
     */
    fun isLocked() = lockCurrentState

    /**
     * 组件变化监听器
     *
     * @author Jero
     */
    interface OnPushChangedListener {
        /**
         * 推动发生变化
         *
         * @param offsetPixel 变化的像素
         * @param percentage 变化的百分比
         *
         * @author Jero
         */
        fun onPushChanged(offsetPixel: Float, percentage: Float)

        /**
         * 推动状态发生变化
         *
         * @param state 状态
         *
         * @author Jero
         */
        fun onPushStateChanged(state: SnapState)

        /**
         * 停靠组件的位移变化
         *
         * @param offsetPixel 位移的像素
         *
         * @author Jero
         */
        fun onSnapOffsetChanged(offsetPixel: Float)

        /**
         * 顶部组件的位移变化
         *
         * @param offsetPixel 位移的像素
         *
         * @author Jero
         */
        fun onTopOffsetChanged(offsetPixel: Float)
    }

    /**
     * 简单实现的组件变化监听器，可以复写该类进行简单监听，参考[OnPushChangedListener]
     *
     * @author Jero
     */
    open class SimpleOnPushChangedListener : OnPushChangedListener {
        override fun onPushChanged(offsetPixel: Float, percentage: Float) {

        }

        override fun onSnapOffsetChanged(offsetPixel: Float) {
        }

        override fun onTopOffsetChanged(offsetPixel: Float) {
        }


        override fun onPushStateChanged(state: SnapState) {
        }

    }

    /**
     * 组件状态枚举类型
     *
     * @author Jero
     */
    enum class SnapState {
        /**
         * 停靠状态
         * @author Jero
         */
        SNAP,

        /**
         * 非停靠时的默认状态
         *
         * @author Jero
         */
        NORMAL
    }
}