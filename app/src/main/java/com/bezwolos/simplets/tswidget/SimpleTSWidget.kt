package com.bezwolos.simplets.tswidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.widget.RemoteViews
import androidx.work.WorkManager
import com.bezwolos.simplets.*
import com.bezwolos.simplets.KEY_WIDGET_VALUES
import com.bezwolos.simplets.MainActivity
import com.bezwolos.simplets.SIMPLE_TS_WIDGET_UPDATE
import com.bezwolos.simplets.URL_AS_STRING
import java.lang.StringBuilder

//   internal constants


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [SimpleTSWidgetConfigureActivity]
 */
class SimpleTSWidget : AppWidgetProvider() {
    private val TAG = "simplets.Widget"


    private var currentValue = "-?-"


    private val UPDATE_SETTINGS = "update_on_change_settings"
    private val UPDATE = "update_value"
    private val NO_ACTION = "update_nothing_action"
    private val SET_VALUE = "set_gotten_value"
    private val UPDATE_VALUE = "update_value_action"

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
        Log.d(TAG, "call \"onDeleted\"")
        if (appWidgetIds.isEmpty()) return
        for (widgetId in appWidgetIds) {
            deleteWidgetSettings(context, widgetId)
            //  kill work for deleted widget
            Log.v(TAG, "Delete work for widget $widgetId")
            WorkManager.getInstance(context).cancelAllWorkByTag(SIMPLE_TS_WIDGET_TAG + widgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        Log.d(TAG, "Create first widget")

    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d(TAG, "onDisabled()")
        if (deleteAllWidgetSettings(context)) Log.v(
            TAG,
            "On disabled delete all settings"
        ) else Log.w(TAG, "Err - can`t delete settings on disabled widgets")
        Log.d(TAG, " cancel all Work for widget")
        WorkManager.getInstance(context).cancelAllWork()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        Log.i(
            TAG,
            "onReceive() ${if (context == null) "context = null" else ""} ${if (intent == null) " intent == null" else ""} "
        )
        if (context == null || intent == null) {
            Log.d(TAG, " exit on some == null")
            return
        }
        Log.d(TAG, "call onReceive() get Intent ${intent.toString()}")
        val wAction = getIntentTarget(intent)       //first - action_name, second - widgetId
        Log.d(TAG, "get intent with widgetId=${wAction.second} action - ${wAction.first} ")
        val manager = AppWidgetManager.getInstance(context)
        when (wAction.first) {
            UPDATE -> WidgetUtility().updateImmediately(context, wAction.second)
            UPDATE_SETTINGS -> WidgetUtility().startAutoUpdate(context, wAction.second)
            SET_VALUE -> {
                currentValue = intent.extras?.getString(KEY_WIDGET_VALUES) ?: "-?-"
                Log.i(TAG, "Widget onReceive() get  SET_VALUE new value = $currentValue")
                construct(context, manager, wAction.second)
            }
            else -> return
        }
    }


    // ---------------------------------  own fun  --------------------------------------

    /*
        parse intent action and extras and return type action for widget
        return :
           a. if widgetId = 0 or not extras - NO_ACTION
           b. if widget be resized - UPDATE_UI
           c. if get message from settings activity - UPDATE_SETTINGS
           on some wrong return NO_ACTION
     */
    private fun getIntentTarget(intent: Intent): Pair<String, Int> {
        var widgetId = -1
        val extra = intent.extras ?: return Pair(NO_ACTION, 0)
        widgetId = extra.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return Pair(NO_ACTION, 0)
        // from settings activity return extra with key  SIMPLE_TS_WIDGET_UPDATE
        val isSetValue = extra.getBoolean(SIMPLE_TS_WIDGET_UPDATE, false)
        if (isSetValue) return Pair(UPDATE_SETTINGS, widgetId)
        // click on value in widget -> send intent to self with UPDATE_ACTION parameter
        val isUpdate = extra.getBoolean(UPDATE_VALUE, false)
        if (isUpdate || intent.action == "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS") return Pair(
            UPDATE,
            widgetId
        )
        if (intent.action == "android.appwidget.action.APPWIDGET_UPDATE") return Pair(
            SET_VALUE,
            widgetId
        )
        Log.w(TAG, "!!! SURPRISE - [ get intent with unable action ]")
        return Pair(NO_ACTION, -1)
    }

    /*
        add click listener for settings, main buttons and for click on value TextView
     */
    private fun setOnClickForWidget(
        context: Context,
        widgetId: Int,
        views: RemoteViews,
        url: String
    ) {
        //  set on click action for config buttom
        val intent1 = Intent(
            context,
            SimpleTSWidgetConfigureActivity::class.java
        )
        //  on click for settings button
        intent1.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        val pIntent1 =
            PendingIntent.getActivity(context, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.widget_button_config, pIntent1)
        // set on click for text of value( it start run service directly )
        // изврат, однако - отправить себе intent на себя-же
        val intent2 = Intent(context, SimpleTSWidget::class.java)
        intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent2.putExtra(UPDATE_VALUE, true)
        intent2.setAction("android.appwidget.action.APPWIDGET_UPDATE")
        val pIntent2 = PendingIntent.getBroadcast(
            context,
            0,
            intent2,
            0
        )
        views.setOnClickPendingIntent(R.id.appwidget_value_text, pIntent2)
        //  on click main button
        val intent3 = Intent(context, MainActivity::class.java)
        intent3.putExtra(CALL_FROM_WIDGET, true)
        val pIntent3 = PendingIntent.getActivity(
            context,
            0,
            intent3,
            0
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
            if (settings.max == null && settings.min == null || currentValue == "-?-") false
            else WidgetUtility().actionForThreshold(context, settings, currentValue)
        Log.d(TAG, "call construct() settings ${settings.asString()}")
        val col1 = Color.parseColor(settings.backColor)
        val col2 = Color.parseColor("#40888888")
        val fontCol = if (isError) Color.RED else if (col1 > col2) Color.BLACK else Color.WHITE
        val labelCol =
            if (fontCol == Color.BLACK) Color.parseColor("#001040") else Color.parseColor("#fffaff")
        // Construct the RemoteViews object
        RemoteViews(context.packageName, R.layout.simple_t_s_widget).also { views ->
            setOnClickForWidget(context, widgetId, views, settings.reqUrl)
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
            appWidgetManager.updateAppWidget(widgetId, views)
        }
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

    enum class Colors(private val argb: Int) {
        BROWN(R.color.brown), WHITE(R.color.white), GREEN(R.color.green), BLUE(R.color.blue),
        YELLOW(R.color.yellow);

        fun getColorValue(context: Context): String = context.getString(argb)
    }


}
