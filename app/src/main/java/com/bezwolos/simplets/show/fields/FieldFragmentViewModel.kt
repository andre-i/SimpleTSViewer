package com.bezwolos.simplets.show.fields

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bezwolos.simplets.MIN_PAUSE_DURATION
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.data.Field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


internal class FieldFragmentViewModel : ViewModel() {
    private val TAG = "simplets.FieldFrgmVM"

    private val values = MutableLiveData<Array<Field>>(emptyArray<Field>())
    private var channelName = " "
    private var isWatch = false
    private var frequency = 0L

    private lateinit var dataHandler: DataHandler

    fun prepare(handler: DataHandler) {
        dataHandler = handler
        setFieldsValues(getFieldsToShow())
        val name = dataHandler.getCurrentChannelName()
        channelName = if(name.length > 1)name else dataHandler.getCurrentChannelId().toString()
        frequency = dataHandler.getCurrentChannel().requestFrequency
    }

    fun getChannelName(): String {
        return channelName
    }

    fun getFieldValues(): LiveData<Array<Field>> {
        return values
    }

    fun setFieldsValues(newValues: Array<Field>) {
        values.value = newValues
    }

    /*
    @return false if meet some wrong,
        true otherwise
        wrongs(
            1 watch time less than 15c
     */
    fun flipWatch(_isWatch: Boolean): Boolean {
        Log.d(TAG, " Call flipWatch( $_isWatch )")
        isWatch = _isWatch
        if (isWatch) {
            if (frequency < MIN_PAUSE_DURATION) {
                Log.w(TAG, "Wrong pauseBetweenRequest - it must be > 5")
                return false
            }
            Log.d(TAG, "StartWatch frequency = $frequency")
            viewModelScope.launch(Dispatchers.IO) {
                updateFromSite()
            }
        } else {
            Log.d(TAG, "STOP watch")
            return true
        }
        return watchChannel(frequency)
    }

    /*
        @param pauseBetweenRequest (seconds) - pause for next request to server for get data
     pauseBetweenRequest - second
     */
    private fun watchChannel(pauseBetweenRequest: Long): Boolean {
        // val pauseBeetwenRequest = dataHandler.getCurrentChannel().requestFrequency
        val pause = pauseBetweenRequest * 1000L
        lateinit var fields: Array<Field>
        if (dataHandler == null) throw IllegalStateException("cant`t work witch data dataHandler not initialised[ FieldFragmentViewModel ]")
        this.viewModelScope.launch(Dispatchers.IO) {
            while (isWatch) {
                delay(pause)
                updateFromSite()
            }
        }
        return true
    }

    private suspend fun updateFromSite() {
        //Log.d(TAG, "request to update from FieldFragmentViewModel")
        dataHandler.updateFieldsFromTS()
        // TO DO comment after debug
/*                for (it in fields) {
                    Log.d(
                        TAG,
                        "after request fields value ${it.toString()}"
                    )
                }*/
        val newValues = getFieldsToShow()
        viewModelScope.launch(Dispatchers.Main) {
            setFieldsValues(newValues)
            // TO DO comment after debug
            /*val newVal = values.value
            if (newVal == null) {
                Log.d(TAG, "Wrong result - LiveData is NULL")
            } else {
                for (item in newVal) {
                    Log.d(
                        TAG,
                        "after request fields values = ${item.toString()}"
                    )
                }
            }*/
        }
    }

    /*
            getFields on start execute
     */
    fun getFieldsToShow(): Array<Field> {
        val fields = dataHandler.getFields()
        var res = emptyArray<Field>()
        var n = 0
        for (item in fields) {
            if (item.isShow) {
                res += item
            }
        }
        //   Logger
        Log.d(TAG, "ARRAY TO SHOW       ")
        for (item in res) Log.d(TAG, item.toString())
        return res
    }
}






