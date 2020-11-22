package com.bezwolos.simplets

import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bezwolos.simplets.data.Channel
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.data.Field
import com.bezwolos.simplets.data.db.ChannelsDao
import com.bezwolos.simplets.data.db.DatabaseSimpleTS
import com.bezwolos.simplets.data.db.FieldsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.NullPointerException

//
//
///**
// * This is not meant to be a full set of tests. For simplicity, most of your samples do not
// * include tests. However, when building the Room, it is helpful to make sure it works before
// * adding the UI.
// */

@RunWith(AndroidJUnit4::class)
class SimpleDBT {

    private val TAG = "simplets.TEST"

    private lateinit var dataHandler: DataHandler
    private lateinit var db: DatabaseSimpleTS
    private lateinit var channelsDao: ChannelsDao
    private lateinit var fieldsDao: FieldsDao

    //  data for test
    val id_1 = 4007L
    val id_2 = 3333L


    @Before
    fun createDb() {
        Log.i(TAG, " START PREPARE DataBase")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        db = Room.inMemoryDatabaseBuilder(context, DatabaseSimpleTS::class.java)
            // Allowing main thread queries, just for testing.
            .allowMainThreadQueries()
            .build()
        channelsDao = db.channelsDao
        fieldsDao = db.fieldsDao
        fillDb()
        dataHandler = DataHandler(db)
    }

    private fun fillDb() {
        val channel = Channel(id_1, "DvorikiMeteo", "HTTP", true)
        channelsDao.insertChannel(channel)
        val channel2 = Channel(id_2, "Inside lab", "HTTPS", true)
        channelsDao.insertChannel(channel2)
        for (i in 1..6) {
            val fieldId = "field$i"
            val fName = "Name_for[$i]"
            fieldsDao.insertField(Field(id_2, fieldId, fName))
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun checkCannelsId() {
        val all = channelsDao.getIdValues()
        Log.d(TAG, "have ${all.size} channels [${all.get(0)} , ${all.get(1)}]")
        assertEquals(all.size, 2)
        assertEquals(all.get(0), id_1)
        assertEquals(all.get(1), id_2)
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetChannel() {
        val id = 1L
        val channel = Channel(id, "DvorikiMeteo")
        channelsDao.insertChannel(channel)
        val newChannel = channelsDao.getChannel(id)
        Log.d(TAG, "Channel = [ ${newChannel.toString()} ]")
        assertEquals(newChannel?.protocolName, "http")
        assertEquals(channel, newChannel)
        channelsDao.deleteChannel(channel)
        assertNull(channelsDao.getChannel(1L))
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetField() {
        val channelId = 2L
        channelsDao.insertChannel(Channel(channelId, "Test for create", "https"))
        val fieldId = "field1"
        val fieldName = "SomeName"
        val field = Field(channelId, fieldId, fieldName, "°C")
        fieldsDao.insertField(field)
        Log.d(TAG, "field [ ${field.toString()} ]")
        val gotten = fieldsDao.getField(channelId, fieldId)
        assertEquals(field.fieldName, gotten?.fieldName)
        assertEquals(field, gotten)
    }

    @Test
    @Throws(Exception::class)
    fun checkCascadeRemove() {
        Log.d(TAG, " start cascade test")
        assertEquals(channelsDao.getAllChannels().size, 2)
        assertEquals(fieldsDao.getChannelFields(id_2).size, 6)
        assertEquals(fieldsDao.getChannelFields(id_1).size, 0)
        val channel = channelsDao.getChannel(id_2)
        assertNotEquals(channel, null)
        if (channel != null) channelsDao.deleteChannel(channel)
        assertEquals(fieldsDao.getChannelFields(id_2).size, 0)

    }

    @Test
    @Throws(Exception::class)
    fun checkChangeIsChecked() {
        //   channels
        Log.d(TAG, "[  checkChangeIsChecked  ]")
        Log.d(TAG, " CHECK change channel isChecked")
        val channel = channelsDao.getChannel(id_1)
        if (channel == null) throw NullPointerException("isChecked is null")
        var old = channel.isChecked
        channel.isChecked = !(channel.isChecked)
        channelsDao.updateChannel(channel)
        Log.d(
            TAG,
            "old isChecked = $old  | new IsChecked = ${channelsDao.getChannel(id_1)?.isChecked} "
        )
        assert(old == channelsDao.getChannel(id_1)?.isChecked)
        //  fields
        Log.d(TAG, "CHECK change field isEnabled")
        val field = fieldsDao.getField(id_2, "field2")
        if (field == null) throw NullPointerException("field is null")
        old = field.isShow
        field.isShow = !field.isShow
        fieldsDao.updateField(field)
        val new = fieldsDao.getField(id_2, "field2")?.isShow
        Log.d(TAG, "old isShow = $old | new isShow = $new")
        assert(old != new)
    }


    @Test
    @Throws(Exception::class)
    fun checkSetChannelIsChecked() {
        checkChangeIsChecked()
        Log.d(TAG, "[  checkSetIsChecked  ]")
        showDBcontent()
        val old = db.channelsDao.getChannel(id_1)?.isChecked
        Log.d(TAG, "old val -$old")
        assertEquals(old, false)
        dataHandler.makeChannelVisible(id_1)
        val new = db.channelsDao.getChannel(id_1)?.isChecked
        Log.d(TAG, "new val -$new")
        assertNotNull(new)
        assertNotEquals(old, new)
        Log.d(TAG, "after replace isChecked ")
        showDBcontent()
    }

    private fun showDBcontent(){
        val all = dataHandler.getChannels()
        Log.d(TAG, "CHANNELS LIST")
        if(all.size > 0){
            for(item in all){
                Log.d(TAG, "id: ${item.channelId}\t name : ${item.channelName}\t proto : ${item.protocolName}\t isChecked : ${item.isChecked}")
            }
        }else( Log.d(TAG, "channels is empty"))
    }

}