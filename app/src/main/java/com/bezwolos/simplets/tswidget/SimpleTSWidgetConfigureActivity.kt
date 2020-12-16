package com.bezwolos.simplets.tswidget

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.View
import android.widget.*
import com.bezwolos.simplets.MyApp
import com.bezwolos.simplets.R
import com.bezwolos.simplets.SIMPLE_TS_WIDGET_UPDATE
import com.bezwolos.simplets.data.db.DatabaseSimpleTS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The configuration screen for the [SimpleTSWidget] AppWidget.
 */
class SimpleTSWidgetConfigureActivity : Activity(), AdapterView.OnItemSelectedListener {
    private val TAG = "simplets.tsWidget.Conf"

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var db: DatabaseSimpleTS

    // views
    private lateinit var channels: Spinner
    private lateinit var fields: Spinner
    private lateinit var isNotify: CheckBox
    private lateinit var maxNotif: EditText
    private lateinit var minNotif: EditText
    private lateinit var updateeTime: EditText
    private lateinit var fontSize: EditText
    private lateinit var colors: RadioGroup

    //  for spinner
    private lateinit var channelsCombo: Array<Pair<Long, String>>
    private lateinit var fieldsCombo: Array<Triple<String, String, String>>

    // wait for channelsCombo or fieldsCombo
    //  is filled in background
    private var isCompleted = false

    //  old background color(if exist)
    // need if user can`t choose new color
    private var oldColor = ""


