package io.github.gooosolooo.focusbuddy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 一条待办。规则：只有"最小一级"的项目可以直接编辑进度。
 *  - 没有子项目：用自己的 progress（0~100，以 10 为一档）
 *  - 有子项目：进度 = 所有子项目进度的平均值（只读）
 */
public class Todo {
    public String id = UUID.randomUUID().toString();
    public String title = "";
    public long createdAt = System.currentTimeMillis();
    public long completedAt = 0;          // 0 = 未归档；>0 = 已归档到【已完成】
    public int progress = 0;              // 仅在没有子项目时使用
    public List<Sub> subs = new ArrayList<>();

    public static class Sub {
        public String id = UUID.randomUUID().toString();
        public String text = "";
        public int progress = 0;          // 0~100
    }

    /** 对外展示的进度 */
    public int progress() {
        if (subs.isEmpty()) return progress;
        int sum = 0;
        for (Sub s : subs) sum += s.progress;
        return Math.round((float) sum / subs.size());
    }

    /** 已经 100% 的子项目个数 */
    public int doneCount() {
        int d = 0;
        for (Sub s : subs) if (s.progress >= 100) d++;
        return d;
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("createdAt", createdAt);
        o.put("completedAt", completedAt);
        o.put("progress", progress);
        JSONArray arr = new JSONArray();
        for (Sub s : subs) {
            JSONObject so = new JSONObject();
            so.put("id", s.id);
            so.put("text", s.text);
            so.put("progress", s.progress);
            arr.put(so);
        }
        o.put("subs", arr);
        return o;
    }

    /** 兼容旧版数据：旧版只有 done（true/false），转成 100 / 0 */
    static Todo fromJson(JSONObject o) throws JSONException {
        Todo t = new Todo();
        t.id = o.optString("id", t.id);
        t.title = o.optString("title", "");
        t.createdAt = o.optLong("createdAt", t.createdAt);
        t.completedAt = o.optLong("completedAt", 0);
        t.progress = o.has("progress") ? o.optInt("progress", 0) : (o.optBoolean("done", false) ? 100 : 0);
        JSONArray arr = o.optJSONArray("subs");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject so = arr.getJSONObject(i);
                Sub s = new Sub();
                s.id = so.optString("id", s.id);
                s.text = so.optString("text", "");
                s.progress = so.has("progress") ? so.optInt("progress", 0) : (so.optBoolean("done", false) ? 100 : 0);
                t.subs.add(s);
            }
        }
        return t;
    }
}
