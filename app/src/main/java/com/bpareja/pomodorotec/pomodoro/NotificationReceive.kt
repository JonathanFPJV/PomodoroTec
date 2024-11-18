package com.bpareja.pomodorotec.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_PAUSE" -> PomodoroViewModel.instance?.pauseTimer()
            "ACTION_RESUME" -> PomodoroViewModel.instance?.startTimer()
            "ACTION_STOP" -> PomodoroViewModel.instance?.resetTimer()
        }
    }
}
