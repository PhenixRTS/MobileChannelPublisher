/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

@SuppressLint("Registered")
open class EasyPermissionActivity : AppCompatActivity() {

    private val permissionRequestHistory = hashMapOf<Int, (a: Boolean) -> Unit>()

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED

    fun arePermissionsGranted(): Boolean = hasCameraPermission() && hasRecordAudioPermission()

    fun askForPermissions(callback: (granted: Boolean) -> Unit) {
        run {
            val permissions = arrayListOf<String>()
            if (!hasRecordAudioPermission()) {
                permissions.add(Manifest.permission.RECORD_AUDIO)
            }
            if (!hasCameraPermission()) {
                permissions.add(Manifest.permission.CAMERA)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionRequestHistory[requestCode]?.run {
            this(grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED)
            permissionRequestHistory.remove(requestCode)
        }
    }

    private fun Int.low16bits() = this and 0xFFFF

}
