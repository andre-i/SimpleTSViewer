package com.bezwolos.simplets.data.db

import androidx.room.*
import com.bezwolos.simplets.data.Channel

@Dao()
internal interface ChannelsDao {


    @Insert
    fun insertChannel( channel : Channel)

    @Update
    fun updateChannel( channel : Channel)

    @Delete
    fun deleteChannel( channel : Channel)

    /*  get channel by ID */
    @Query("select * from channels_table where channelId = :channelId")
    fun getChannel( channelId : Long) : Channel?

    /*  get all channels from database */
    @Query("SELECT * FROM channels_table")
    fun getAllChannels(): Array<Channel>

    /* get all channels ID from table */
    @Query("SELECT channelId FROM channels_table")
    fun getIdValues() : Array<Long>

    /* return checked channel as first member of channels Array
            ( Array size  = 1 - only checked channel
     */
    @Query("select * from channels_table where isChecked = :isChecked")
    fun getVisibleChannels(isChecked : Boolean) : Array<Channel>


}