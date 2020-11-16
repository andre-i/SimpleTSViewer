package com.bezwolos.simplets.show.fields.dummy


import com.bezwolos.simplets.data.Field
import java.util.ArrayList

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 * TODO: Replace all uses of this class before publishing your app.
 */
object DummyContent {

    /**
     * An array of sample (dummy) items.
     */
    var ITEMS: Array<Field> = emptyArray<Field>()

    val channelId = 10L

    private val COUNT = 6

    init {
        // Add some sample items.
        for (i in 1..COUNT) {
            addItem(createDummyItem(i))
        }
    }

    private fun addItem(item: Field) {
        ITEMS += (item)
    }

    private fun createDummyItem(position: Int): Field {
        return Field(
            channelId,
            "field$position",
            "NAME${position}",
            "${position.hashCode()}"
        )
    }

}