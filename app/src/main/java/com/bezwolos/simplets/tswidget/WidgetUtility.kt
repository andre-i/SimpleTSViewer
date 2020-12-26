package com.bezwolos.simplets.tswidget

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.*
import com.bezwolos.simplets.KEY_WIDGET_VALUES
import com.bezwolos.simplets.R
import com.bezwolos.simplets.SIMPLE_TS_WIDGET_TAG
import com.bezwolos.simplets.URL_AS_STRING
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.NumberFormatException
import java.util.concurrent.TimeUnit

internal class WidgetUtility {
    private val TAG = "simplets.WidUtil"

    //  ID notification
    private val minId = "on bellow min"
    private val maxId = "on great max"
    private val maxNotifId = 2120
    private val minNotifId = 2021

    /*
     check on exit from threshold values if true -> start Notification
     */
    fun actionForThreshold(
        context: Context,
        settings: SimpleTSWidget.WidgetSettings,
        currentValue: String
    ): Boolean {
        Log.d(TAG, "check on threshold")
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
                "\n min $min " +
                context.resources.getString(R.string.value_have) +
                " = $value"
        val title = context.resources.getString(R.string.warning)
        showNotify(mess, context, title, minId, minNotifId)
    }

    private fun showExceedMax(context: Context, max: Float, value: Float) {
        val mess = context.resources.getString(R.string.max_value_alarm) +
                "\n max = $max " +
                context.resources.getString(R.string.value_have) +
                " = $value"
        val title = context.resources.getString(R.string.warning)
        showNotify(mess, context, title, maxId, maxNotifId)
    }


    private fun showNotify(
        message: String,
        context: Context,
        title: String,
        notifyChannelId: String,
        notifyId: Int
    ) {
        Log.i(TAG, "Notify text = $message")
        try {
            val builder = NotificationCompat.Builder(context, notifyChannelId)
                .setSmallIcon(R.drawable.ic_siren)
                .setColor(context.resources.getColor(R.color.warning_button_color))
                .setContentTitle(title)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            val notificationManager = NotificationManagerCompat.from(context)
            Log.i(TAG, "Try start NOTIFY")
            notificationManager.notify(notifyId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "ERROR On show notify: ${e.message}", e)
        }
    }

    /*
        delete or create(if not exists) notification channel
        may be delete if settings min and max is null create otherwise
     */
    private fun changeNotificationChannel(context: Context, widgetId: Int, isRemove: Boolean) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (isRemove) {
                Log.d(TAG, "remove notify channel on null threshold values")
                notificationManager.deleteNotificationChannel(widgetId.toString())
                return
            }
            val name = "notify_Channel1 $widgetId"
            val descriptionText = "description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(widgetId.toString(), name, importance).apply {
                description = descriptionText
            }
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // Register the channel with the system
            Log.d(TAG, "Create  NOTIFY channel")
            notificationManager.createNotificationChannel(channel)
        }
    }


    /*
     on change settings reconfigure widget properties
     */
    internal fun startAutoUpdate(context: Context, widgetId: Int) {
        if (widgetId < 1) {
            Log.w(TAG, "Can`t start for execute widgetId=0")
        }
        val settings: SimpleTSWidget.WidgetSettings = loadWidgetSettingsPref(context, widgetId)
        val isRemoveNotifyChannel = (settings.max != null || settings.min != null)
        changeNotificationChannel(context, widgetId, isRemoveNotifyChannel)
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
        periodicWorkRequest.setInputData(data)
        Log.d(
            TAG,
            "try start AutoUpdate work witch param [ URL=${data.getString(URL_AS_STRING)} widgetId=${data.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                0
            )} ]"
        )
        //  set update by time
        WorkManager.getInstance(context).enqueue(periodicWorkRequest.build())
    }

    private var isBusy = false
    internal fun updateImmediately(context: Context, widgetId: Int) {
        Log.v(TAG, "immediately start update")
        if (widgetId < 1) {
            Log.w(TAG, "Can`t start for execute widgetId=0")
            return
        }
        val settings: SimpleTSWidget.WidgetSettings = loadWidgetSettingsPref(context, widgetId)
        GlobalScope.launch(Dispatchers.IO) {
            if (isBusy) {
                Log.d(TAG, "service Busy - do nothing")
                return@launch
            }
            val startTime = System.currentTimeMillis()
            isBusy = true
            val value = SimpleTSrequestWorker.getNewValue(settings.reqUrl)
            Log.d(TAG, "Get from server value=$value")
            val intent = Intent("android.appwidget.action.APPWIDGET_UPDATE")
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            intent.putExtra(KEY_WIDGET_VALUES, value)
            context.sendBroadcast(intent)
            while ((System.currentTimeMillis() - startTime) < 60000) delay(50)
            isBusy = false
            Log.v(TAG, "ready to start")

        }
    }

}

