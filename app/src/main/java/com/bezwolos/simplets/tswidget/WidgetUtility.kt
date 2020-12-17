package com.bezwolos.simplets.tswidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.bezwolos.simplets.R
import com.bezwolos.simplets.SIMPLE_TS_WIDGET_TAG
import com.bezwolos.simplets.URL_AS_STRING
import java.lang.NumberFormatException
import java.util.concurrent.TimeUnit

internal class WidgetUtility {
    private val TAG = "simplets.WidUtil"

    //  ID notification
    private val minId = 1
    private val maxId = 2

    /*
     check on exit from threshold values if true -> start Notification
     */
    fun actionForThreshold(
        context: Context,
        settings: SimpleTSWidget.WidgetSettings,
        currentValue: String
    ): Boolean {
        // check on of numbers
        val value = try {
            currentValue.toFloat()
        } catch (e: NumberFormatException) {
            return false
        }
        // check max
        val max = settings.max
        if (max != null && max < value) {
            showExceedMax(context, max, value)
            return true
        }
        // check min
        val min = settings.min
        if (min != null && value < min) {
            showBelowMin(context, min, value)
            return true
        }
        return false
    }

    private fun showBelowMin(context: Context, min: Float, value: Float) {
        val mess = context.resources.getString(R.string.min_value_alarm) +
                "| min $min " +
                context.resources.getString(R.string.value_have) +
                " = $value"
        val title = context.resources.getString(R.string.warning)
        showNotify(mess, context, title, minId)
    }

    private fun showExceedMax(context: Context, max: Float, value: Float) {
        val mess = context.resources.getString(R.string.max_value_alarm) +
                "| max = $max " +
                context.resources.getString(R.string.value_have) +
                " = $value"
        val title = context.resources.getString(R.string.warning)
        showNotify(mess, context, title, maxId)
    }


    private fun showNotify(message: String, context: Context, title: String, notifyId: Int) {
        Log.d(TAG, "Notify text = $message")
        val res = message.split("|")
        val builder = NotificationCompat.Builder(context, title)
        with(builder) {
            setSmallIcon(R.drawable.ic_siren)        // setSmallIcon(android.R.drawable.ic_dialog_info)
            setColor(context.resources.getColor(R.color.warning_button_color))
            setContentTitle(title)
            setContentText(res[0])
            setSubText(res[1])
            setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notifyId, builder.build())
    }

    /*
     on change settings reconfigure widget properties
     */
    internal fun startAutoUpdate(context: Context, widgetId: Int) {
        if (widgetId < 1) {
            Log.w(TAG, "Can`t start for execute widgetId=0")
        }
        val settings: SimpleTSWidget.WidgetSettings = loadWidgetSettingsPref(context, widgetId)
        Log.i(TAG, "startAutoupdate() have settings ${settings.toString()}")
        val reqURL = settings.reqUrl
        val workTag = SIMPLE_TS_WIDGET_TAG + widgetId
        //  cancel old work if exist
        WorkManager.getInstance(context).cancelAllWorkByTag(workTag)
        // prepare data for worker
        val data = Data.Builder()
            .putString(URL_AS_STRING, settings.reqUrl)
            .putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .build()
        //  prepare work request
        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            SimpleTSrequestWorker::class.java,
            settings.updateTime,
            TimeUnit.MINUTES,
            settings.updateTime - 5,
            TimeUnit.MINUTES
        )
        periodicWorkRequest.addTag(workTag)
        //  update on start
        val workRequest = OneTimeWorkRequestBuilder<SimpleTSrequestWorker>()
            .setInputData(data)
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
        //  set update by time
        periodicWorkRequest.setInputData(data)
        Log.d(
            TAG,
            "try start work witch param [ widgetId=${data.getString(URL_AS_STRING)} URL=${data.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                0
            )} ]"
        )
        WorkManager.getInstance(context).enqueue(periodicWorkRequest.build())
    }

    internal fun updateImmediately(context: Context, widgetId: Int) {
        Log.d(TAG, "immediately start update")
        if (widgetId < 1) {
            Log.w(TAG, "Can`t start for execute widgetId=0")
        }
        val settings: SimpleTSWidget.WidgetSettings = loadWidgetSettingsPref(context, widgetId)
        // prepare data for worker
        val data = Data.Builder()
            .putString(URL_AS_STRING, settings.reqUrl)
            .putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<SimpleTSrequestWorker>()
            .setInputData(data)
            .setInitialDelay(1, TimeUnit.SECONDS)
            .build()
        Log.d(
            TAG,
            "try start work witch param [ widgetId=${data.getString(URL_AS_STRING)} URL=${data.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                0
            )} ]"
        )
        WorkManager.getInstance(context).enqueue(workRequest)
    }

}

