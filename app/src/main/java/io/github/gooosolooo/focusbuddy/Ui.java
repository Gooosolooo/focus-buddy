package io.github.gooosolooo.focusbuddy;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 界面小工具：莫兰迪配色 + 圆角控件
 */
final class Ui {
    private Ui() {}

    static final int BG = 0xFFF2EFEA;     // 背景：暖灰
    static final int CARD = 0xFFFBFAF7;   // 卡片
    static final int INK = 0xFF3F3B38;    // 正文
    static final int SUB = 0xFF857D76;    // 次要文字
    static final int TRACK = 0xFFE4DED6;  // 进度条底色
    static final int DEEP = 0xFF6F8A76;   // 深鼠尾草绿（强调文字/勾选框）
    static final int TITLE = 0xFF506756;  // 深莫兰迪鼠尾草绿（待办标题）
    static final int ACTION_BLUE = 0xFF607D94; // 深雾霾蓝（添加操作）
    static final int SELECT_BG = 0xFFF0F4EF; // 待办选择区域的浅绿色底色

    static final int SAGE = 0xFFA9BAAC;   // 鼠尾草绿
    static final int BLUE = 0xFF9DB0C2;   // 雾霾蓝
    static final int ROSE = 0xFFD3B3AF;   // 豆沙粉
    static final int MAUVE = 0xFFBEAFC4;  // 藕紫
    static final int SAND = 0xFFE0D5C4;   // 燕麦米

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(null, Typeface.BOLD);
        }
        return t;
    }

    static void setFill(View v, int color) {
        Context c = v.getContext();
        float r = dp(c, 20);
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000), round(color, r), round(0xFFFFFFFF, r)));
    }

    static Button button(Context c, String text, int color, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(INK);
        b.setTextSize(15);
        b.setStateListAnimator(null);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10));
        setFill(b, color);
        if (l != null) {
            b.setOnClickListener(l);
        }
        return b;
    }

    /**
     * 统一的无底色图标按钮，保留 48dp 点击区域及同色按压反馈。
     */
    static ImageButton iconButton(Context context, int iconResource, int color, String description,
        View.OnClickListener listener) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(iconResource);
        button.setImageTintList(ColorStateList.valueOf(color));
        button.setScaleType(ImageView.ScaleType.CENTER);
        int padding = dp(context, 12);
        button.setPadding(padding, padding, padding, padding);
        button.setMinimumWidth(dp(context, 48));
        button.setMinimumHeight(dp(context, 48));
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 48), dp(context, 48)));
        button.setOnClickListener(listener);
        button.setContentDescription(description);
        button.setBackground(new RippleDrawable(
            ColorStateList.valueOf((color & 0x00FFFFFF) | 0x22000000),
            null, round(0xFFFFFFFF, dp(context, 24))));
        return button;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        l.setBackground(round(CARD, dp(c, 18)));
        l.setElevation(dp(c, 1));
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout.LayoutParams lp(Context c, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(c, topDp);
        return p;
    }

    static EditText edit(Context c, String hint, String value, boolean number) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setText(value);
        e.setTextSize(15);
        e.setTextColor(INK);
        e.setHintTextColor(0xFFB5ADA5);
        e.setSingleLine(true);
        if (number) {
            e.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        GradientDrawable g = round(0xFFFFFFFF, dp(c, 12));
        g.setStroke(dp(c, 1), TRACK);
        e.setBackground(g);
        e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        return e;
    }

    /**
     * 圆角进度条
     */
    static ProgressBar bar(Context c, int color) {
        ProgressBar pb = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        float r = dp(c, 4);
        ClipDrawable fill = new ClipDrawable(round(color, r), Gravity.START, ClipDrawable.HORIZONTAL);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{round(TRACK, r), fill});
        ld.setId(0, android.R.id.background);
        ld.setId(1, android.R.id.progress);
        pb.setProgressDrawable(ld);
        return pb;
    }
}
