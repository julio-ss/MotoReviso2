package br.jss.motoreviso.utils;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SystemBarHelper {

    public static void applySystemBarPadding(Activity activity, View rootView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    public static void applyEdgeToEdge(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }
}
