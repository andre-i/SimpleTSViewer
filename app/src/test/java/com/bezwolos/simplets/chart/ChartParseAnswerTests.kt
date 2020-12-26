package com.bezwolos.simplets.chart


import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZonedDateTime

// @RunWith(AndroidJUnit4::class)
class ChartParseAnswerTests(){
    private val TAG = "DataTest"
    private lateinit var data:String

    // request https://api.thingspeak.com/channels/495353/fields/3.json?results=60&average=10
    //  have 31 results first date [ 2020-12-20T08:30:00Z ]  last date [ 2020-12-20T13:30:00Z ]
    private val largeDataAveraged ="""
{"channel":{"id":495353,"name":"Dvoriki-weather-station","description":"Данные с датчиков Двориковской школы, Тульской области Воловского района.\r\n\r\nSensors values for themperature, humidity, pressure for school in Dviriki of Tula -\u003e Volovo region.           ","latitude":"53.4884","longitude":"38.2083","field1":"tIn","field2":"tOut","field3":"humid","field4":"baro","created_at":"2018-05-14T08:01:40Z","updated_at":"2020-11-07T19:02:39Z","elevation":"236","last_entry_id":12296},"feeds":[{"created_at":"2020-12-20T08:30:00Z","field3":"39.0"},{"created_at":"2020-12-20T08:40:00Z","field3":"39.0"},{"created_at":"2020-12-20T08:50:00Z","field3":"39.0"},{"created_at":"2020-12-20T09:00:00Z","field3":"39.0"},{"created_at":"2020-12-20T09:10:00Z","field3":"39.0"},{"created_at":"2020-12-20T09:20:00Z","field3":"39.0"},{"created_at":"2020-12-20T09:30:00Z","field3":"39.0"},{"created_at":"2020-12-20T09:40:00Z","field3":"39.0"},{"created_at":"2020-12-20T09:50:00Z","field3":"39.0"},{"created_at":"2020-12-20T10:00:00Z","field3":"39.0"},{"created_at":"2020-12-20T10:10:00Z","field3":"39.0"},{"created_at":"2020-12-20T10:20:00Z","field3":"39.0"},{"created_at":"2020-12-20T10:30:00Z","field3":"39.0"},{"created_at":"2020-12-20T10:40:00Z","field3":"39.0"},{"created_at":"2020-12-20T10:50:00Z","field3":"39.0"},{"created_at":"2020-12-20T11:00:00Z","field3":"39.0"},{"created_at":"2020-12-20T11:10:00Z","field3":"39.0"},{"created_at":"2020-12-20T11:20:00Z","field3":"39.0"},{"created_at":"2020-12-20T11:30:00Z","field3":"39.0"},{"created_at":"2020-12-20T11:40:00Z","field3":"39.0"},{"created_at":"2020-12-20T11:50:00Z","field3":"39.0"},{"created_at":"2020-12-20T12:00:00Z","field3":"39.0"},{"created_at":"2020-12-20T12:10:00Z","field3":"39.0"},{"created_at":"2020-12-20T12:20:00Z","field3":"39.0"},{"created_at":"2020-12-20T12:30:00Z","field3":"39.5"},{"created_at":"2020-12-20T12:40:00Z","field3":"40.0"},{"created_at":"2020-12-20T12:50:00Z","field3":"40.0"},{"created_at":"2020-12-20T13:00:00Z","field3":"40.0"},{"created_at":"2020-12-20T13:10:00Z","field3":"40.0"},{"created_at":"2020-12-20T13:20:00Z","field3":"39.5"},{"created_at":"2020-12-20T13:30:00Z","field3":"39.0"}]}
"""


