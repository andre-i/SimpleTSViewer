package com.bezwolos.simplets.data

import android.util.Log
import com.bezwolos.simplets.data.db.DatabaseSimpleTS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

/*
    On Errors in "try- catch" block be set error message in var lastError
    before use networking methods ( get Data from internet) need clean last error message call DropErrors()
 */
internal class DataHandler(database: DatabaseSimpleTS) {
    private val TAG = "simplets.DataHandler"


    private var db: DatabaseSimpleTS
    private var curFields = emptyArray<Field>()
    private var curChannels = emptyArray<Channel>()
    private var curChannel: Channel = Channel(-1L, "Empty channel")
    private var lastError = ""
    private var waitForSiteAnswer = false

    init {
        db = database
//        val current = db.channelsDao.getVisibleChannel()
        GlobalScope.launch(Dispatchers.IO) {
            val visibleChannels = db.channelsDao.getVisibleChannels(true)
            curChannel = if (visibleChannels.size > 0) {
                visibleChannels[0]
            } else if (db.channelsDao.getIdValues().isEmpty()) {
                curChannel
            } else {
                db.channelsDao.getChannel(db.channelsDao.getIdValues()[0]) ?: curChannel
            }
        }
    }

    companion object {
        // network data errors
        const val WRONG_REQUEST = "bad_request"
        const val NETWORKING_ERROR = "connect_error"
    }

    fun getFields(): Array<Field> = curFields

    fun getLastError(): String {
        val cur = lastError
        lastError = ""
        return cur
    }


    suspend fun getFields(channelId: Long): Array<Field> {
        lastError = ""
        var nFields = getFieldsFromDB(channelId)
        if (nFields.isEmpty()) {
            //  first request to channel
            Log.d(TAG, "can`t get fields for cannel \"$channelId\" : try instantiate it")
            if (db.channelsDao.getChannel(channelId) == null) curChannel = Channel(channelId, "")
            nFields = fillFieldsFromTS(channelId)
        }
        curFields = nFields
        Log.d(TAG, "from response set array fields size[ ${curFields.size} ]")
        return curFields
    }


    fun getCurrentChannelName(): String {
        return curChannel.channelName
    }

    fun getCurrentChannelId(): Long {
        return curChannel.channelId
    }

    suspend fun getChannelName(channelId: Long): String {
        if (curChannel.channelId == channelId) return curChannel.channelName
        var cur: Channel?
        try {
            cur = db.channelsDao.getChannel(channelId)
        } catch (e: Exception) {
            lastError = "For channel $channelId database occur error : ${e.message}"
            cur = null
        }
        if (cur != null) curChannel = cur
        else curChannel.channelName = "DataBase Error"
        return curChannel.channelName
    }

    fun isWaitForSiteAnswer(): Boolean = waitForSiteAnswer


//                             ================================
/*  ======================================  NetWorking  ========================================= */
//                             ================================

    /*
        use for first connect to Thingspeak
     */
    private fun fillFieldsFromTS(channelId: Long, proto: String = "http"): Array<Field> {
        var mFields = emptyArray<Field>()
        val url = "${proto}s://api.thingspeak.com/channels/${channelId}/feeds/last.json"
        val res = getDataFromTS(url)
        if (res.isEmpty()) {
            Log.w(TAG, "empty response -> Array be empty")
            return mFields
        }
        lateinit var field: Field
        var n = 0
        for (item in res) {
            val cur = item.split(':')
            if (cur.size < 2) {
                Log.w(TAG, "WRONG field in response - [ $item ]")
                return mFields
            }
            field = Field(channelId, cur.get(0))
            Log.d(TAG, "try add \"${field.fieldId}\"")
            mFields += field
            n++
        }
        return mFields
    }