    public override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(name, context, attrs)

    }

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        Log.d(TAG, " call onCreate() in config activity")
        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        setContentView(R.layout.simple_t_s_widget_configure)
        findViewById<View>(R.id.add_button).setOnClickListener(onClickListener)

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            widgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        Log.d(TAG, "widgetId = $widgetId")
        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            db = (this@SimpleTSWidgetConfigureActivity.applicationContext as MyApp).getDataBase()

            bindViews()
            // note - on empty Shared preferences return default settings
            val oldSettings = loadWidgetSettingsPref(this@SimpleTSWidgetConfigureActivity, widgetId)
            fillValues(oldSettings)
        }
        //  there must be apply changes for widgets

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ConfigActivity onDestroy")
    }

    /*     ============================================================================         */
    /*  ===========================  own fun  ================================================  */


    /*    ----------   save widget settings  ------------------------------     */

    private var onClickListener = View.OnClickListener {
        try {
            Log.d(TAG, "Start on click handler(store and redraw widget")
            // When the button is clicked, store widgetSettings
            Log.d(TAG, "save settings widgetId = $widgetId")
            saveNewSettings();
           // emit broadcast for enter new settings
            val messIntent = Intent("android.appwidget.action.APPWIDGET_UPDATE")
            messIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            messIntent.putExtra(SIMPLE_TS_WIDGET_UPDATE, true)
            sendBroadcast(messIntent)
           // Make sure we pass back the original widgetId
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "On click in widget e=${e.message}", e)
        }
    }

    /* return api key or empty string if key not found */
    private fun getChannelProp(channelId: String): Pair<String, String> {
        isCompleted = false
        var pair = Pair("http", "")
        GlobalScope.launch(Dispatchers.IO) {
            val channel = db.channelsDao.getChannel(channelId.toLong())
            if (channel != null) pair = Pair(channel.protocolName, channel.readTSKey)
            isCompleted = true
        }
        while (isCompleted == false) 1
        return pair
    }

    private fun makeRequestUrl(): String {
        val channelId = channelsCombo[channels.selectedItemPosition].first.toString()
        val channelProp = getChannelProp(channelId)
        val proto = channelProp.first
        val apiKey = if (channelProp.second == "") "" else "?api_key=${channelProp.second}"
        val fieldId = fieldsCombo[fields.selectedItemPosition].first
        return "$proto://api.thingspeak.com/channels/$channelId/fields/${fieldId[5]}/last.json$apiKey"
    }

    /*
        get values from appropriate fields if needed replace on default values
        and save to shared preferences
     */
    private fun saveNewSettings(): SimpleTSWidget.WidgetSettings {
        val context = this@SimpleTSWidgetConfigureActivity
        val reqUrl = makeRequestUrl()
        val fieldName =
            if (fieldsCombo[fields.selectedItemPosition].second.length < 1) fieldsCombo[fields.selectedItemPosition].first
            else fieldsCombo[fields.selectedItemPosition].second
        val measureUnit = fieldsCombo[fields.selectedItemPosition].third
        val tMax: Float? = try {
            maxNotif.getText().toString().toFloat()
        } catch (e: NumberFormatException) {
            null
        }
        val tMin: Float? = try {
            minNotif.getText().toString().toFloat()
        } catch (e: NumberFormatException) {
            null
        }
        val updTime = try {
            val t = updateeTime.getText().toString().toLong()
            if (t < 1) 1 else if (t > 120) 120 else t
        } catch (e: NumberFormatException) {
            30L
        }
        val fSize = try {
            fontSize.getText().toString().toFloat()
        } catch (e: NumberFormatException) {
            24F
        }
        val checkedCol = when (colors.checkedRadioButtonId) {
            R.id.conf_black_color -> SimpleTSWidget.Colors.BROWN.getColorValue(context)
            R.id.conf_blue_color -> SimpleTSWidget.Colors.BLUE.getColorValue(context)
            R.id.conf_white_color -> SimpleTSWidget.Colors.WHITE.getColorValue(context)
            R.id.conf_yellow_color -> SimpleTSWidget.Colors.YELLOW.getColorValue(context)
            R.id.conf_green_color -> SimpleTSWidget.Colors.GREEN.getColorValue(context)
            else -> if (oldColor != "") oldColor else SimpleTSWidget.Colors.BROWN.getColorValue(
                context
            )
        }
        val newSettings = SimpleTSWidget.WidgetSettings(
            reqUrl,
            fieldName,
            measureUnit,
            tMax,
            tMin,
            updTime,
            fSize,
            checkedCol
        )
        saveWidgetSettingsPref(context, newSettings, widgetId)
        return newSettings
    }


    /*   ------------  restore( or set empty ) widget settings  ---------------  */


    /* if widget be created fill values from old values */
    private fun fillValues(old: SimpleTSWidget.WidgetSettings) {
        /*  val url = old.reqUrl
          val channelId = getChannelIdFromUrl(url)
          Log.d(TAG, "fillValues()  channelId = $channelId")*/
        try {
            tuneChannelsSpinner()
            GlobalScope.launch(Dispatchers.IO) {
                while (isCompleted == false) delay(100)
                GlobalScope.launch(Dispatchers.Main) {
                    Log.d(TAG, "Start fill fields from [ ${old.asString()} ]")
                    if (old.reqUrl.length > 10)// empty settings have empty url
                        fillWithOldValues(old)
                    else tuneThresholdArea()
                }
            }
        } catch (e: Exception) {
            deleteWidgetSettings(this@SimpleTSWidgetConfigureActivity, widgetId)
            Log.e(TAG, "ERROR ${e.message}", e)
        }

    }

    /*
         for colors can`t set checked state
     */
    private fun fillWithOldValues(old: SimpleTSWidget.WidgetSettings) {
        fillSpinners(old)
        fillThreshold(old)
        updateeTime.setText("${old.updateTime}")
        fontSize.setText("${old.fontSize}")
        oldColor = old.backColor
        tuneThresholdArea()
    }

    /*
        // visibility for notify area(max, min labels)
     */
    private fun fillThreshold(old: SimpleTSWidget.WidgetSettings) {
        setAreaVisibility(old.max != null || old.min != null)
        maxNotif.setText("${old.max ?: ""}")
        minNotif.setText("${old.min ?: ""}")
    }

    /*
        fill spinners on change settings
     */
    private fun fillSpinners(old: SimpleTSWidget.WidgetSettings) {
        var pos = 0
        val url = old.reqUrl
        val channelId = if (url.indexOf("api") > 0) url.substring(
            url.indexOf("els") + 4,
            url.indexOf("fields") - 1
        )
        else "0"
        Log.d(TAG, "fillValues()  channelId = $channelId")
        for ((n, pair) in channelsCombo.withIndex()) {
            Log.d(TAG, "old channelId = $channelId")
            if (pair.first ==
                channelId.toLong()
            ) pos = n
        }
        channels.setSelection(pos)
        isCompleted = false
        GlobalScope.launch(Dispatchers.IO) {
            tuneFieldSpinner(channelId.toLong())
            while (isCompleted == false) delay(100)
            GlobalScope.launch(Dispatchers.Main) {
                pos = 0
                for ((n, pair) in fieldsCombo.withIndex()) {
                    if (pair.second == old.fieldName) pos = n
                }
                fields.setSelection(pos)
            }
        }
    }

    /*
     ----------------------  SPINNERS (channel and field)  ------------------------------
     */

    /*   fill channel spinner from database and call fun to set names to spinner */
    private fun tuneChannelsSpinner() {
        GlobalScope.launch(Dispatchers.IO) {
            val all = db.channelsDao.getAllChannels()
            Log.d(TAG, "all channels size = ${all.size}")
            var names = Array<String>(all.size) { "" }
            //  first in pair -channelId, second - channelName
            channelsCombo = Array<Pair<Long, String>>(all.size) { Pair(0L, "") }
            for ((n, channel) in all.withIndex()) {
                val pair = channel.channelId to channel.channelName
                channelsCombo[n] = pair
                names[n] = if (channel.channelName.length < 2) channel.channelId.toString()
                else channel.channelName
            }
            GlobalScope.launch(Dispatchers.Main) {
                prepareSpinner(names, channels)
                isCompleted = true
            }
        }

    }

    /*   fill field spinner from database and call fun to set names to spinner */
    private fun tuneFieldSpinner(channelId: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            val all = db.fieldsDao.getChannelFields(channelId)
            Log.d(TAG, "channel fields size=${all.size}")
            // first in triple - fieldId, second - fieldName, third - measureUnit
            fieldsCombo = Array<Triple<String, String, String>>(all.size) { Triple("", "", "") }
            var names = Array<String>(all.size) { "" }
            for ((n, field) in all.withIndex()) {
                val triple = Triple(
                    field.fieldId,
                    if (field.fieldName.length < 1) field.fieldId else field.fieldName,
                    field.measureUnit
                )
                fieldsCombo[n] = triple
                names[n] = triple.second
            }
            GlobalScope.launch(Dispatchers.Main) {
                prepareSpinner(names, fields)
                isCompleted = true
            }
        }
    }

    /* fill spinner with gotten names  */
    private fun prepareSpinner(names: Array<String>, spinner: Spinner) {
        val adapter =
            ArrayAdapter<String>(
                this@SimpleTSWidgetConfigureActivity,
                android.R.layout.simple_spinner_item,
                names
            )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.setAdapter(adapter)
        spinner.setOnItemSelectedListener(this@SimpleTSWidgetConfigureActivity)

    }

    /* spinner on itemSelectedListener methods */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        when (parent.id) {
            channels.id -> {
                val channelId = channelsCombo[pos].first
                Log.d(TAG, "SElected channel $channelId")
                tuneFieldSpinner(channelId)
            }
            fields.id -> {
                val fieldId = fieldsCombo[pos].first
                Log.d(TAG, "Selected fields $fieldId")
            }
        }
    }

    /* spinner on itemSelectedListener methods */
    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }

    /*    --------------------  END SPINNERS  ---------------------------*/

    /*
        -------------------------------  checkbox< max, min ----------------
     */

    private fun tuneThresholdArea() {
        setAreaVisibility(isNotify.isChecked)  // visibility on start
        isNotify.setOnCheckedChangeListener { buttonView, isChecked ->
            setAreaVisibility(isChecked)
        }
    }

    private fun setAreaVisibility(isChecked: Boolean) {
        val visibility = if (isChecked) View.VISIBLE else View.GONE
        maxNotif.setVisibility(visibility)
        minNotif.setVisibility(visibility)
        findViewById<TextView>(R.id.conf_notify_max_label).setVisibility(visibility)
        findViewById<TextView>(R.id.conf_notify_min_label).setVisibility(visibility)
    }


    /*
     bind views by it Id`s
 */
    private fun bindViews() {
        channels = findViewById(R.id.conf_channel_spinner)
        fields = findViewById(R.id.conf_field_spinner)
        isNotify = findViewById(R.id.conf_notify_checkbox)
        maxNotif = findViewById(R.id.conf_max_notify_val)
        minNotif = findViewById(R.id.conf_min_notify_val)
        updateeTime = findViewById(R.id.conf_update_value)
        fontSize = findViewById((R.id.conf_font_value))
        colors = findViewById(R.id.conf_colors)
    }


}


