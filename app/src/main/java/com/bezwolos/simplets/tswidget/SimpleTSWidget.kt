package com.bezwolos.simplets.tswidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.widget.RemoteViews
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.bezwolos.simplets.*
import com.bezwolos.simplets.KEY_WIDGET_VALUES
import com.bezwolos.simplets.MainActivity
import com.bezwolos.simplets.SIMPLE_TS_WIDGET_UPDATE
import com.bezwolos.simplets.URL_AS_STRING
import com.bezwolos.simplets.chart.CHANNEL_ID
import java.lang.StringBuilder

//   internal constants


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [SimpleTSWidgetConfigureActivity]
 */
class SimpleTSWidget : AppWidgetProvider() {
    private val TAG = "simplets.Widget"


    private var currentValue = "--"


    private val UPDATE_SETTINGS = "UPDATE_SETTINGS "
    private val UPDATE = "UPDATE"
    private val NO_ACTION = "NO_ACTION"
    private val SET_VALUE = "SET_VALUE"
    private val UPDATE_ON_CLICK = "UPDATE_ON_CLICK"
    private val DELLETE_WIDG = "DELETE_WIDGET"
    private val DISABLED_ALL = "DISABLED_ALL"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            Log.d(TAG, "onUpdate() widgetId=$appWidgetId")
            /*updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId
            )*/
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        Log.d(TAG, "onDeleted()")
        if (appWidgetIds.isEmpty()) return
        for (widgetId in appWidgetIds) {
            deleteWidgetSettings(context, widgetId)
            //  kill work for deleted widget
            Log.d(TAG, "Start Delete work for widget $widgetId")
            with(WorkManager.getInstance(context)) {
                cancelAllWorkByTag(SIMPLE_TS_WIDGET_TAG + widgetId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Delete channel for notifications on delete widget")
                with(NotificationManagerCompat.from(context)) {
                    deleteNotificationChannel(widgetId.toString())
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        Log.d(TAG, "Create first widget")
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d(TAG, "onDisabled()")
        if (deleteAllWidgetSettings(context)) Log.d(
            TAG,
            "On disabled delete all settings"
        ) else Log.w(TAG, "Err - can`t delete settings on disabled widgets")
        Log.d(TAG, " cancel all Work for widget")
        WorkManager.getInstance(context).cancelAllWork()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        logOnReseive(context, intent)
        if (context == null || intent == null) {
            Log.i(TAG, " exit on some == null")
            return
        }
        //first - action_name, second - widgetId
        val wAction = getIntentTarget(intent)
        if(wAction.second < 2)return
        when (wAction.first) {
            UPDATE -> {
                WidgetUtility().updateImmediately(context, wAction.second)
            }
            UPDATE_ON_CLICK -> WidgetUtility().updateImmediately(context, wAction.second)
            UPDATE_SETTINGS -> {

               // WidgetUtility().updateImmediately(context, wAction.second)
               // WidgetUtility().startAutoUpdate(context, wAction.second)
            }
            SET_VALUE -> {
                val manager = AppWidgetManager.getInstance(context)
                currentValue = intent.extras?.getString(KEY_WIDGET_VALUES) ?: "--"
                Log.v(TAG, "Widget onReceive() get  SET_VALUE new value = $currentValue")
                construct(context, manager, wAction.second)
            }
            DELLETE_WIDG -> onDeleted(context, IntArray(1) { wAction.second })
                DISABLED_ALL -> onDisabled(context)
            else -> return
        }
    }


    // ---------------------------------  own fun  --------------------------------------


    private fun changeSettings(context: Context,widgetId: Int) {
    }


    private fun logOnReseive(context: Context?, intent: Intent?) {
        Log.d(
            TAG,
            "onReceive() ${if (context == null) "context = null" else if (intent == null) " intent == null" else " get Intent ${intent.toString()} "}"
        )
/*        Log.v(TAG, "context ${context.toString()}")
        Log.v(TAG, "action  ${intent?.action}")
        val extra = intent?.extras
        if (extra == null) Log.v(TAG, "Intent has no extras")
        val widgetId = extra?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val hasSettings = extra?.getBoolean(SIMPLE_TS_WIDGET_UPDATE)
        val isUpdate = extra?.getBoolean(UPDATE_ON_CLICK, false)
        val getValue = extra?.getString(KEY_WIDGET_VALUES) ?: "--"
        Log.v(
            TAG,
            "widgetId $widgetId , hasSettings $hasSettings , isUpdate $isUpdate , getValue $getValue"
        )*/

    }


    /*
        parse intent action and extras and return type action for widget
        return :
           a. if widgetId = 0 or not extras - NO_ACTION
           b. if widget be resized - UPDATE_UI
           c. if get message from settings activity - UPDATE_SETTINGS
           on some wrong return NO_ACTION
     */
    private fun getIntentTarget(intent: Intent): Pair<String, Int> {
        var widgetId = 0
        val extra = intent.extras // ?: return Pair(NO_ACTION,0)
        if (extra != null) {
            widgetId = extra.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
   /*         Log.v(TAG, "EXTRA size ${extra.size()}  toString() [ ${extra.toString()}")
            for( item in extra.keySet()) Log.v("simplets.EXTRA", "[  key: $item  value ${extra.get(item)}  ]")*/
            val hasSettings = extra.getBoolean(SIMPLE_TS_WIDGET_UPDATE)
            if (hasSettings) return Pair(UPDATE_SETTINGS, widgetId)
            // click on value in widget -> send intent to self with UPDATE_ON_CLICK parameter
            val isUpdate = extra.getBoolean(UPDATE_ON_CLICK, false)
            if (isUpdate) return Pair(UPDATE_ON_CLICK, widgetId)
        }
        Log.v(TAG, "widgetId=$widgetId action=${intent.action}")
        val retVal = when (intent.action) {
            // resize
            "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS" -> {
                val action = if(widgetId < 2)NO_ACTION  else UPDATE
                Pair(action, widgetId)
            }
            // click on textView
            "android.appwidget.action.APPWIDGET_UPDATE" -> {
                val action = if(widgetId < 2)NO_ACTION else SET_VALUE
                Pair(action, widgetId)
            }
            // on Delete widget
            "android.appwidget.action.APPWIDGET_DELETED" -> Pair(DELLETE_WIDG, widgetId)
            // delete all widgets
            "android.appwidget.action.APPWIDGET_DISABLED" -> Pair(DISABLED_ALL, widgetId)
            //  some other - not handle
            else -> {
                Log.w(TAG, "!!! SURPRISE - [ get intent with unable action ]")
                Pair(NO_ACTION, 0)
            }
        }
        return retVal
    }

    /*
        add click listener for settings, main buttons and for click on value TextView
     */
    private fun setOnClickForWidget(
        context: Context,
        widgetId: Int,
        views: RemoteViews,
        channelId: Long
    ) {
        Log.v(TAG, "setOnClickForWidget()")
        //  set on click action for config buttom
        val intent1 = Intent(
            context,
            SimpleTSWidgetConfigureActivity::class.java  // WidgetConfigureActivity::class.java
        )
        //  on click for settings button
        intent1.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent1.putExtra(SIMPLE_TS_WIDGET_UPDATE, true)
        val pIntent1 =
            PendingIntent.getActivity(context, widgetId, intent1, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.widget_button_config, pIntent1)
        // set on click for text of value( it start run service directly )
        // изврат, однако - отправить себе intent на себя-же
        val intent2 = Intent(context, SimpleTSWidget::class.java)
        intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent2.putExtra(UPDATE_ON_CLICK, true)
        intent2.setAction("android.appwidget.action.APPWIDGET_UPDATE")
        val pIntent2 = PendingIntent.getBroadcast(
            context,
            widgetId,
            intent2,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        views.setOnClickPendingIntent(R.id.appwidget_value_text, pIntent2)
        //  on click main button
        val intent3 = Intent(context, MainActivity::class.java)
        intent3.putExtra(CALL_FROM_WIDGET, true)
       //          Log.d(TAG, "put to main activity channelId=$channelId" )
        intent3.putExtra( CHANNEL_ID, channelId )
        val pIntent3 = PendingIntent.getActivity(
            context,
            widgetId,
            intent3,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_button_main, pIntent3)
    }

    /*
        redraw widget and set listeners for it components
     */
    private fun construct(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val settings = loadWidgetSettingsPref(context, widgetId)
        var isError =
            if (settings.max == null && settings.min == null || currentValue == "--") false
            else WidgetUtility().actionForThreshold(context, settings, currentValue)
        Log.v(TAG, "call construct() settings ${settings.asString()}")
        val col1 = Color.parseColor(settings.backColor)
        val col2 = Color.parseColor("#40888888")
        val fontCol = if (isError) Color.RED else if (col1 > col2) Color.BLACK else Color.WHITE
        val labelCol =
            if (fontCol == Color.BLACK) Color.parseColor("#001040") else Color.parseColor("#fffaff")
        // Construct the RemoteViews object
        RemoteViews(context.packageName, R.layout.simple_t_s_widget).also { views ->
            views.setInt(
                R.id.widget_root_container,
                "setBackgroundColor",
                col1
            )
            views.setTextViewTextSize(
                R.id.appwidget_value_text,
                COMPLEX_UNIT_SP,
                settings.fontSize
            )
            views.setTextColor(R.id.appwidget_value_text, fontCol)
            views.setTextViewText(R.id.appwidget_value_text, "$currentValue${settings.measureUnit}")
            views.setTextViewTextSize(
                R.id.appwidget_name_text,
                COMPLEX_UNIT_SP,
                if (settings.fontSize > 28) (settings.fontSize / 2) - 1 else 13f
            )
            views.setTextColor(R.id.appwidget_name_text, labelCol)
            views.setTextViewText(R.id.appwidget_name_text, settings.fieldName)
            setOnClickForWidget(context, widgetId, views, computeChannelIdFromURL(settings.reqUrl))
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    /*
        get from url channelId substring and convert it in Long
     */
    private fun computeChannelIdFromURL(url: String): Long {
        val res = if (url.length < 2) {
            0L
        } else {
            //  request example :  https://api.thingspeak.com/channels/495353/fields/2.json
            val start = url.indexOf("channels") + 9
            val end = url.indexOf("fields") - 1
            url.substring(start, end).toLong()
        }
        Log.v(TAG, "computeChannelFromURL() channelId = $res")
        return res
    }


    // ++++++++++++++++++++++++++++++++ classes  ++++++++++++++++++++++++++++++++++++++
    /* contain data for show widget  */
    data class WidgetSettings(
        internal var reqUrl: String = "",
        internal var fieldName: String = "",
        internal var measureUnit: String = "",
        internal var max: Float? = null,
        internal var min: Float? = null,
        internal var updateTime: Long = 0L,
        internal var fontSize: Float = 0f,
        internal var backColor: String = "#302060"
    ) {
        internal val SEPARATOR_AS_TEXT = "#amp;#"
        internal fun asString(): String {
            val fName = if (fieldName.contains("&")) fieldName.replace("&", SEPARATOR_AS_TEXT)
            else fieldName
            val res = StringBuilder()
            res.append("$reqUrl&$fieldName&$measureUnit&$max&$min&$updateTime&$fontSize&$backColor")
            return res.toString()
        }
    }

    /*
    widget background colors
     */
    enum class Colors(private val argb: Int) {
        BROWN(R.color.brown), WHITE(R.color.white), GREEN(R.color.green), BLUE(R.color.blue),
        YELLOW(R.color.yellow);

        fun getColorValue(context: Context): String = context.getString(argb)
    }


}
