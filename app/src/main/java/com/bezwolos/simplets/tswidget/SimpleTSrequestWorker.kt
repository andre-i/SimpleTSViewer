package com.bezwolos.simplets.tswidget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bezwolos.simplets.KEY_WIDGET_VALUES
import com.bezwolos.simplets.URL_AS_STRING
import com.bezwolos.simplets.data.DataHandler
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL


class SimpleTSrequestWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams)  {
    private val TAG = "simplets.WidgWork"

    private val job = SupervisorJob()
    private var isBusy = false
    private val workerParams = workerParams
    private val context = appContext

    override fun doWork(): Result {
        Log.v(TAG, " doWork()")
        // if service in work -> do nothing
        if(isBusy) {
            Log.d(TAG, "service Busy - do nothing")
            return Result.success()
        }
        isBusy = true
        val startTime = System.currentTimeMillis()
        val workData = getInputData()
        val requestURL = workData.getString(URL_AS_STRING) ?: ""
        val widgetId = workData.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
        Log.d(TAG, "worker execute: request=$requestURL  widgetId=$widgetId")
        if (requestURL.isEmpty() || widgetId == 0) {
            Log.w(TAG, "WARNING !!!   Exit on Get empty param")
            return Result.failure()
        }
        try {
            GlobalScope.launch(Dispatchers.IO) {
                val value = getNewValue(requestURL)
                Log.v(TAG, "Get from server value=$value")
                val intent = Intent("android.appwidget.action.APPWIDGET_UPDATE")
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                intent.putExtra(KEY_WIDGET_VALUES, value)
                context.sendBroadcast(intent)
                Log.v(TAG, "service execute work and wait for pause(60s)")
            }
        }catch (e: Exception){
            Log.e(TAG, "doWork() catch err=${e.message}", e)
            return Result.failure()
        }
        GlobalScope.launch(Dispatchers.IO) {
            while ((System.currentTimeMillis() - startTime) < 60000) delay(5)
            isBusy = false
            Log.v(TAG, "stop doWork() thread")
        }
        return Result.success()
    }

    companion object {
        private val TAG="simplets.WidWork"
        internal suspend fun getNewValue(_url: String): String {
            // if service in work -> do nothing
            val url = URL(_url)
            val response = StringBuilder("")
            Log.d(TAG, "Start HTTPS request")
            try {
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"  // optional default is GET
                    Log.d(TAG, "\nSent 'GET' request to URL : $url ")
                    if (responseCode != 200) {
                        Log.w(TAG, "WRONG URL \"$_url\" get code $responseCode")
                        return "wrongURL"
                    }
                    inputStream.bufferedReader().use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        Log.d(TAG, "[ ${response.toString()} ]")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error ON GET DATA FROM SERVER ", e)
                Log.w(TAG, "Error is : [${e.toString()}]")
                return "err"
            }
            val res = response.toString()
            val start = res.lastIndexOf(':') + 2
            val end = res.lastIndexOf('"')
            if (start < 0 || end < 0) {
                Log.w(TAG, "error parse of $res index be start=$start, end=$end")
                return "NaN"
            }
            return res.substring(start, end)
        }
    }

}
