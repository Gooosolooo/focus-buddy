package io.github.gooosolooo.focusbuddy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 一条待办。有子项目时，进度 = 已完成子项目 / 全部子项目；没有子项目时，由 done 决定 0% 或 100% */
public class Todo {
    public String id = UUID.randomUUID().toString();
    public String title = "";
    public long createdAt = System.currentTimeMillis();
    public long completedAt = 0;          // 0 = 未归档；>0 = 已归档到【已完成】
    public boolean done = false;          // 仅在没有子项目时使用
    public List<Sub> subs = new ArrayList<>();

    public static class Sub {
        public String id = UUID.randomUUID().toString();
        public String text = "";
        public boolean done = false;
    }

    public int progress() {
        if (subs.isEmpty()) return done ? 100 : 0;
        int d = 0;
        for (Sub s : subs) if (s.done) d++;
        return Math.round(100f * d / subs.size());
    }

    public int doneCount() {
        int d = 0;
        for (Sub s : subs) if (s.done) d++;
        return d;
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("createdAt", createdAt);
        o.put("completedAt", completedAt);
        o.put("done", done);
        JSONArray arr = new JSONArray();
        for (Sub s : subs) {
            JSONObject so = new JSONObject();
            so.put("id", s.id);
            so.put("text", s.text);
            so.put("done", s.done);
            arr.put(so);
        }
        o.put("subs", arr);
        return o;
    }

    static Todo fromJson(JSONObject o) throws JSONException {
        Todo t = new Todo();
        t.id = o.optString("id", t.id);
        t.title = o.optString("title", "");
        t.createdAt = o.optLong("createdAt", t.createdAt);
        t.completedAt = o.optLong("completedAt", 0);
        t.done = o.optBoolean("done", false);
        JSONArray arr = o.optJSONArray("subs");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject so = arr.getJSONObject(i);
                Sub s = new Sub();
                s.id = so.optString("id", s.id);
                s.text = so.optString("text", "");
                s.done = so.optBoolean("done", false);
                t.subs.add(s);
            }
        }
        return t;
    }
}
