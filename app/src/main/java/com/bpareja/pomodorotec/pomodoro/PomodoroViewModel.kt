package com.bpareja.pomodorotec.pomodoro

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bpareja.pomodorotec.MainActivity
import com.bpareja.pomodorotec.R

enum class Phase {
    FOCUS, BREAK
}

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    private val _timeLeft = MutableLiveData("25:00")
    val timeLeft: LiveData<String> = _timeLeft

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _currentPhase = MutableLiveData(Phase.FOCUS)
    val currentPhase: LiveData<Phase> = _currentPhase

    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingInMillis: Long = 25 * 60 * 1000L // Tiempo inicial para FOCUS

    companion object {
        var instance: PomodoroViewModel? = null
    }
    init {
        instance = this
    }


    // Función para iniciar la sesión de concentración
    fun startFocusSession() {
        _currentPhase.value = Phase.FOCUS
        timeRemainingInMillis = 25 * 60 * 1000L // Ajusta a 2 minutos para pruebas
        _timeLeft.value = "02:00"
        showNotification("Inicio de Concentración", "La sesión de concentración ha comenzado.")
        startTimer()
    }

    // Función para iniciar la sesión de descanso
    private fun startBreakSession() {
        _currentPhase.value = Phase.BREAK
        timeRemainingInMillis = 5 * 60 * 1000L // 5 minutos para descanso
        _timeLeft.value = "05:00"
        showNotification("Inicio de Descanso", "La sesión de descanso ha comenzado.")
        startTimer()
    }

    // Inicia o reanuda el temporizador
    fun startTimer() {
        if (_isRunning.value == true) return

        _isRunning.value = true
        countDownTimer = object : CountDownTimer(timeRemainingInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingInMillis = millisUntilFinished
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                _timeLeft.value = String.format("%02d:%02d", minutes, seconds)

                showNotification(
                    "Pomodoro (${_currentPhase.value})",
                    "Tiempo restante: ${_timeLeft.value}"
                )
            }


            override fun onFinish() {
                _isRunning.value = false
                when (_currentPhase.value) {
                    Phase.FOCUS -> startBreakSession()
                    Phase.BREAK -> startFocusSession()
                    else -> {}
                }
            }
        }.start()
    }

    // Pausa el temporizador
    fun pauseTimer() {
        countDownTimer?.cancel()
        _isRunning.value = false
    }

    // Restablece el temporizador
    fun resetTimer() {
        countDownTimer?.cancel()
        _isRunning.value = false
        _currentPhase.value = Phase.FOCUS
        timeRemainingInMillis = 25 * 60 * 1000L
        _timeLeft.value = "25:00"

        NotificationManagerCompat.from(context).cancel(MainActivity.NOTIFICATION_ID)
    }


    // Muestra la notificación
    private fun showNotification(title: String, message: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val pauseIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_PAUSE"
            }
            val pausePendingIntent = PendingIntent.getBroadcast(
                context, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val resumeIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_RESUME"
            }
            val resumePendingIntent = PendingIntent.getBroadcast(
                context, 0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_STOP"
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.pomodoro)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .addAction(R.drawable.ic_pause, "Pausa", pausePendingIntent)
                .addAction(R.drawable.ic_play, "Continuar", resumePendingIntent)
                .addAction(R.drawable.ic_stop, "Detener", stopPendingIntent)
                .setContentIntent(PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                ))
                .build()

            NotificationManagerCompat.from(context).notify(MainActivity.NOTIFICATION_ID, notification)
        } else {
            Toast.makeText(context, "No se puede mostrar la notificación sin permiso", Toast.LENGTH_SHORT).show()
        }
    }


}