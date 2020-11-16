package com.bezwolos.simplets.data.db

import androidx.room.*
import com.bezwolos.simplets.data.Channel

@Dao()
interface ChannelsDao {


    @Insert
    fun insertChannel( channel : Channel)

    @Update
    fun updateChannel( channel : Channel)

    @Delete
    fun deleteChannel( channel : Channel)

    @Query("select * from channels_table where channelId = :channelId")
    fun getChannel( channelId : Long) : Channel?

    @Query("SELECT * FROM channels_table")
    fun getAllChannels(): Array<Channel>

    @Query("SELECT channelId FROM channels_table")
    fun getIdValues() : Array<Long>

    @Query("select * from channels_table where isChecked = :isChecked")
    fun getVisibleChannels(isChecked : Boolean) : Array<Channel>

}