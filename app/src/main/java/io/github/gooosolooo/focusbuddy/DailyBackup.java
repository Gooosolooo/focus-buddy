package io.github.gooosolooo.focusbuddy;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 开工核对后的本地每日备份，通过系统目录授权访问用户选择的位置。 */
final class DailyBackup {
    private static final String TAG = "DailyBackup";
    private static final String PREFERENCES = "daily_backup";
    private static final String DIRECTORY = "directory";
    private static final String PREFIX = "focusbuddy-daily-";
    private static final String SUFFIX = ".json";
    private static final int RETENTION_DAYS = 30;
    private static final int DIRECTORY_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION
        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1,
        0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(16),
        task -> new Thread(task, "focusbuddy-daily-backup"), new ThreadPoolExecutor.AbortPolicy());

    private DailyBackup() {
    }

    static Uri directory(Context context) {
        String value = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(DIRECTORY, null);
        return value == null ? null : Uri.parse(value);
    }

    static void setDirectory(Context context, Uri directory) {
        context.getContentResolver().takePersistableUriPermission(directory, DIRECTORY_FLAGS);
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(DIRECTORY, directory.toString()).apply();
    }

    static String directoryLabel(Context context) {
        Uri directory = directory(context);
        if (directory == null) {
            return context.getString(R.string.daily_backup_not_configured);
        }
        // 展示系统提供的卷与目录标识，不假定文档 URI 可以转换成文件路径。
        return DocumentsContract.getTreeDocumentId(directory);
    }

    static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }

    /** 先完整写入临时文件，再发布当天备份，成功后才清理旧文件。 */
    static boolean write(Context context, Uri directory, String json, LocalDate date) throws Exception {
        Uri parent = DocumentsContract.buildDocumentUriUsingTree(directory,
            DocumentsContract.getTreeDocumentId(directory));
        List<BackupDocument> documents = list(context, directory);
        String name = PREFIX + date + SUFFIX;
        Uri temporary = DocumentsContract.createDocument(context.getContentResolver(), parent,
            "application/json", ".focusbuddy-pending-" + UUID.randomUUID() + ".tmp");
        if (temporary == null) {
            throw new IOException("无法创建备份文件，请重新选择可写入的本地目录");
        }
        boolean published = false;
        Uri previous = null;
        BackupDocument existing = null;
        try {
            try (OutputStream output = context.getContentResolver().openOutputStream(temporary, "wt")) {
                if (output == null) {
                    throw new IOException("无法打开备份文件");
                }
                output.write(json.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
            for (BackupDocument document : documents) {
                if (name.equals(document.name)) {
                    existing = document;
                    previous = DocumentsContract.renameDocument(context.getContentResolver(),
                        document.uri, ".focusbuddy-previous-" + UUID.randomUUID() + ".tmp");
                    if (previous == null) {
                        throw new IOException("无法更新当天备份，原备份已保留");
                    }
                    break;
                }
            }
            Uri renamed = DocumentsContract.renameDocument(context.getContentResolver(), temporary, name);
            if (renamed == null) {
                throw new IOException("无法保存备份文件名，原备份已保留");
            }
            published = true;
        } finally {
            if (!published) {
                delete(context, temporary);
                if (previous != null) {
                    DocumentsContract.renameDocument(context.getContentResolver(), previous, name);
                }
            }
        }
        boolean cleaned = true;
        if (previous != null) {
            cleaned = delete(context, previous);
        }
        LocalDate oldest = date.minusDays(RETENTION_DAYS - 1L);
        for (BackupDocument document : documents) {
            if (document == existing) {
                continue;
            }
            LocalDate backupDate = parseDate(document.name);
            if (backupDate != null && (backupDate.equals(date) || backupDate.isBefore(oldest))) {
                cleaned &= delete(context, document.uri);
            }
        }
        return cleaned;
    }

    static LocalDate parseDate(String name) {
        if (name == null || !name.startsWith(PREFIX) || !name.endsWith(SUFFIX)) {
            return null;
        }
        String value = name.substring(PREFIX.length(), name.length() - SUFFIX.length());
        try {
            LocalDate date = LocalDate.parse(value);
            return date.toString().equals(value) ? date : null;
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static List<BackupDocument> list(Context context, Uri directory) throws IOException {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(directory,
            DocumentsContract.getTreeDocumentId(directory));
        String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE};
        List<BackupDocument> documents = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(children, columns, null, null, null)) {
            if (cursor == null) {
                throw new IOException("无法读取备份目录，请重新选择目录");
            }
            while (cursor.moveToNext()) {
                if (!DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))) {
                    documents.add(new BackupDocument(DocumentsContract.buildDocumentUriUsingTree(
                        directory, cursor.getString(0)), cursor.getString(1)));
                }
            }
        }
        return documents;
    }

    private static boolean delete(Context context, Uri document) {
        try {
            return DocumentsContract.deleteDocument(context.getContentResolver(), document);
        } catch (Exception exception) {
            Log.w(TAG, "无法清理备份文件", exception);
            return false;
        }
    }

    private static final class BackupDocument {
        private final Uri uri;
        private final String name;

        private BackupDocument(Uri uri, String name) {
            this.uri = uri;
            this.name = name;
        }
    }
}
