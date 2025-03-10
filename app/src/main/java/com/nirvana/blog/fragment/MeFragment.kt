package com.nirvana.blog.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.gyf.immersionbar.ImmersionBar
import com.nirvana.blog.R
import com.nirvana.blog.activity.user.AccountActivity
import com.nirvana.blog.adapter.user.CarouselRecyclerViewAdapter
import com.nirvana.blog.adapter.user.UserOptionsRecyclerViewAdapter
import com.nirvana.blog.base.BaseFragment
import com.nirvana.blog.databinding.FragmentMeBinding
import com.nirvana.blog.entity.UserOption
import com.nirvana.blog.utils.StatusBarUtils.setBaseStatusBar
import java.util.*

class MeFragment : BaseFragment<FragmentMeBinding>() {

    companion object {
        @JvmStatic
        fun newInstance() = MeFragment()
    }

    private var bannerHeight: Int = 0

    // 从 999999 开始，刚好可以被 3 整除，也就是从第一张开始，左右都可以滑动
    private var curCarouselPosition = 999999
    private var carouselTask: TimerTask? = null
    private val carouselTimer = Timer()
    private val carouselHandler = Handler(Looper.myLooper()!!) {
        binding.meCarousel.setCurrentItem(++curCarouselPosition, true)
        true
    }

    private val userOptions = mutableListOf(
        UserOption("我的钱包", "首充优惠"),
        UserOption("牛蛙呐活动", "日更挑战"),
        UserOption("每日任务"),
        UserOption("我的专题"),
        UserOption("浏览历史"),
    )
    private val systemUserOptions = mutableListOf(
        UserOption("设置"),
        UserOption("帮助与反馈"),
        UserOption("了解更多"),
    )

    override fun bind(inflater: LayoutInflater, container: ViewGroup?): FragmentMeBinding =
        FragmentMeBinding.inflate(inflater, container, false)

    override fun initView() {
        // 计算出横幅高度，当滑动距离超过横幅后，正好让标题栏TitleBar的透明度变为1
        bannerHeight = binding.meBanner.layoutParams.height -
                binding.meToolbar.height -
                ImmersionBar.getStatusBarHeight(this)

        // 设置轮播图属性
        setCarousel()

        // 设置用户下方选项菜单信息
        binding.meUserOptionsRv.apply {
            adapter = UserOptionsRecyclerViewAdapter(userOptions)
            layoutManager = LinearLayoutManager(requireContext())
            // 取消嵌套滑动，因为当前的 rv 是在 NestedScrollView 中的子元素
            // 设置成 false 可以在滑动时不让 rv 滑动，只滑动 NestedScrollView
            isNestedScrollingEnabled = false
        }
        binding.meUserSystemOptionsRv.apply {
            adapter = UserOptionsRecyclerViewAdapter(systemUserOptions)
            layoutManager = LinearLayoutManager(requireContext())
            // 取消嵌套滑动，因为当前的 rv 是在 NestedScrollView 中的子元素
            // 设置成 false 可以在滑动时不让 rv 滑动，只滑动 NestedScrollView
            isNestedScrollingEnabled = false
        }
    }

    override fun initObserver() {
        /*
         * 监听滑动，当滑动距离超过横幅后，正好让标题栏TitleBar的透明度变为1
         */
        binding.meScrollView.apply {
            setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                // scrollY 为总滑动距离
                val alpha = scrollY.toFloat() / bannerHeight
                binding.meToolbar.alpha = if (alpha > 1) 1f else alpha
            })
        }

        /*
         * 跳转登录
         */
        binding.meLoginClickable.setOnClickListener {
            startActivity(Intent(context, AccountActivity::class.java))
        }
        /*
         * 红包点击
         */
        binding.meRedPacketClickable.setOnClickListener {

        }
        /*
         * 扫码
         */
        binding.meScanCode.setOnClickListener {
            println("扫码扫码扫码扫码扫码扫码扫码")
        }
        /*
         * 写文章
         */
        binding.meWrite.setOnClickListener {
            println("写文章写文章写文章写文章写文章")
        }
    }

    override fun onResume() {
        super.onResume()
        setBaseStatusBar {
            titleBar(binding.meToolbar)
        }
        // 启动轮播图
        startCarousel()
    }

    override fun onPause() {
        super.onPause()
        // 停止轮播图
        stopCarousel()
    }

    /**
     * 轮播图
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCarousel() {
        binding.meCarousel.apply {
            val imgIds = mutableListOf(R.mipmap.carousel1, R.mipmap.carousel2, R.mipmap.carousel3)
            adapter = CarouselRecyclerViewAdapter(imgIds)
            // 从 999999 开始，刚好可以被 3 整除，也就是从第一张开始，并且左右都可以滑动
            setCurrentItem(999999, false)

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                // 切换页面过程中，动态移动轮播图下方小白点的位置，调用 CarouselDots 的 setSelectedDotX 即可
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    binding.meCarouselDots.setSelectedDotX(position % imgIds.size, positionOffset)
                }

                // 当切换到新的下标位置时，及时更新 curCarouselPosition 的值
                // Handler 根据 curCarouselPosition 的值进行自动滚动
                override fun onPageSelected(position: Int) {
                    curCarouselPosition = position
                }
            })
            /*
             * 轮播图手动滑动时，让计时器停止，等到手移开后重新计时
             * 注意不要返回 true，返回 true 就消费了事件，导致 vp 没办法正常滑动
             */
            getChildAt(0).setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        stopCarousel()
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        startCarousel()
                        false
                    }
                    else -> false
                }
            }
            // 轮播图下面的小点的设置
            binding.meCarouselDots.dotCount = 3
        }
    }

    /**
     * 定时发送任务给 Handler，让 Handler 修改 vp
     */
    private fun newCarouselTask() = object : TimerTask() {
        override fun run() {
            carouselHandler.sendEmptyMessage(0)
        }
    }

    /**
     * 开始定时任务，每 3s 切换一次轮播图
     */
    private fun startCarousel() {
        carouselTask = newCarouselTask()
        carouselTimer.schedule(carouselTask, 3000L, 3000L)
    }

    private fun stopCarousel() {
        carouselTask?.cancel()
        carouselTimer.purge()
    }

}