package com.bezwolos.simplets.data.db

import androidx.room.*
import com.bezwolos.simplets.data.Field

@Dao
internal interface FieldsDao {

    @Insert
    fun insertField( field : Field)

    @Update
    fun updateField( field : Field)

    @Delete
    fun deleteField( field : Field)

    @Query("SELECT * FROM fields_table WHERE channelId =:channelId AND fieldId =:fieldId")
    fun getField( channelId : Long, fieldId : String): Field?

    @Query("SELECT * FROM fields_table WHERE channelId =:channelId")
    fun getChannelFields( channelId : Long) : Array<Field>
}