package com.bezwolos.simplets.chart

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith


/*
    ------------  TEST be WRONG - it set alwais true results  ----------------------------
*/
@RunWith(AndroidJUnit4::class)
class ChartDataTest {
    private val TAG = "simplets.TEST"

    private fun getContext() : Context {
        Log.i(TAG, " get Context")
        return InstrumentationRegistry.getInstrumentation().targetContext
    }

    /*
      network request
      "${proto}://api.thingspeak.com/channels/ ${channelId} /fields/ ${fieldId[5]} .json?minutes= ${hoursCount*60} &median= ${medianTime} &round=2"
     */
    @Test
    @Throws(Exception::class)
    fun testRequest(){
        val cHandler = ChartDataHandler()
        print("Start chart data test")
        GlobalScope.launch(Dispatchers.IO){
            var results = cHandler.getLastDaysResults("http", 495353L, "field2", 6)
            Log.i(TAG, "get array size = ${results.size}")
            for (item in results){
                Log.i(TAG, item.toString())
            }
            assert(results.size == 12)
            results = cHandler.getLastDaysResults("http", 495373L, "field2", 6)
            assert(results.size == 1)
        }
    }


}