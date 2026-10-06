package com.petmed.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: return
        val recordId = intent.getIntExtra(EXTRA_RECORD_ID, 0)
        val isRepeat = intent.getBooleanExtra(EXTRA_IS_REPEAT, false)

        NotificationHelper.showNotification(context, recordId, medicationName)

        if (isRepeat) {
            val day = intent.getIntExtra(EXTRA_REPEAT_DAY, 0)
            val repeatTime = intent.getStringExtra(EXTRA_REPEAT_TIME) ?: return
            AlarmScheduler.rescheduleRepeatAlarm(context, recordId, medicationName, day, repeatTime)
        }
    }

    companion object {
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_RECORD_ID = "record_id"
        const val EXTRA_IS_REPEAT = "is_repeat"
        const val EXTRA_REPEAT_DAY = "repeat_day"
        const val EXTRA_REPEAT_TIME = "repeat_time"
    }
}
