package com.bezwolos.simplets.chart

import android.content.Context
import android.util.Log
import androidx.work.Operation
import com.bezwolos.simplets.MyApp
import com.bezwolos.simplets.data.Channel
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.data.TSResult
import com.jjoe64.graphview.series.DataPoint
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL


private const val WRONG_URL = "wrong"
private const val CONNECTION_ERROR = "err"
internal const val SUCCESS_RESULT = 0
internal const val WRONG_WEB = 1
internal const val WRONG_VALUE = 2
internal const val PARSE_ERROR = 3
internal const val EMPTY_ANSWER = 4

class ChartDataHandler {
    private val TAG = "simplets.ChartHnd"

    /*
      on choose period action
     */
    suspend fun getResultsForPeriod(channel: Channel, fieldId: String, startDate : String, endDate : String, medianValue : Int): Pair<Array<TSResult>, Int>{
        Log.v(TAG, "results for channel parameters : channel [ $channel ] , start $startDate end $endDate median = $medianValue")
        val key = if (channel.readTSKey.length != 16) "?" else "?api_key=${channel.readTSKey}&"
        val start = "start=$startDate"
        val end = "end=$endDate"
        val median = "median=$medianValue"
        val request = "${channel.protocolName}://api.thingspeak.com/channels/${channel.channelId}/fields/${fieldId[5]}.json" +
                "${key}${start}&${end}&${median}&round=2"
        Log.v(TAG, "on period request [ $request ]")
        return getData(request)
    }

    /*
        on choose spinner(hours or days) item action
     */
    suspend fun getLastResults(channel: Channel, fieldId: String, itemCount : Int, isDays : Boolean) : Pair<Array<TSResult>, Int>{
        Log.v(TAG, "get request for channel = [ ${channel} ] , count=$itemCount , isDays=$isDays ")
        val key = if (channel.readTSKey.length != 16) "?" else "?api_key=${channel.readTSKey}&"
        val intervalCount = if(isDays) "days=${itemCount}" else "minutes=${itemCount * 60}"
        val median = "median=${getMedianTime(itemCount, isDays)}"
        val request = "${channel.protocolName}://api.thingspeak.com/channels/${channel.channelId}/fields/${fieldId[5]}.json" +
                "${key}$intervalCount&${median}&round=2"
        Log.v(TAG, "request [ $request ]")
        return getData(request)
    }

    /*
        on choose watch action
     */
    suspend fun getForWatch(url: String) : Pair<Array<DataPoint>, Int>{
        val res = getData(url)
        return Pair(getOnlyValues(res.first), res.second)
    }

    /*
     return minutes count for requested period
      this time depend from value of hours or days
     */
    private fun getMedianTime(count : Int, isDays : Boolean) : Int {
        if(isDays){
            return when (count) {
                1 -> 0
                3 -> 20
                10 -> 60
                else -> 240
            }
        }else{
            return  when (count) {
                1 -> 0
                2 -> 0
                6 -> 10
                else -> 10
            }
        }
    }

    /*
    * call other fun for get server answer and make array with TSResults( date and value)
     */
    private suspend fun getData(url: String):  Pair<Array<TSResult>, Int>{
        val rawAnswer = getServerAnswer(url)
        if (rawAnswer == WRONG_URL || rawAnswer == CONNECTION_ERROR)
            return Pair(arrayOf(TSResult(rawAnswer,0.0 )), WRONG_WEB)
        val result = getResults(rawAnswer)
        Log.v(TAG, "get arrais size = ${result.first.size}")
        return result
    }

    /*
     accept get request to ThingSpeak and return answer string
     */
    private suspend fun getServerAnswer(reqUrl: String): String {
        Log.v(TAG, "Start HTTPS request")
        val url = URL(reqUrl)
        val response = StringBuilder("")
        try {
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.v(TAG, "Send 'GET' request to URL : $url ")
                if (responseCode != 200) {
                    Log.w(TAG, "WRONG URL \"$reqUrl\" can`t get answer")
                    return WRONG_URL
                }
                delay(2000)
                inputStream.bufferedReader().use {
                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    Log.v(TAG, "[ ${response.substring(0, 200)} ]")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ON GET DATA FROM SERVER [ ${e.toString()} ]", e)
            return CONNECTION_ERROR
        }
        return response.toString()
    }


    /*
    {"created_at":"2020-12-22T20:43:05Z","entry_id":12956,"field2":"-3.00"} - when get all values no median
    {"created_at":"2020-12-22T16:50:00Z","field2":"-2.00"}
     */
    private fun getResults(answer: String):  Pair<Array<TSResult>, Int>{
        // println("Before trim have $answer")
        val chunks = answer.substring(answer.indexOf('[') + 1, answer.indexOf(']'))
        // return on empty results
        if (chunks.length < 10) return Pair(arrayOf(TSResult("", 0.0)), EMPTY_ANSWER)
        val all = chunks.substring(1, chunks.length - 2).split("},{")
        //  println("raw data size = ${all.size}")
        Log.v(TAG, " after trim have   $chunks after split have array size = ${all.size} ")
        var n = 0
        val results = Array<TSResult>(all.size) { TSResult() }
        var shift = 0
        while (n < all.size) {
            try {
                val dateTime =
                    all[n].substring(all[n].indexOf("_at") + 6, all[n].indexOf("\",\"") - 1)
                try {
                    val value =
                        all[n].substring(all[n].indexOf("field") + 9, all[n].length - 1).toDouble()
                    if (n == 0) Log.v(
                        TAG,
                        "${TSResult(dateTime, value)}"
                    ) // for example show firs out
                    results[n - shift] = TSResult(dateTime, value)
                } catch (e: Exception) {
                    shift++
                    Log.w(TAG, "for results get unexpected value")
                }
                n++
            } catch (e: Exception) {
                Log.e(TAG, "error on get Double ${e.message}", e)
                Log.e(TAG, "error get with data [ $chunks ]")
                return Pair(arrayOf(TSResult("", 0.0)), PARSE_ERROR)
            }
            // println("${TSResult(dateTime, value)}")
        }
        if (shift > 0) {
            n = 0
            // for rarefied array return only entire part
            val trimed = Array<TSResult>(all.size - shift) { TSResult() }
            while (n < (all.size - shift)) {
                trimed[n] = results[n]
                n++
            }
            Log.i(TAG, "Send trimed Array size=${trimed.size}")
            return Pair(trimed, WRONG_VALUE)
        }
        //println("arr Size = ${results.size}")
        return Pair(results, SUCCESS_RESULT)
    }

    internal fun getOnlyValues(results: Array<TSResult>): Array<DataPoint> {
        if (results.size < 1) return arrayOf(DataPoint(0.0, 0.0))
        val points = Array<DataPoint>(results.size) { DataPoint(0.0, 0.0) }
        for ((n, item) in results.withIndex()) {
            points[n] = DataPoint(n.toDouble(), item.value)
        }
        return points
    }


}


// first {"created_at":"2020-12-20T08:30:00Z"    second  "field3":"39.0"}
// LocalDateTime   yyyy-MM-dd-HH-mm-ss-ns
//   Data   getTimezoneOffset()

