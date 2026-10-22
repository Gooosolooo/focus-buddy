package io.github.gooosolooo.focusbuddy;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;

import java.util.function.BooleanSupplier;

/** 统一应用弹窗的卡片、文字、按钮和安全区留白。 */
final class AppDialog {
    /** 弹窗最大宽度，单位 dp。 */
    private static final int MAX_WIDTH_DP = 360;
    /** 弹窗与屏幕安全区域左右边缘的最小距离，单位 dp。 */
    private static final int HORIZONTAL_MARGIN_DP = 24;
    /** 弹窗内容四周的内边距，单位 dp。 */
    private static final int CONTENT_PADDING_DP = 24;
    /** 标题、正文、输入区域和按钮行之间的间距，单位 dp。 */
    private static final int CONTENT_GAP_DP = 16;
    /** 操作按钮之间的间距，单位 dp。 */
    private static final int BUTTON_GAP_DP = 8;
    /** 操作按钮最小高度，单位 dp。 */
    private static final int BUTTON_MIN_HEIGHT_DP = 48;

    private AppDialog() {}

    static final class Builder {
        private final Context context;
        private String title;
        private String message;
        private View customView;
        private String positiveLabel;
        private String negativeLabel;
        private int positiveColor = Ui.SAGE;
        private boolean cancelable = true;
        private DialogInterface.OnClickListener positiveListener;
        private DialogInterface.OnClickListener negativeListener;
        private BooleanSupplier positiveAction;

        Builder(Context context) {
            this.context = context;
        }

        Builder setTitle(String value) {
            title = value;
            return this;
        }

        Builder setTitle(int resource) {
            return setTitle(context.getString(resource));
        }

        Builder setMessage(String value) {
            message = value;
            return this;
        }

        Builder setMessage(int resource) {
            return setMessage(context.getString(resource));
        }

        Builder setView(View view) {
            customView = view;
            return this;
        }

        Builder setCancelable(boolean value) {
            cancelable = value;
            return this;
        }

        Builder setPositiveButton(String label, DialogInterface.OnClickListener listener) {
            positiveLabel = label;
            positiveListener = listener;
            positiveAction = null;
            return this;
        }

        Builder setPositiveButtonColor(int color) {
            positiveColor = color;
            return this;
        }

        /** 返回 false 时保留弹窗，供授权或输入校验等操作使用。 */
        Builder setPositiveAction(int labelResource, BooleanSupplier action) {
            positiveLabel = context.getString(labelResource);
            positiveAction = action;
            positiveListener = null;
            return this;
        }

        Builder setNegativeButton(String label, DialogInterface.OnClickListener listener) {
            negativeLabel = label;
            negativeListener = listener;
            return this;
        }

        Builder setNegativeButton(int resource, DialogInterface.OnClickListener listener) {
            return setNegativeButton(context.getString(resource), listener);
        }

        AlertDialog create() {
            LinearLayout content = Ui.card(context);
            int padding = Ui.dp(context, CONTENT_PADDING_DP);
            content.setPadding(padding, padding, padding, padding);
            if (title != null) {
                append(content, Ui.text(context, title, 18, Ui.TITLE, true));
            }
            if (message != null) {
                append(content, Ui.text(context, message, 14, Ui.INK, false));
            }
            if (customView != null) {
                append(content, customView);
            }
            ScrollView scroll = new ScrollView(context);
            scroll.addView(content);
            AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(scroll)
                .setCancelable(cancelable)
                .create();
            LinearLayout actions = Ui.row(context);
            actions.setBaselineAligned(false);
            if (negativeLabel != null) {
                addButton(actions, negativeLabel, Ui.SAND, view -> {
                    if (negativeListener != null) {
                        negativeListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
                    }
                    dialog.dismiss();
                });
            }
            if (positiveLabel != null) {
                addButton(actions, positiveLabel, positiveColor, view -> {
                    if (positiveAction != null && !positiveAction.getAsBoolean()) {
                        return;
                    }
                    if (positiveListener != null) {
                        positiveListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                    }
                    dialog.dismiss();
                });
            }
            if (actions.getChildCount() > 0) {
                append(content, actions);
            }
            dialog.setCanceledOnTouchOutside(cancelable);
            dialog.setOnShowListener(view -> applyWindowStyle(dialog, context));
            return dialog;
        }

        AlertDialog show() {
            AlertDialog dialog = create();
            dialog.show();
            return dialog;
        }

        private void append(LinearLayout content, View view) {
            content.addView(view, Ui.lp(context, content.getChildCount() == 0 ? 0 : CONTENT_GAP_DP));
        }

        private void addButton(LinearLayout actions, String label, int color, View.OnClickListener listener) {
            Button button = Ui.button(context, label, color, listener);
            button.setMinimumHeight(Ui.dp(context, BUTTON_MIN_HEIGHT_DP));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (actions.getChildCount() > 0) {
                params.setMarginStart(Ui.dp(context, BUTTON_GAP_DP));
            }
            actions.addView(button, params);
        }
    }

    private static void applyWindowStyle(AlertDialog dialog, Context context) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowMetrics metrics = manager.getCurrentWindowMetrics();
        Insets safe = metrics.getWindowInsets().getInsets(
            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        int availableWidth = metrics.getBounds().width() - safe.left - safe.right;
        int width = Math.min(Ui.dp(context, MAX_WIDTH_DP),
            availableWidth - Ui.dp(context, HORIZONTAL_MARGIN_DP) * 2);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(Math.max(1, width), ViewGroup.LayoutParams.WRAP_CONTENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }
}
