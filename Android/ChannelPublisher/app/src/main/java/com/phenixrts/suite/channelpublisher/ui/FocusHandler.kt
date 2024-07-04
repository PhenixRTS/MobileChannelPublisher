/*
 * Copyright 2024 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui

import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.phenixrts.suite.channelpublisher.databinding.FocusHandlerBinding
import com.phenixrts.suite.phenixcommon.common.launchMain
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

class FocusHandler : CoordinatorLayout {
    private lateinit var binding: FocusHandlerBinding
    private lateinit var hideIconRunnable: Runnable
    private var focusResetRunnable: Runnable? = null

    private val touchEvents = MutableSharedFlow<Point>(replay = 1)

    val touchFlow: SharedFlow<Point> = touchEvents

    fun showFocusPosition(position: Point) {
        // Cancel icon hiding if it was pending
        handler.removeCallbacks(hideIconRunnable)

        binding.focusIcon.x = position.x - binding.focusIcon.width / 2f
        binding.focusIcon.y = position.y - binding.focusIcon.height / 2f

        var scaleAnimation = ScaleAnimation(
            1.5f, 1f, 1.5f, 1f,
            Animation.ABSOLUTE, position.x.toFloat(),
            Animation.ABSOLUTE, position.y.toFloat()
        )
        scaleAnimation.duration = 500
        binding.focusIcon.startAnimation(scaleAnimation)

        binding.focusIcon.visibility = View.VISIBLE
        handler.postDelayed(hideIconRunnable, 1500)

        // Once the focus is set to a certain distance, it stays there.
        // The focus must be reset at some point. Some options to reset it are for example to detect
        // a movement of the device, an reset button for a user to press, or automatically after
        // a delay. Here it is done after a few seconds.
        focusResetRunnable?.run{
            handler.removeCallbacks(this)
            handler.postDelayed(this, 5000)
        }
    }

    fun setFocusResetMethod(method: () -> Unit) {
        focusResetRunnable = Runnable {
            method()
        }
    }

    constructor(context: Context) : super(context) {
        initView()
    }


    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
    }

    fun initView() {
        binding = FocusHandlerBinding.inflate(LayoutInflater.from(context), this, true)
        hideIconRunnable = Runnable {
            binding.focusIcon.visibility = View.GONE
        }

        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                touchEvents.tryEmit(Point(event.x.toInt(), event.y.toInt()))
            }

            false
        }
    }
}