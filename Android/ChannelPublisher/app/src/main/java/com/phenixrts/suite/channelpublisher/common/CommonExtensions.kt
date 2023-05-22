/*
 * Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.common

import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.phenixrts.suite.channelpublisher.R
import com.phenixrts.suite.channelpublisher.common.enums.ExpressError
import com.phenixrts.suite.phenixcommon.common.launchMain
import kotlin.system.exitProcess

private fun AppCompatActivity.closeApp() {
    finishAffinity()
    finishAndRemoveTask()
    exitProcess(0)
}

fun AppCompatActivity.showErrorDialog(error: ExpressError) {
    AlertDialog.Builder(this, R.style.AlertDialogTheme)
        .setCancelable(false)
        .setMessage(getErrorMessage(error))
        .setPositiveButton(getString(R.string.popup_ok)) { dialog, _ ->
            dialog.dismiss()
            if (error != ExpressError.STREAM_ERROR) {
                closeApp()
            }
        }
        .create()
        .show()
}

private fun AppCompatActivity.getErrorMessage(error: ExpressError): String {
    return when (error) {
        ExpressError.DEEP_LINK_ERROR -> getString(R.string.err_invalid_deep_link)
        ExpressError.UNRECOVERABLE_ERROR -> getString(R.string.err_unrecoverable_error)
        ExpressError.STREAM_ERROR -> getString(R.string.err_stream_error)
        ExpressError.CONFIGURATION_CHANGED_ERROR -> getString(R.string.err_configuration_changed)
    }
}

fun View.showSnackBar(message: String, duration: Int = Snackbar.LENGTH_INDEFINITE) = launchMain {
    Snackbar.make(this@showSnackBar, message, duration).show()
}

fun Spinner.onSelectionChanged(callback: (Int) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            /* Ignored */
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            callback(position)
        }
    }
}
