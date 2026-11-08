package com.le1567.spring;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/**
 * 除夕 20:00 与春节 00:05 的本地提醒。
 * 使用精确闹钟；无权限时退化为窗口闹钟。
 */
public class FestivalScheduler {

    /** 2026-2036 春节（正月初一）的北京日期 */
    private static final int[][] CNY = {
            {2026, 2, 17}, {2027, 2, 6}, {2028, 1, 26}, {2029, 2, 13},
            {2030, 2, 3}, {2031, 1, 23}, {2032, 2, 11}, {2033, 1, 31},
            {2034, 2, 19}, {2035, 2, 8}, {2036, 1, 28}
    };

    private static final String[] GANZHI = {
            "丙午", "丁未", "戊申", "己酉", "庚戌",
            "辛亥", "壬子", "癸丑", "甲寅", "乙卯", "丙辰"
    };

    private static final String[] ZODIAC = {
            "马", "羊", "猴", "鸡", "狗", "猪", "鼠", "牛", "虎", "兔", "龙"
    };

    private static Calendar cnyCalendar(int year) {
        for (int[] r : CNY) {
            if (r[0] == year) {
                Calendar c = Calendar.getInstance();
                c.clear();
                c.set(r[0], r[1] - 1, r[2], 0, 5, 0);
                return c;
            }
        }
        return null;
    }

    private static Calendar nextCny() {
        Calendar now = Calendar.getInstance();
        for (int[] r : CNY) {
            Calendar c = cnyCalendar(r[0]);
            if (c != null && c.getTimeInMillis() > now.getTimeInMillis()) return c;
        }
        return null;
    }

    private static int idxOf(int year) {
        for (int i = 0; i < CNY.length; i++) if (CNY[i][0] == year) return i;
        return -1;
    }

    static String describeNext() {
        Calendar c = nextCny();
        if (c == null) return "";
        long days = (c.getTimeInMillis() - System.currentTimeMillis() + 86399999L) / 86400000L;
        int i = idxOf(c.get(Calendar.YEAR));
        String gz = i >= 0 ? GANZHI[i] : "";
        String z = i >= 0 ? ZODIAC[i] : "";
        return c.get(Calendar.YEAR) + "|" + days + "|" + gz + "|" + z;
    }

    static void schedule(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Calendar now = Calendar.getInstance();
        for (int[] r : CNY) {
            Calendar day = cnyCalendar(r[0]);
            if (day == null) continue;
            Calendar eve = (Calendar) day.clone();
            eve.add(Calendar.DAY_OF_MONTH, -1);
            eve.set(Calendar.HOUR_OF_DAY, 20);
            eve.set(Calendar.MINUTE, 0);
            eve.set(Calendar.SECOND, 0);
            if (eve.getTimeInMillis() > now.getTimeInMillis()) {
                set(am, ctx, eve.getTimeInMillis(), r[0], 0);
            }
            if (day.getTimeInMillis() > now.getTimeInMillis()) {
                set(am, ctx, day.getTimeInMillis(), r[0], 1);
            }
        }
    }

    private static void set(AlarmManager am, Context ctx, long at, int year, int kind) {
        Intent it = new Intent(ctx, FestivalReceiver.class);
        it.putExtra("year", year);
        it.putExtra("kind", kind);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, year * 10 + kind, it, flags);
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exact) {
                if (Build.VERSION.SDK_INT >= 23) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
                }
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, at, 30 * 60 * 1000L, pi);
            }
        } catch (SecurityException e) {
            am.setWindow(AlarmManager.RTC_WAKEUP, at, 30 * 60 * 1000L, pi);
        }
    }
}
