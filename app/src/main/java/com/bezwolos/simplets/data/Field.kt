package com.bezwolos.simplets.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey

@Entity(
    tableName = "fields_table",
    primaryKeys = ["channelId", "fieldId"],
    foreignKeys = [ForeignKey(
        onDelete = CASCADE, entity = Channel::class,
        parentColumns = ["channelId"], childColumns = ["channelId"]
    )]
)
data class Field(
    @ColumnInfo(name = "channelId")
    val channelId: Long,
    @ColumnInfo(name = "fieldId")
    val fieldId: String,
    @ColumnInfo(name = "fieldName")
    val fieldName: String = "",
    @ColumnInfo(name = "measureUnit")
    val measureUnit: String = "",
    @ColumnInfo(name = "value")
    var value: String = "NaN",
    @ColumnInfo(name = "isShow")
    var isShow: Boolean = true
)