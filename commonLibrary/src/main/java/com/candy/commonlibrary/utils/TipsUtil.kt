package com.candy.commonlibrary.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 16:21
 */
object TipsUtil {
    fun showToast(context: Context?, msg: String?) {
        context?.apply {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun showSnackbar(view: View?, msg: String?) {
        Snackbar.make(view!!, msg!!, Snackbar.LENGTH_SHORT).show()
    }

    fun showSnackbar(view: View?, msg: String?, action: String?, listener: View.OnClickListener?) {
        Snackbar.make(view!!, msg!!, Snackbar.LENGTH_LONG)
                .setAction(action, listener)
                .show()
    }
}