    /*
    make get request by url param to thingspeak and return List witch fields data
     */
    private fun getDataFromTS(_url: String): List<String> {
        waitForSiteAnswer = true
        Log.d(TAG, "get Channel Data from server")
        lastError = ""
        val url = URL(_url)
        val response = StringBuilder("")
        Log.d(TAG, "Start HTTPS request")
        try {
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d(TAG, "\nSent 'GET' request to URL : $url ")
                if (responseCode != 200) {
                    Log.w(TAG, "WRONG URL \"$_url\" can`t get answer")
                    lastError = WRONG_REQUEST
                    return emptyList<String>()
                }
                inputStream.bufferedReader().use {
                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    Log.d(TAG, "[ ${response.toString()} ]")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ON GET DATA FROM SERVER ", e)
            Log.w(TAG, "Error is : [${e.toString()}]")
            lastError = NETWORKING_ERROR
            waitForSiteAnswer = false
            return emptyList()
        }
        val full = response.toString()
        val first = full.indexOf("field", 0, true) - 1
        var fields = full.substring(first, full.length - 1)
        fields = fields.replace("\"", "")
        Log.d(TAG, "fields is [ $fields ]")
        waitForSiteAnswer = false
        return fields.split(",")
    }

    /*
        get values from Thingspeak for exists channel and set fields value to Field
     */
    suspend fun updateFieldsFromTS(channelId: Long = 0L) {
        val id = if(channelId == 0L) curChannel.channelId else channelId
        val data = getDataFromTSOnUpdate(id)
        if (data.isEmpty()) return  // if array is empty do nothing
        val all = db.fieldsDao.getChannelFields(id)
        if (all.isEmpty() || all.size != data.size) {
            Log.w(
                TAG,
                "wrong data (empty database fields or fields count changed on Thingspeak) I DELETE CHANNEL{$id}"
            )
            // action on wrong data
            deleteChannel(id)
            refreshChannelsFromDB()
            curFields = emptyArray<Field>()
            return  // if array is empty do nothing
        }
        for ((n, item) in data.withIndex()) {
            val cur = item.split(':')
            all[n].value = cur[1]
        }
        curFields = all
    }

    /*
        try get and data from thingspeak
        if is some wrong - log on problen and return empty answer
     */
    private suspend fun getDataFromTSOnUpdate(channelId: Long): List<String> {
        val channel = db.channelsDao.getChannel(channelId)
        if (channel == null) {
            Log.e(
                TAG,
                "CANT get channel on updateFieldsFromTS",
                IllegalStateException("on update channel must be not null")
            )
            curFields = emptyArray()
            return emptyList()
        }
        val url =
            "${channel.protocolName}://api.thingspeak.com/channels/$channelId/feeds/last.json"
        val res = getDataFromTS(url)
        if (res.isEmpty()) {
            Log.w(TAG, "empty response -> Array be empty")
            curFields = emptyArray()
        }
        curChannel = channel
        return res
    }


//  ==========================================================================================
/* ============================== database operation  ========================================  */
//  ==========================================================================================


    /*              witch field op
        =================================  FIELD  ============================================
     */

    private fun getFieldsFromDB(channelId: Long): Array<Field> {
        return db.fieldsDao.getChannelFields(channelId)

    }


    suspend fun saveChannelFields(channelId: Long, fields: Array<Field>) {
        for (item in fields) {
            if (db.fieldsDao.getField(channelId, item.fieldId) == null) db.fieldsDao.insertField(
                item
            )
            else db.fieldsDao.updateField(item)
        }
    }

    /*                            write or change channel
            ============================   CHANNEL  ==========================================
     */
/*
    suspend fun getChannel(channelId : Long): Channel? {
       return db.channelsDao.getChannel(channelId)
    }*/


  suspend fun writeChannel(channel: Channel): Boolean {
        try {
            if (db.channelsDao.getChannel(channel.channelId) == null) {
                db.channelsDao.insertChannel(channel)
            } else {
                db.channelsDao.updateChannel(channel)
            }
            return true
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "on wite Channel : ERROR ", e)
            return false
        }
    }

    /*
        set isSchow for gotten channel and remove for another
     */
   suspend fun makeChannelVisible(channelId: Long): Boolean {
        val channel = db.channelsDao.getChannel(channelId) ?: return false
        try {
            Log.d(TAG, "get old checked")
            val checkeds = db.channelsDao.getVisibleChannels(true)
            Log.d(TAG, "get visible channels size - ${checkeds.size} ")
            if (checkeds.size > 0) {
                for(old in checkeds) {
                    val oldId = old.channelId
                    old.isChecked = false
                    db.channelsDao.updateChannel(old)
                    Log.d(
                        TAG,
                        "after old replaced visible = ${db.channelsDao.getChannel(oldId)?.isChecked}"
                    )
                }
            }else Log.d(TAG, "can`t get visible channels -  nothing replace!!!")
            Log.d(TAG, "get channel $channelId")
            channel.isChecked = true
            Log.d(TAG, "try change channel visibility")
            return writeChannel(channel)
        } catch (e: Exception) {
            Log.e(TAG, "on make channelVisible", e)
            return false
        }

    }

    suspend fun deleteChannel(channel: Channel): Boolean {
        try {
            db.channelsDao.deleteChannel(channel)
            Log.d(TAG, "Succes delete ${channel.channelId} channel")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "on delete channel", e)
            return false
        }
    }

    suspend fun deleteChannel(channelId: Long) {
        val channel = db.channelsDao.getChannel(channelId)
        if (channel != null) db.channelsDao.deleteChannel(channel)
    }

    /*
        return current channels arry
     */
    fun getChannels(): Array<Channel> {
        return curChannels
    }

    /*
        get channels array from database, set is  as current channels array
        and return it
     */
    suspend fun refreshChannelsFromDB(): Array<Channel> {
        val all = db.channelsDao.getAllChannels()
        curChannels = all
        return curChannels
    }

    /*
        return current channel
     */
    fun getCurrentChannel():Channel{
        return curChannel
    }
}