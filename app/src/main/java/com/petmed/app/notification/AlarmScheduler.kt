package com.petmed.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.petmed.app.data.model.MedicationRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun schedule(context: Context, record: MedicationRecord) {
        val triggerTime = runCatching {
            dateFormat.parse(record.dateTime)?.time
        }.getOrNull() ?: return
        if (triggerTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildOneTimePendingIntent(context, record)
        setExact(alarmManager, triggerTime, pendingIntent)
    }

    fun scheduleRepeat(context: Context, record: MedicationRecord) {
        if (record.repeatDays.isBlank() || record.repeatTime.isBlank()) return
        val days = record.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        val (hour, minute) = parseTime(record.repeatTime) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (day in days) {
            val triggerTime = nextOccurrence(day, hour, minute)
            val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                putExtra(MedicationAlarmReceiver.EXTRA_RECORD_ID, record.id)
                putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_NAME, record.medicationName)
                putExtra(MedicationAlarmReceiver.EXTRA_IS_REPEAT, true)
                putExtra(MedicationAlarmReceiver.EXTRA_REPEAT_DAY, day)
                putExtra(MedicationAlarmReceiver.EXTRA_REPEAT_TIME, record.repeatTime)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, repeatRequestCode(record.id, day), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExact(alarmManager, triggerTime, pendingIntent)
        }
    }

    fun rescheduleRepeatAlarm(
        context: Context,
        recordId: Int,
        medicationName: String,
        day: Int,
        repeatTime: String
    ) {
        val nextTrigger = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(MedicationAlarmReceiver.EXTRA_RECORD_ID, recordId)
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(MedicationAlarmReceiver.EXTRA_IS_REPEAT, true)
            putExtra(MedicationAlarmReceiver.EXTRA_REPEAT_DAY, day)
            putExtra(MedicationAlarmReceiver.EXTRA_REPEAT_TIME, repeatTime)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, repeatRequestCode(recordId, day), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExact(alarmManager, nextTrigger, pendingIntent)
    }

    fun cancel(context: Context, record: MedicationRecord) {
        if (record.isRepeat) cancelRepeat(context, record)
        else cancelById(context, record.id)
    }

    fun cancelRepeat(context: Context, record: MedicationRecord) {
        val days = record.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (day in days) {
            val pi = PendingIntent.getBroadcast(
                context, repeatRequestCode(record.id, day),
                Intent(context, MedicationAlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pi)
        }
    }

    private fun cancelById(context: Context, recordId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, recordId,
            Intent(context, MedicationAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    private fun buildOneTimePendingIntent(context: Context, record: MedicationRecord): PendingIntent {
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(MedicationAlarmReceiver.EXTRA_RECORD_ID, record.id)
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_NAME, record.medicationName)
        }
        return PendingIntent.getBroadcast(
            context, record.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setExact(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun repeatRequestCode(recordId: Int, day: Int) = 1_000_000 + recordId * 7 + (day - 1)

    private fun parseTime(time: String): Pair<Int, Int>? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        return Pair(parts[0].toIntOrNull() ?: return null, parts[1].toIntOrNull() ?: return null)
    }

    private fun nextOccurrence(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = cal.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = (dayOfWeek - current + 7) % 7
        if (daysToAdd == 0 && cal.timeInMillis <= System.currentTimeMillis()) daysToAdd = 7
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return cal.timeInMillis
    }
}
