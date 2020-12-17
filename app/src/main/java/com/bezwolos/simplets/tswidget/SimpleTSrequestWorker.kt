package com.bezwolos.simplets.tswidget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
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
        Log.d(TAG, " doWork()")
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
        Log.i(TAG, "worker execute: request=$requestURL  widgetId=$widgetId")
        if (requestURL.isEmpty() || widgetId == 0) {
            Log.w(TAG, "WARNING !!!   Exit on Get empty param")
            return Result.failure()
        }
        try {
            val value = getNewValue(requestURL)
            Log.d(TAG, "Get from server value=$value")
            val intent = Intent("android.appwidget.action.APPWIDGET_UPDATE")
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            intent.putExtra(KEY_WIDGET_VALUES, value)
            context.sendBroadcast(intent)
            Log.d(TAG, "service execute work and wait for pause(60s)")
            Log.d(TAG, "call Service stopSelf() Ready for next work")
        }catch (e: Exception){
            Log.e(TAG, "doWork() catch err=${e.message}", e)
            return Result.failure()
        }
        GlobalScope.launch {
            while ((System.currentTimeMillis() - startTime) < 60000) delay(5)
            isBusy = false
        }
        return Result.success()
    }

    private fun getNewValue(_url: String): String {
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
