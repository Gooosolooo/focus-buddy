package io.github.gooosolooo.focusbuddy;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 待办 + 每日进度快照，存成 app 私有目录里的一个 JSON 文件；备份就是导出这个 JSON */
public class TodoStore {
    private static final String FILE = "todos.json";

    /** 某一天"开工时刻"的进度快照：待办 id -> 进度 */
    static class Snap {
        String date;       // yyyy-MM-dd
        long time;
        Map<String, Integer> p = new LinkedHashMap<>();
    }

    private static List<Todo> cache;
    private static List<Snap> snaps;

    // ---------- 待办 ----------
    static synchronized List<Todo> all(Context c) {
        ensure(c);
        return cache;
    }

    static synchronized void save(Context c) {
        ensure(c);
        try (FileOutputStream out = c.openFileOutput(FILE, Context.MODE_PRIVATE)) {
            out.write(toJsonString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }

    // ---------- 快照 ----------
    static String todayStr() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    static synchronized boolean hasSnapshotToday(Context c) {
        ensure(c);
        String d = todayStr();
        for (Snap s : snaps) if (s.date.equals(d)) return true;
        return false;
    }

    /** 记录"此刻"所有未归档待办的进度；同一天重复调用会覆盖当天那份 */
    static synchronized void takeSnapshot(Context c) {
        ensure(c);
        Snap s = new Snap();
        s.date = todayStr();
        s.time = System.currentTimeMillis();
        for (Todo t : cache) if (t.completedAt == 0) s.p.put(t.id, t.progress());
        for (int i = snaps.size() - 1; i >= 0; i--) if (snaps.get(i).date.equals(s.date)) snaps.remove(i);
        snaps.add(s);
        while (snaps.size() > 120) snaps.remove(0);
        save(c);
    }

    /** 今天开工时这条待办的进度；没有快照（或待办是今天新建的）返回 null */
    static synchronized Integer todayBase(Context c, String todoId) {
        ensure(c);
        String d = todayStr();
        for (Snap s : snaps) if (s.date.equals(d)) return s.p.get(todoId);
        return null;
    }

    // ---------- 备份 / 恢复 ----------
    static synchronized String exportJson(Context c) throws Exception {
        ensure(c);
        return toJsonString();
    }

    /** 导入并覆盖；格式不对返回 false，原数据不动。兼容没有 snapshots 的旧备份 */
    static synchronized boolean importJson(Context c, String json) {
        try {
            List<Todo> newTodos = new ArrayList<>();
            List<Snap> newSnaps = new ArrayList<>();
            parse(json, newTodos, newSnaps);
            ensure(c);
            cache.clear();
            cache.addAll(newTodos);
            snaps.clear();
            snaps.addAll(newSnaps);
            save(c);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- 内部 ----------
    private static void ensure(Context c) {
        if (cache != null) return;
        cache = new ArrayList<>();
        snaps = new ArrayList<>();
        try (FileInputStream in = c.openFileInput(FILE)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            parse(new String(bo.toByteArray(), StandardCharsets.UTF_8), cache, snaps);
        } catch (Exception ignored) { }   // 第一次运行没有文件
    }

    private static String toJsonString() throws Exception {
        JSONArray todos = new JSONArray();
        for (Todo t : cache) todos.put(t.toJson());
        JSONArray sn = new JSONArray();
        for (Snap s : snaps) {
            JSONObject so = new JSONObject();
            so.put("date", s.date);
            so.put("time", s.time);
            JSONArray items = new JSONArray();
            for (Map.Entry<String, Integer> e : s.p.entrySet()) {
                JSONObject io = new JSONObject();
                io.put("id", e.getKey());
                io.put("p", e.getValue());
                items.put(io);
            }
            so.put("items", items);
            sn.put(so);
        }
        JSONObject root = new JSONObject();
        root.put("version", 2);
        root.put("exportedAt", System.currentTimeMillis());
        root.put("todos", todos);
        root.put("snapshots", sn);
        return root.toString(2);
    }

    private static void parse(String json, List<Todo> outTodos, List<Snap> outSnaps) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray arr = root.getJSONArray("todos");
        for (int i = 0; i < arr.length(); i++) outTodos.add(Todo.fromJson(arr.getJSONObject(i)));
        JSONArray sn = root.optJSONArray("snapshots");
        if (sn != null) {
            for (int i = 0; i < sn.length(); i++) {
                JSONObject so = sn.getJSONObject(i);
                Snap s = new Snap();
                s.date = so.optString("date", "");
                s.time = so.optLong("time", 0);
                JSONArray items = so.optJSONArray("items");
                if (items != null) {
                    for (int j = 0; j < items.length(); j++) {
                        JSONObject io = items.getJSONObject(j);
                        s.p.put(io.optString("id"), io.optInt("p", 0));
                    }
                }
                outSnaps.add(s);
            }
        }
    }
}
