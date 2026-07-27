package com.nishuapps.gonotes;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

public class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_DISMISS = "com.nishuapps.gonotes.ACTION_DISMISS";
    public static final String ACTION_SNOOZE = "com.nishuapps.gonotes.ACTION_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String noteId = intent.getStringExtra("noteId");
        int notifId = (noteId != null) ? Math.abs(noteId.hashCode()) : 0;

        // 1. Cancel the notification
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(notifId);
        }

        if (ACTION_SNOOZE.equals(action) && noteId != null) {
            // 2. Reschedule for Tomorrow (Next Day, same time)
            long rescheduleTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // exactly 24 hours later
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                Intent alarmIntent = new Intent(context, MainActivity.ReminderReceiver.class);
                alarmIntent.putExtra("noteId", noteId);
                alarmIntent.putExtra("isDaily", false); // Keep it as one-time postponement

                int requestCode = Math.abs(noteId.hashCode());
                PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, rescheduleTime, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, rescheduleTime, pi);
                }

                // Update registry so UI/BootReceiver knows about the new time
                context.getSharedPreferences("MyNotesAlarms", Context.MODE_PRIVATE).edit()
                        .putLong("alarm_" + noteId, rescheduleTime)
                        .apply();

                Toast.makeText(context, "Rescheduled for tomorrow", Toast.LENGTH_SHORT).show();
            }
        } else if (ACTION_DISMISS.equals(action) && noteId != null) {
            // Mark as completed - basically just clearing the alarm registry
            context.getSharedPreferences("MyNotesAlarms", Context.MODE_PRIVATE).edit()
                    .remove("alarm_" + noteId)
                    .remove("daily_" + noteId)
                    .remove("title_" + noteId)
                    .apply();
            
            // Tell MainActivity to clear it from its in-memory list too (on next resume)
            java.util.Set<String> fired = new java.util.HashSet<>(
                    context.getSharedPreferences("MyNotesData", Context.MODE_PRIVATE)
                            .getStringSet("fired_once_alarms", new java.util.HashSet<>()));
            fired.add(noteId);
            context.getSharedPreferences("MyNotesData", Context.MODE_PRIVATE)
                    .edit().putStringSet("fired_once_alarms", fired).apply();
            
            Toast.makeText(context, "Marked as completed", Toast.LENGTH_SHORT).show();
        }
    }
}
