package com.cona.spechrecognitionforegroundservice

import android.content.Intent
import android.os.IBinder
import android.R.string.cancel
import android.media.AudioManager
import android.support.v4.app.ServiceCompat.stopForeground
import android.Manifest.permission.FOREGROUND_SERVICE
import android.app.*
import android.graphics.Bitmap
import com.cona.spechrecognitionforegroundservice.R.mipmap.ic_launcher
import android.support.v4.app.NotificationCompat
import android.graphics.BitmapFactory
import android.content.Context
import android.speech.SpeechRecognizer
import android.os.Bundle
import android.support.v4.os.HandlerCompat.postDelayed
import android.speech.RecognitionListener
import android.os.Build
import android.speech.RecognizerIntent
import android.content.Context.AUDIO_SERVICE
import android.graphics.Color
import android.os.Handler
import android.provider.ContactsContract.Intents.Insert.ACTION
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat.getSystemService
import android.util.Log


class ForegroundService : Service {

    protected var mAudioManager: AudioManager? = null
    protected var mSpeechRecognizer: SpeechRecognizer? = null
    protected var mSpeechRecognizerIntent: Intent? = null

    protected var mIsListening: Boolean = false
    private var mIsStreamSolo: Boolean = false

    var isInMuteMode = true


    constructor(){}

    constructor(listener: onResultsReady) {
        try {
            mListener = listener
        } catch (e: ClassCastException) {
            Log.e(TAG, e.toString())
        }

    }

    fun addListener(listener: onResultsReady){
        try {
            mListener = listener
        } catch (e: ClassCastException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun listenAgain() {
        if (mIsListening) {
            mIsListening = false
            mSpeechRecognizer!!.cancel()
            startListening()
        }
    }

    private fun startListening() {
        if (!mIsListening) {
            mIsListening = true
            // turn off beep sound
            mAudioManager?.setStreamMute(AudioManager.STREAM_MUSIC, true)
            Handler().postDelayed({ mAudioManager?.setStreamMute(AudioManager.STREAM_MUSIC, false) }, 500)
            mIsStreamSolo = true

            mSpeechRecognizer!!.startListening(mSpeechRecognizerIntent)
        }
    }

    fun destroy() {
        mIsListening = false
        if (!mIsStreamSolo) {
            mAudioManager?.setStreamMute(AudioManager.STREAM_MUSIC, false)
            mIsStreamSolo = true
        }
        Log.d(TAG, "onDestroy")
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer!!.stopListening()
            mSpeechRecognizer!!.cancel()
            mSpeechRecognizer!!.destroy()
            mSpeechRecognizer = null
        }
    }

    protected inner class SpeechRecognitionListener : RecognitionListener {

        override fun onBeginningOfSpeech() {}

        override fun onBufferReceived(buffer: ByteArray) {}

        override fun onEndOfSpeech() {
            mAudioManager?.setStreamMute(AudioManager.STREAM_MUSIC, true)
            Handler().postDelayed({ mAudioManager?.setStreamMute(AudioManager.STREAM_MUSIC, false) }, 500)
        }

        @Synchronized
        override fun onError(error: Int) {
            Log.d(TAG, "ERROR! ERROR!")
            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                if (mListener != null) {
                    val errorList = ArrayList<String>(1)
                    errorList.add("ERROR RECOGNIZER BUSY")
                    if (mListener != null)
                        mListener!!.onResults(errorList)
                }
                return
            }

            if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                if (mListener != null)
                    mListener!!.onResults(null)
            }

            if (error == SpeechRecognizer.ERROR_NETWORK) {
                val errorList = ArrayList<String>(1)
                errorList.add("STOPPED LISTENING")
                if (mListener != null)
                    mListener!!.onResults(errorList)
            }
            Log.d(TAG, "error = $error")
            Handler().postDelayed(Runnable { listenAgain() }, 100)
        }


        override fun onEvent(eventType: Int, params: Bundle) {}

        override fun onPartialResults(partialResults: Bundle) {

        }

        override fun onReadyForSpeech(params: Bundle) {}

        override fun onResults(results: Bundle?) {
            mAudioManager?.setStreamMute(AudioManager.STREAM_MUSIC, true)
            Handler().postDelayed({ mAudioManager?.setStreamMute(AudioManager.STREAM_MUSIC, false) }, 500)
            if (results != null) {
                mListener?.onResults(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
                val result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (result != null && result.size > 0)
                    showoNot(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)[0])
            }
            listenAgain()
        }

        override fun onRmsChanged(rmsdB: Float) {}
    }

    fun ismIsListening(): Boolean {
        return mIsListening
    }


    interface onResultsReady {
        fun onResults(results: ArrayList<String>?)
    }

    fun mute(mute: Boolean) {
        isInMuteMode = mute
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == Constants.STARTFOREGROUND_ACTION) {
            Log.i(TAG, "Received Start Foreground Intent ")
           showoNot("none")

            val context = baseContext
            mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.baseContext)
            mSpeechRecognizer!!.setRecognitionListener(SpeechRecognitionListener())
            mSpeechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            mSpeechRecognizerIntent?.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            mSpeechRecognizerIntent?.putExtra(
                RecognizerIntent.EXTRA_CALLING_PACKAGE,
                context.packageName
            )
            startListening()

        } else if (intent.action == Constants.ACTION_PREV) {
            Log.i(TAG, "Clicked Previous")
        } else if (intent.action == Constants.ACTION_STOPFOREGROUND) {
            Log.d(TAG, "In onDestroy")
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }

    private fun showoNot(value:String){
        val notificationIntent = Intent(applicationContext, ForegroundService::class.java)
        notificationIntent.action = Constants.ACTION_MAIN
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val previousIntent = Intent(this, ForegroundService::class.java)
        previousIntent.setAction(Constants.ACTION_PREV)
        val ppreviousIntent = PendingIntent.getService(
            this, 0,
            previousIntent, 0
        )

        val icon = BitmapFactory.decodeResource(
            getResources(),
            R.drawable.ic_hearing
        )

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)

        val notification = notificationBuilder
            .setContentTitle("Listening")
            .setTicker(value)
            .setContentText(value)
            .setSmallIcon(R.drawable.ic_hearing)
            .setLargeIcon(
                icon
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous", ppreviousIntent
            )
            .build()
        startForeground(
            Constants.NOTIFICATION_ID,
            notification
        )
    }

    override fun onDestroy() {
        Log.d(TAG, "In onDestroy")
        super.onDestroy()

        mIsListening = false
        if (!mIsStreamSolo) {
            mAudioManager?.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
            mAudioManager?.setStreamMute(AudioManager.STREAM_ALARM, false)
            mAudioManager?.setStreamMute(AudioManager.STREAM_MUSIC, false)
            mAudioManager?.setStreamMute(AudioManager.STREAM_RING, false)
            mAudioManager?.setStreamMute(AudioManager.STREAM_SYSTEM, false)
            mIsStreamSolo = true
        }
        Log.d(TAG, "onDestroy")
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer!!.stopListening()
            mSpeechRecognizer!!.cancel()
            mSpeechRecognizer!!.destroy()
            mSpeechRecognizer = null
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "In onBind")
        return null
    }

    companion object {
        private val TAG = "ForegroundService"
        private var mListener: onResultsReady? = null
    }
}