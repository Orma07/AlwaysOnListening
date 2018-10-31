package com.cona.spechrecognitionforegroundservice

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.content.Intent

import android.support.v4.app.ActivityCompat
import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.widget.Button
import java.util.jar.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import java.security.Permission
import java.security.Permissions


class MainActivity : AppCompatActivity(), View.OnClickListener, ForegroundService.onResultsReady {

    override fun onResults(results: ArrayList<String>?) {
        if (results != null && results.size > 0) {
            Log.d(TAG, "Speech-to-text: " + results[0])
            /*SHOW RESULT*/
            lastSaid = results[0]
            if(lastSaid != null){
                runOnUiThread {
                    display?.text = results[0]
                }

            }
        }
    }

    var display : TextView? = null
    private val TAG = "ForegroundMain"
    private var fservice: ForegroundService? = null

    companion object {
        var lastSaid : String? = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        display = findViewById(R.id.display)

        fservice = ForegroundService()
        fservice?.addListener(this)

        val PERMISSION_ALL = 1
        val PERMISSIONS = arrayOf<String>( android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
        }

        val startButton = findViewById(R.id.buttonStart) as Button
        val stopButton = findViewById(R.id.buttonStop) as Button

        startButton.setOnClickListener(this)
        stopButton.setOnClickListener(this)
    }

    fun hasPermissions(context: Context?,  permissions: Array<String>): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context!!, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()

        if(lastSaid != null){
            display?.text = lastSaid
        }
    }


    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.buttonStart -> {
                /*fservice = ForegroundService(object : ForegroundService.onResultsReady {
                    override fun onResults(results: ArrayList<String>?) {
                        if (results != null && results.size > 0) {
                            Log.d(TAG, "Speech-to-text: " + results[0])
                            /*SHOW RESULT*/
                            lastSaid = results[0]
                            if(lastSaid != null){
                                display?.text = lastSaid
                            }

                        }
                    }
                })*/
                val startIntent = Intent(this@MainActivity, ForegroundService::class.java)
                startIntent.action = Constants.STARTFOREGROUND_ACTION
                startService(startIntent)
            }
            R.id.buttonStop -> {
                val stopIntent = Intent(this@MainActivity, ForegroundService::class.java)
                stopIntent.action = Constants.STOPFOREGROUND_ACTION
                startService(stopIntent)
            }
            else -> {
            }
        }
    }
}
