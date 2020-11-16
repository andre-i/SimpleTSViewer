package com.bezwolos.simplets.show.create

import android.util.Log
import androidx.lifecycle.ViewModel
import com.bezwolos.simplets.data.Field

class PropsViewModel : ViewModel() {
    private val TAG = "simplets.PropsVM"

    private lateinit var fields: Array<Field>

    var channelName = ""
    var channelId = 0L

    init {
        Log.d(TAG, "create PropsViewModel")
    }

    fun setFields(_fields: Array<Field>) {
        fields = _fields
    }

    fun getFields(): Array<Field> {
        return fields
    }

    fun replaceField(field: Field, index: Int) {
        if (index > (fields.size - 1)) throw IndexOutOfBoundsException(" index of fields array  is > array size")
            Log.i(TAG, "before replace [ ${fields[index]} ]")
        fields[index] = field
        Log.i(TAG, " after replace [ ${fields[index]}")
    }

}