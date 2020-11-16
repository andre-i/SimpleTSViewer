package com.bezwolos.simplets.show.channels.dummy

import com.bezwolos.simplets.data.Channel
import java.util.ArrayList

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 *
 */
object DummyContent {

    /**
     * An array of sample (dummy) items.
     */
    val ITEMS: MutableList<Channel> = ArrayList()

    private val COUNT = 5L

    init {
        // Add some sample items.
        for (i in 1..COUNT) {
            addItem(createDummyItem(i))
        }
    }

    private fun addItem(item: Channel) {
        ITEMS.add(item)
    }

    private fun createDummyItem(position: Long): Channel {
        return Channel(
            position,
            "name Channel $position proto is",
            "HTTPs"
        )
    }

}