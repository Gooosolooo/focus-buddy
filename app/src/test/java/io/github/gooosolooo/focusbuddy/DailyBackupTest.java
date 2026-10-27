package io.github.gooosolooo.focusbuddy;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** 仅识别本功能生成的日期文件，避免清理用户文件或手动备份。 */
public class DailyBackupTest {
    @Test
    public void recognizesDailyBackupIncludingLeapDay() {
        assertEquals(LocalDate.of(2028, 2, 29),
            DailyBackup.parseDate("focusbuddy-daily-2028-02-29.json"));
    }

    @Test
    public void ignoresManualBackupsAndOtherFiles() {
        assertNull(DailyBackup.parseDate("focusbuddy-backup-20261027-1400.json"));
        assertNull(DailyBackup.parseDate("notes.json"));
        assertNull(DailyBackup.parseDate(".focusbuddy-pending-example.tmp"));
        assertNull(DailyBackup.parseDate("focusbuddy-daily-2026-10-27 (1).json"));
        assertNull(DailyBackup.parseDate(null));
    }

    @Test
    public void rejectsInvalidOrNonCanonicalDates() {
        assertNull(DailyBackup.parseDate("focusbuddy-daily-2026-02-29.json"));
        assertNull(DailyBackup.parseDate("focusbuddy-daily-2026-2-09.json"));
        assertNull(DailyBackup.parseDate("focusbuddy-daily-.json"));
    }
}
