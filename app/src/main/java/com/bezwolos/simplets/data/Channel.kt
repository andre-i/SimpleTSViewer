package com.bezwolos.simplets.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels_table")
data class Channel(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "channelId")
    val channelId: Long,
    @ColumnInfo(name = "channelName")
    var channelName : String,
    @ColumnInfo(name = "protocolName")
    var protocolName : String = "http",
    @ColumnInfo(name = "isChecked")
    var isChecked : Boolean = false
)