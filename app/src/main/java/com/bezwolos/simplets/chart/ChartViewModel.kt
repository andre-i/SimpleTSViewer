package com.bezwolos.simplets.chart

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.Channel
import com.bezwolos.simplets.data.Field
import com.bezwolos.simplets.data.TSResult
import com.jjoe64.graphview.series.DataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChartViewModel(): ViewModel() , PeriodChooser.PeriodChooserListener{
    private val TAG = "simplets.chartVM"
    private val chartDataHandler = ChartDataHandler()


    // delay on start for spinners actions
    //  VERY useful
    private var isSpinnerDelay = true

    // watch to channel
    private var isWatch = false

    private var test : String? = null
    private lateinit var channel: Channel
    private lateinit var field: Field

    //  Live Data  first - array of results, second - return code ( see it on top ChartDataHandler )
    private val chartData = MutableLiveData<Pair<Array<DataPoint>, Int>>(Pair(arrayOf(DataPoint(0.0,0.0)), 0))
 /*   private val fragmentTitle = MutableLiveData<String>()
    private val chartTitle = MutableLiveData<String>()
    private val selectedItem = MutableLiveData<String>()*/

    /*
       set data for build chart
     */
    fun setData( newData : Pair<Array<DataPoint>, Int>){
        chartData.postValue(newData)
    }

    /*
        liveData for build chart
     */
    fun getData():LiveData<Pair<Array<DataPoint>, Int>>{
        return chartData // as LiveData<Pair<Array<DataPoint>, Int>>
    }


    fun setStartParameters( _context : Context, _channel: Channel, _field: Field){
       if( test == null){
           Log.v(TAG, "Init start parameters")
           channel = _channel
           field = _field
           test = ""
       }
        isSpinnerDelay = true
        delayForSpinner()
    }

    fun getChannelName(): String {
        return channel.channelName
    }

    /*
     fieldDescription - message above chart
     */
    fun getFieldDescription(): String {
        return "${field.fieldName} ( ${field.measureUnit} )"
    }

    /*
     on return true - can`t act on change spinner
     */
    fun isSpinnerDelay(): Boolean{
      //  Log.v(TAG, "call isSpinner Delay delay is $isSpinnerDelay")
        return isSpinnerDelay
    }

    /*
     while first 2 seconds - spinner onChange not act It need for drop
     first`s onChange event for spinner ( this event make system and not user)
     */
    private fun delayForSpinner(){
        viewModelScope.launch(Dispatchers.IO){
            delay(2000)
            isSpinnerDelay = false
            Log.v(TAG, "End Of Delay isSpinnerDelay = $isSpinnerDelay")
        }
    }

    /*
    handle action on spinner changed
try send to server request and set gotten data for show diagramm
 */
    fun handleUserChoose(count: Int, isDays: Boolean) {
        isWatch = false
        viewModelScope.launch(Dispatchers.IO) {
            Log.v(TAG, "handleuserChoose() - call handler for get chart data")
            val data = chartDataHandler.getLastResults(channel,field.fieldId,count,isDays)

            setData(Pair(chartDataHandler.getOnlyValues(data.first), data.second))
        }
    }

    /*
        listener for period chooser
     */
    override fun onPeriodChoose(start: String, end: String, median: Int) {
        isWatch = false
        viewModelScope.launch(Dispatchers.IO) {
            Log.v(TAG, "onPeriodChoose get start:$start , end:$end , median=$median")
            val data = chartDataHandler.getResultsForPeriod(channel,field.fieldId, start, end, median)
            setData(Pair(chartDataHandler.getOnlyValues(data.first), data.second))
        }
    }

    fun startWatch() : Boolean {
        Log.v(TAG, "Start watch")
        if(channel.requestFrequency < 1){
            Log.d(TAG, "return on wrong watch value")
            return false
        }
        isWatch = true
        val delay = channel.requestFrequency*1000L
        val key = if (channel.readTSKey.length != 16) "?" else "?api_key=${channel.readTSKey}&"
        val req = "${channel.protocolName}://api.thingspeak.com/channels/${channel.channelId}/fields/${field.fieldId[5]}.json" +
        "${key}results=20&round=2"
        var res = Pair(arrayOf(DataPoint(0.0, 0.0)), EMPTY_ANSWER)
        viewModelScope.launch(Dispatchers.IO){
            while(isWatch){
                delay(delay)
                if(isWatch == false)return@launch
                res = chartDataHandler.getForWatch(req)
                if(res.second == EMPTY_ANSWER)isWatch = false
                setData(res)
            }
        }
        return true
    }

}