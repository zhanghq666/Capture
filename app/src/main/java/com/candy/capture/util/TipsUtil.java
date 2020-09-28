package com.candy.capture.util;

import android.content.Context;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;
import android.widget.Toast;

/**
 * Created by zhanghq on 2016/10/30.
 */

public class TipsUtil {

    public static void showToast(Context context, String msg) {
        Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public static void showSnackbar(View view, String msg) {
        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
    }

    public static void showSnackbar(View view, String msg, String action, View.OnClickListener listener) {
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                .setAction(action, listener)
                .show();
    }
}