    //  request  https://api.thingspeak.com/channels/495353/fields/2.json?days=1&average=720&round=2
    //   have 3 results  first date [ 2020-12-19T12:00:00Z ]  last date  [ 2020-12-20T12:00:00Z ]
    private val dayRequest = "https://api.thingspeak.com/channels/495353/fields/2.json?days=1&average=720&round=2"
    private val dayResults = """
{"channel":{"id":495353,"name":"Dvoriki-weather-station","description":"Данные с датчиков Двориковской школы, Тульской области Воловского района.\r\n\r\nSensors values for themperature, humidity, pressure for school in Dviriki of Tula -\u003e Volovo region.           ","latitude":"53.4884","longitude":"38.2083","field1":"tIn","field2":"tOut","field3":"humid","field4":"baro","created_at":"2018-05-14T08:01:40Z","updated_at":"2020-11-07T19:02:39Z","elevation":"236","last_entry_id":12295},"feeds":[{"created_at":"2020-12-19T12:00:00Z","field2":"-1.44"},{"created_at":"2020-12-20T00:00:00Z","field2":"-2.10"},{"created_at":"2020-12-20T12:00:00Z","field2":"-1.50"}]}
    """

    @Before
    fun prepare(){
      //
        data = """
{"channel":{"id":495353,"name":"Dvoriki-weather-station","description":"Данные с датчиков Двориковской школы, Тульской области Воловского района.\r\n\r\nSensors values for themperature, humidity, pressure for school in Dviriki of Tula -\u003e Volovo region.           ","latitude":"53.4884","longitude":"38.2083","field1":"tIn","field2":"tOut","field3":"humid","field4":"baro","created_at":"2018-05-14T08:01:40Z","updated_at":"2020-11-07T19:02:39Z","elevation":"236","last_entry_id":11796},"feeds":[{"created_at":"2020-12-17T20:00:00Z","field2":"1.00"},{"created_at":"2020-12-17T21:00:00Z","field2":"1.00"},{"created_at":"2020-12-17T22:00:00Z","field2":"1.00"},{"created_at":"2020-12-17T23:00:00Z","field2":"1.00"},{"created_at":"2020-12-18T00:00:00Z","field2":"1.00"},{"created_at":"2020-12-18T01:00:00Z","field2":"0.92"},{"created_at":"2020-12-18T02:00:00Z","field2":"1.00"},{"created_at":"2020-12-18T03:00:00Z","field2":"0.17"},{"created_at":"2020-12-18T04:00:00Z","field2":"0.00"},{"created_at":"2020-12-18T05:00:00Z","field2":"0.00"},{"created_at":"2020-12-18T06:00:00Z","field2":"0.00"},{"created_at":"2020-12-18T07:00:00Z","field2":"0.92"},{"created_at":"2020-12-18T08:00:00Z","field2":"1.17"},{"created_at":"2020-12-18T09:00:00Z","field2":"1.00"},{"created_at":"2020-12-18T10:00:00Z","field2":"1.00"},{"created_at":"2020-12-18T11:00:00Z","field2":"1.00"},{"created_at":"2020-12-18T12:00:00Z","field2":"0.85"},{"created_at":"2020-12-18T13:00:00Z","field2":"0.00"},{"created_at":"2020-12-18T14:00:00Z","field2":"0.00"},{"created_at":"2020-12-18T15:00:00Z","field2":"0.00"},{"created_at":"2020-12-18T16:00:00Z","field2":"0.00"},{"created_at":"2020-12-18T17:00:00Z","field2":"0.00"},{"created_at":"2020-12-18T18:00:00Z","field2":"0.00"},{"created_at":"2020-12-18T19:00:00Z","field2":"0.00"},{"created_at":"2020-12-18T20:00:00Z","field2":"0.00"}]}            
        """.trimIndent()
    }

    @Test
    @Throws(Exception::class)
    fun onParse( ){
        val date = LocalDateTime.now()
        val offset = ZonedDateTime.now().getOffset().getTotalSeconds()
        println("DATE : ${date.toString()}  offset : $offset  offsetInHours : ${offset/3600}")
        println()
        println("Test on request $dayRequest")
        val len = ChartDataHandler().getResults(dayResults)
        assert(len.size == 3)
    }



}