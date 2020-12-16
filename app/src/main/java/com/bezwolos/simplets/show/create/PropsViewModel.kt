package com.bezwolos.simplets.show.create

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bezwolos.simplets.data.Channel
import com.bezwolos.simplets.data.Field

internal class PropsViewModel : ViewModel() {
    private val TAG = "simplets.PropsVM"

    private lateinit var fields: Array<Field>

    private lateinit var channel : Channel


    init {
        Log.d(TAG, "create PropsViewModel")
    }

    fun setChannel(_channel : Channel){
        channel = _channel
    }

    fun getChannel()= channel

    fun setFields(_fields: Array<Field>) {
        fields = _fields
    }

    fun getFields(): Array<Field> {
        return fields
    }

    fun replaceField(newField: Field) {
        Log.d(TAG, "on replace get ${newField.toString()} ")
        for((ind, field) in fields.withIndex() ){
            if(field.fieldId == newField.fieldId){
                Log.i(TAG, "before replace [ $field ]")
                fields[ind] = newField
                Log.i(TAG, " after replace [ ${fields[ind]}")
            }
        }
    }

}