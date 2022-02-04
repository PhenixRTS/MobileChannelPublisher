/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.phenixcore.PhenixCore
import com.phenixrts.suite.phenixdeeplinks.DeepLinkActivity
import java.util.*
import javax.inject.Inject

@SuppressLint("Registered")
abstract class EasyPermissionActivity : DeepLinkActivity() {

    @Inject lateinit var phenixCore: PhenixCore

    private val permissionRequestHistory = hashMapOf<Int, (a: Boolean) -> Unit>()

    override val additionalConfiguration = hashMapOf(Pair("publishingEnabled", "true"))

    override fun onCreate(savedInstanceState: Bundle?) {
        ChannelPublisherApplication.component.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionRequestHistory[requestCode]?.run {
            this(grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED)
            permissionRequestHistory.remove(requestCode)
        }
    }

    fun arePermissionsGranted(): Boolean = hasCameraPermission() && hasRecordAudioPermission() && hasBluetoothPermission()

    fun askForPermissions(callback: (granted: Boolean) -> Unit) {
        run {
            val permissions = arrayListOf<String>()
            if (!hasRecordAudioPermission()) {
                permissions.add(Manifest.permission.RECORD_AUDIO)
            }
            if (!hasCameraPermission()) {
                permissions.add(Manifest.permission.CAMERA)
            }
            if (!hasBluetoothPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (permissions.isNotEmpty()) {
                val requestCode = Date().time.toInt().low16bits()
                permissionRequestHistory[requestCode] = callback
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), requestCode)
            } else {
                callback(true)
            }
        }
    }

    private fun Int.low16bits() = this and 0xFFFF

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED

    private fun hasBluetoothPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PERMISSION_GRANTED else true

}
