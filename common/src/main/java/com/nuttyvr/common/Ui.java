package com.nuttyvr.common;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class Ui {
    public static final int BG = Color.rgb(16, 21, 16), PANEL = Color.rgb(28, 36, 28), MINT = Color.rgb(169, 239, 100), WHITE = Color.rgb(242, 247, 239), MUTED = Color.rgb(174, 188, 171);
    private Ui() {}
    public static int dp(Activity activity, int size) { return Math.round(size * activity.getResources().getDisplayMetrics().density); }
    public static GradientDrawable shape(int color, int stroke) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(18);
        if (stroke != 0) drawable.setStroke(2, stroke); return drawable;
    }
    public static LinearLayout page(Activity activity, String title, String subtitle) {
        ScrollView scroll = new ScrollView(activity); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout outer = new LinearLayout(activity); outer.setOrientation(LinearLayout.VERTICAL); outer.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(outer);
        LinearLayout page = new LinearLayout(activity); page.setOrientation(LinearLayout.VERTICAL);
        int width = Math.min(activity.getResources().getDisplayMetrics().widthPixels, dp(activity, 920));
        outer.addView(page, new LinearLayout.LayoutParams(width, -2));
        int padding = dp(activity, 28); page.setPadding(padding, padding, padding, padding);
        text(page, "N / VR      ·      NUTTYVR PROJECT", 14, MINT);
        text(page, title, 34, WHITE).setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text(page, subtitle, 17, MUTED);
        activity.setContentView(scroll); return page;
    }
    public static LinearLayout panel(LinearLayout parent) {
        Activity activity = (Activity) parent.getContext();
        LinearLayout panel = new LinearLayout(activity); panel.setOrientation(LinearLayout.VERTICAL); panel.setBackground(shape(PANEL, 0));
        int padding = dp(activity, 20); panel.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(activity, 18); parent.addView(panel, params); return panel;
    }
    public static TextView text(LinearLayout parent, String content, int size, int color) {
        TextView view = new TextView(parent.getContext()); view.setText(content); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, 8, 0, 12); view.setLineSpacing(2, 1.1f); parent.addView(view, new LinearLayout.LayoutParams(-1, -2)); return view;
    }
    public static Button button(LinearLayout parent, String label, boolean primary, Runnable action) {
        Activity activity = (Activity) parent.getContext(); Button button = new Button(activity);
        button.setText(label); button.setTextSize(16); button.setAllCaps(false); button.setGravity(Gravity.CENTER); button.setMinHeight(dp(activity, 58));
        button.setTextColor(primary ? BG : WHITE); button.setBackground(shape(primary ? MINT : BG, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(activity, 10); parent.addView(button, params);
        button.setOnFocusChangeListener((v, focused) -> v.setBackground(shape(primary ? MINT : BG, focused ? WHITE : 0)));
        button.setOnHoverListener((v, event) -> { if (!v.hasFocus()) v.setAlpha(event.getAction() == android.view.MotionEvent.ACTION_HOVER_EXIT ? 1f : 0.85f); return false; });
        button.setOnClickListener(view -> action.run()); return button;
    }
}
