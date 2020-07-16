package net.vrgsoft.parallaxview

import android.content.Context
import android.graphics.Rect
import android.support.v4.math.MathUtils
import android.support.v4.view.ScrollingView
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView


class ParallaxView : FrameLayout {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        Log.i("ParallaxView","##onScrollChanged")
        updateTransformation()
    }

    /**
     * 当View树的状态发生改变或者View树内部的View的可见性发现改变时,onGlobalLayout方法将被回调,
     */
    private val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            Log.i("ParallaxView","##onGlobalLayout")
            initScrollableParent()
            viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    /**
     * 5.确定View大小(onSizeChanged)  [【Android 自定义 View 实战】之你应该明白的事儿 - Android - 掘金](https://juejin.im/entry/580ead8dc4c97100589d8dda)
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Log.i("ParallaxView","##onSizeChanged")
        initScrollableParent()

        if (isNeedScale) {
            scaleX = parallaxScale
            scaleY = parallaxScale
        } else {
            scaleX = 1.0f
            scaleY = 1.0f
        }

        /*
            This crutch is extremely needed,
            because right coordinates are available on 5-6 call of getLocationInWindow()
         */
        for(i in 1..5) postDelayed({ updateTransformation() }, 50)
    }

    /**
     * sca： 附加到屏幕时，add 两个监听
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewTreeObserver.removeOnScrollChangedListener(scrollListener)
    }

    private var scrollableParent: View? = null
//   数组 x和y
    private val scrollableParentLocation = IntArray(2)
//   数组 x和y
    private var location = IntArray(2)

    var isEnabledHorizontalParallax = true
    var isEnabledVerticalParallax = true
    var isInvertedHorizontalParallax = false
    var isInvertedVerticalParallax = false
//    缩放, 会伸缩视图，但是不是动态伸缩的。设置后，立刻见效
    var isNeedScale = false
    var parallaxScale = 1.5f
    var decelerateFactor = 0.2f



    private fun initScrollableParent() {
        Log.i("ParallaxView","initScrollableParent")
        Log.i("ParallaxView","initScrollableParent getLocationInWindow 前:"+ "_x:"+scrollableParentLocation[0]+"_y:" + scrollableParentLocation[1])

//      sca: 两个值都 != 0 ，就什么也不做 。有一个= 0就会执行return 后面的代码。
        if (scrollableParentLocation[0] != 0 && scrollableParentLocation[1] != 0){
            return
        }
        var viewParent = parent
        while (viewParent is View) {
            if (viewParent is ScrollingView ||
                    viewParent is ScrollView ||
                    viewParent is HorizontalScrollView ||
                    viewParent is AdapterView<*>) {
                scrollableParent = viewParent
//                计算这一观点在其窗口的坐标。 所述参数必须是两个整数的数组。 该方法返回后，该数组包含按该顺序在x和y位置
                viewParent.getLocationInWindow(scrollableParentLocation)
                break
            }
            viewParent = viewParent.getParent()
        }
        Log.i("ParallaxView","initScrollableParent getLocationInWindow 后:"+ "_x:"+scrollableParentLocation[0]+"_y:" + scrollableParentLocation[1])

    }

    private fun init(context: Context?, attrs: AttributeSet?) {
        clipChildren = true

        val a = context?.obtainStyledAttributes(attrs, R.styleable.ParallaxView)
        isEnabledHorizontalParallax = a?.getBoolean(R.styleable.ParallaxView_isEnabledHorizontalParallax, isEnabledHorizontalParallax) ?: isEnabledHorizontalParallax
        isEnabledVerticalParallax = a?.getBoolean(R.styleable.ParallaxView_isEnabledVerticalParallax, isEnabledVerticalParallax) ?: isEnabledVerticalParallax
        isInvertedHorizontalParallax = a?.getBoolean(R.styleable.ParallaxView_isInvertedHorizontalParallax, isInvertedHorizontalParallax) ?: isInvertedHorizontalParallax
        isInvertedVerticalParallax = a?.getBoolean(R.styleable.ParallaxView_isInvertedVerticalParallax, isInvertedVerticalParallax) ?: isInvertedVerticalParallax
        isNeedScale = a?.getBoolean(R.styleable.ParallaxView_isNeedScale, isNeedScale) ?: isNeedScale
        decelerateFactor = a?.getFloat(R.styleable.ParallaxView_decelerateFactor, decelerateFactor) ?: decelerateFactor
        parallaxScale = a?.getFloat(R.styleable.ParallaxView_parallaxScale, parallaxScale) ?: parallaxScale
        MathUtils.clamp(decelerateFactor, 0f, 1f)
        a?.recycle()
        Log.i("ParallaxView","init 前:"+ "_x:"+scrollableParentLocation[0]+"_y:" + scrollableParentLocation[1])

    }

    /**
     * sca: 更新偏移量
     */
    private fun updateTransformation() {
        Log.i("ParallaxView","updateTransformation")
        if (scrollableParent == null) return


        Log.i("ParallaxView","updateTransformation getLocationInWindow 前:"+ "_x:"+location[0]+"_y:" + location[1])

        getLocationInWindow(location)
        Log.i("ParallaxView","updateTransformation getLocationInWindow 后:"+ "_x:"+location[0]+"_y:" + location[1])


        var viewCenter: Int
//       sca: decelerateFactor  两个层的偏移系数不同，所以形成了视差效果。
        if (isEnabledHorizontalParallax) {
            viewCenter = location[0] + (width * scaleX / 2).toInt()
// translation  滑动时，更新x 轴偏移量
            translationX = (viewCenter - (scrollableParentLocation[0] + scrollableParent!!.width / 2)) *
                    decelerateFactor * (if (isInvertedHorizontalParallax) -1 else 1)
        }

        if (isEnabledVerticalParallax) {
            viewCenter = location[1] + (height * scaleY / 2).toInt()
            translationY = (viewCenter - (scrollableParentLocation[1] + scrollableParent!!.height / 2)) * decelerateFactor * if (isInvertedVerticalParallax) -1 else 1
        }
    }

    private fun View.isVisible(): Boolean {
        if (!isShown) return false

        val actualPosition = Rect()
        getGlobalVisibleRect(actualPosition)
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val screen = Rect(0, 0, screenWidth, screenHeight)
        return actualPosition.intersect(screen)
    }
}