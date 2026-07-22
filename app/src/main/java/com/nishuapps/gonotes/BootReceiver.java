package com.nishuapps.gonotes;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Map;

/**
 * BugFix-6: Device restart (BOOT_COMPLETED) ke baad saare alarms
 * dobara schedule karta hai.
 *
 * Kaise kaam karta hai:
 *   - setAlarm() ek plain SharedPreferences file "MyNotesAlarms" mein
 *     har active alarm ka time + isDaily store karta hai.
 *   - cancelAlarm() us entry ko wahan se hatata bhi hai.
 *   - Restart hone par Android is receiver ko call karta hai.
 *   - Hum "MyNotesAlarms" padh ke har future alarm ko
 *     AlarmManager se dubara schedule kar dete hain.
 *   - Past ke "once" alarms ignore kiye jaate hain (woh fire ho chuke the).
 *   - Past ke "daily" alarms ko agle valid time par reschedule kiya jaata hai.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
            return;
        }

        SharedPreferences alarmSp =
                context.getSharedPreferences("MyNotesAlarms", Context.MODE_PRIVATE);
        Map<String, ?> all = alarmSp.getAll();
        if (all == null || all.isEmpty()) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long now = System.currentTimeMillis();

        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            // Sirf "alarm_<noteId>" keys process karo
            if (!key.startsWith("alarm_")) continue;

            String noteId = key.substring(6); // "alarm_" = 6 chars
            Object val = entry.getValue();
            if (!(val instanceof Long)) continue;

            long alarmTime = (Long) val;
            boolean isDaily = alarmSp.getBoolean("daily_" + noteId, false);

            if (alarmTime > now) {
                // Future alarm — seedha schedule karo
                scheduleAlarm(context, am, noteId, alarmTime, isDaily);
            } else if (isDaily) {
                // Daily alarm ka time nikal gaya (device off tha)
                // Agle valid occurrence ke liye schedule karo
                long nextTime = alarmTime;
                while (nextTime <= now) {
                    nextTime += AlarmManager.INTERVAL_DAY;
                }
                scheduleAlarm(context, am, noteId, nextTime, true);
                // Registry update karo
                alarmSp.edit().putLong("alarm_" + noteId, nextTime).apply();
            }
            // Past "once" alarm: ignore — woh fire ho chuka tha ya miss hua
        }
    }

    private void scheduleAlarm(Context context, AlarmManager am,
                               String noteId, long time, boolean isDaily) {
        Intent alarmIntent = new Intent(context, MainActivity.ReminderReceiver.class);
        alarmIntent.putExtra("noteId", noteId);
        alarmIntent.putExtra("isDaily", isDaily);

        // setAlarm() se same requestCode — Math.abs zaroori hai
        int requestCode = Math.abs(noteId.hashCode());

        PendingIntent pi = PendingIntent.getBroadcast(
                context, requestCode, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) return; // permission nahi hai
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, time, pi);
        }
    }
}