private const val PREFS_NAME = "com.bezwolos.simplets.tswidget.SimpleTSWidget"
private const val PREFIX_SETTINGS_KEY = "settings_for_widget_"


/*
    Write all settings as String to the SharedPreferences object for this widget
 */
internal fun saveWidgetSettingsPref(
    context: Context,
    settings: SimpleTSWidget.WidgetSettings,
    widgetId: Int
) {
    Log.d("simplets.widgetSettings", "Save values :${settings.asString()}")
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.putString(PREFIX_SETTINGS_KEY + widgetId, settings.asString())
    prefs.apply()
}

/*
    Read the prefix from the SharedPreferences object for widget.
            restore settings
            if there it not found or be wrong -> return empty settings
             in empty settings channelId = ""
 */
internal fun loadWidgetSettingsPref(
    context: Context,
    widgetId: Int
): SimpleTSWidget.WidgetSettings {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0)
    val asString = prefs.getString(PREFIX_SETTINGS_KEY + widgetId, "") ?: ""
    Log.d("simplets.loadWdgSetPref", "on restore have [ $asString ]")
    val old = SimpleTSWidget.WidgetSettings()
    if (asString.trim().isEmpty()) return old  // on first request return the empty settings
    val arr = asString.split("&")
    if (arr.size != 8) {// on some wrong
        deleteWidgetSettings(context, widgetId)  // delete settings
        return old  // return the empty settings
    }
    old.reqUrl = arr[0]
    old.fieldName =
        if (arr[1].contains(old.SEPARATOR_AS_TEXT)) {
            arr[1].replace(old.SEPARATOR_AS_TEXT, "&")
        } else arr[1]
    old.measureUnit = arr[2]
    old.max = if (arr[3] != "null") arr[3].toFloat() else null
    old.min = if (arr[4] != "null") arr[4].toFloat() else null
    old.updateTime = arr[5].toLong()
    old.fontSize = arr[6].toFloat()
    old.backColor = arr[7]
    Log.d(
        "simplets.loadWdgSetPref",
        "after construct have url=${old.reqUrl} fieldName=${old.fieldName} measureUnit=${old.measureUnit} max=${old.max} min=${old.min} uTime=${old.updateTime} fSize=${old.fontSize} color=${old.backColor}"
    )
    return old
    //return asString ?: ""
}

/*
    Remove the prefix from the SharedPreferences object for this widget
 */
internal fun deleteWidgetSettings(context: Context, widgetId: Int) {
    Log.d("simplets.widgetSettings", "delete settings for [ $widgetId ]")
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.remove(PREFIX_SETTINGS_KEY + widgetId)
    prefs.apply()

}

internal fun deleteAllWidgetSettings(context: Context): Boolean {
    Log.d("simplets.widgetSettings", "delete settings for all widgets")
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.clear()
    return prefs.commit()
}