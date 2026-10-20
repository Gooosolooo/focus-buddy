package io.github.gooosolooo.focusbuddy;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 底部操作指南，展开时限制高度，内容可独立滚动。 */
final class OperationGuideView extends LinearLayout {
    private static final int MAX_CONTENT_HEIGHT_DP = 200;

    OperationGuideView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(Ui.dp(context, 16), 0, Ui.dp(context, 16), Ui.dp(context, 8));

        TextView header = Ui.text(context, "▸ 操作指南", 14, Ui.DEEP, true);
        header.setPadding(0, Ui.dp(context, 12), 0, Ui.dp(context, 12));
        addView(header);

        ScrollView content = new ScrollView(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int maxHeight = Ui.dp(getContext(), MAX_CONTENT_HEIGHT_DP);
                if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
                    maxHeight = Math.min(maxHeight, MeasureSpec.getSize(heightMeasureSpec));
                }
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
            }
        };
        LinearLayout entries = new LinearLayout(context);
        entries.setOrientation(VERTICAL);
        String[] instructions = {
            "> 修改名称：单击待办或子项目标题。",
            "> 展开/收起：单击待办左侧 ▸ / ▾。",
            "> 调整进度：按住进度条圆点拖动，每档 10%；无子项目的待办可直接调整。",
            "> 查看已完成：单击“已完成: 数量/总数”展开或收起。",
            "> 恢复未完成：将已完成子项目的进度调至 100% 以下。",
            "> 添加子项目：单击卡片左下角 +。",
            "> 悬浮计时：单击卡片右下角“悬浮窗计时”。",
            "> 删除：长按待办或子项目标题，确认后删除。",
            "> 自动归档：待办进度达到 100% 后移入“已完成”页。"
        };
        for (String instruction : instructions) {
            TextView entry = Ui.text(context, instruction, 13, Ui.INK, false);
            entry.setPadding(0, 0, 0, Ui.dp(context, 8));
            entries.addView(entry);
        }
        content.addView(entries);
        content.setVisibility(View.GONE);
        addView(content, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        header.setOnClickListener(view -> {
            boolean expanded = content.getVisibility() == View.VISIBLE;
            content.setVisibility(expanded ? View.GONE : View.VISIBLE);
            header.setText(expanded ? "▸ 操作指南" : "▾ 操作指南");
        });
    }
}
