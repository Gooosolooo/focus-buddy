package io.github.gooosolooo.focusbuddy;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 把待办存成 app 私有目录里的一个 JSON 文件（todos.json）。简单、没有依赖，备份时直接导出这个 JSON */
public class TodoStore {
    private static final String FILE = "todos.json";
    private static List<Todo> cache;

    static synchronized List<Todo> all(Context c) {
        if (cache == null) cache = read(c);
        return cache;
    }

    static synchronized void save(Context c) {
        try (FileOutputStream out = c.openFileOutput(FILE, Context.MODE_PRIVATE)) {
            out.write(toJsonString(all(c)).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }

    static String exportJson(Context c) throws Exception {
        return toJsonString(all(c));
    }

    /** 导入并覆盖现有待办；格式不对返回 false，原数据不会被动 */
    static synchronized boolean importJson(Context c, String json) {
        try {
            List<Todo> list = parse(json);
            all(c).clear();
            all(c).addAll(list);
            save(c);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String toJsonString(List<Todo> list) throws Exception {
        JSONArray arr = new JSONArray();
        for (Todo t : list) arr.put(t.toJson());
        JSONObject root = new JSONObject();
        root.put("version", 1);
        root.put("exportedAt", System.currentTimeMillis());
        root.put("todos", arr);
        return root.toString(2);
    }

    private static List<Todo> parse(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray arr = root.getJSONArray("todos");
        List<Todo> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) list.add(Todo.fromJson(arr.getJSONObject(i)));
        return list;
    }

    private static List<Todo> read(Context c) {
        try (FileInputStream in = c.openFileInput(FILE)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return parse(new String(bo.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new ArrayList<>();   // 第一次运行没有文件，返回空列表
        }
    }
}
