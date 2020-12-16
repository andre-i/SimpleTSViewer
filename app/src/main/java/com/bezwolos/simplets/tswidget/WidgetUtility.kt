package com.bezwolos.simplets.tswidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bezwolos.simplets.R
import com.bezwolos.simplets.URL_AS_STRING
import java.lang.NumberFormatException

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
        val settings: SimpleTSWidget.WidgetSettings = loadWidgetSettingsPref(context, widgetId)
        Log.i(TAG, "startAutoupdate() have settings ${settings.toString()}")
        val updateTime = settings.updateTime * 60000L //   updateTime in millis
 /*       //  if exist old intent be cancel
        val oldIntent = Intent(context, SimpleTSService::class.java)
        val oldPendingIntent = PendingIntent.getService(
            context,
            widgetId,
            oldIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
                TODO// uncomment code with NOTE below
        */
        // set to alarm manager new intent
        val newIntent = Intent(context, SimpleTSrequestWorker::class.java)
        newIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        newIntent.putExtra(URL_AS_STRING, settings.reqUrl)
        val curPendingIntent = PendingIntent.getService(
            context,
            0,
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            /*  NOTE : uncomment if uncomment code before in this method
            Log.d(TAG, "delete old pendingIntent")
            cancel(oldPendingIntent)
             */
            Log.d(TAG, "set new pendingIntent")
            setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 4000, updateTime, curPendingIntent)
        }
        curPendingIntent.send()
    }

    internal fun updateImmediately(context: Context, widgetId: Int){
        Log.d(TAG, "immediately start update")
        val  settings: SimpleTSWidget.WidgetSettings = loadWidgetSettingsPref(context, widgetId)
        val intent = Intent(context, SimpleTSrequestWorker::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent.putExtra(URL_AS_STRING, settings.reqUrl)
        context.startService(intent)
    }

}