package com.bezwolos.simplets

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class Snipets (){

    private val TAG="snipets"
    suspend fun sendGet() {

        val url = URL("https://api.thingspeak.com/channels/495353/feeds/last.json")
        var response = StringBuilder("")
        Log.d(TAG, "Start HTTPS request")
        thread(start = true) {  //  omited for suspend fun
            try {
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"  // optional default is GET

                    Log.d(TAG, "\nSent 'GET' request to URL : $url ")

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
                Log.e(TAG, "Error in sendGet ", e)
            }
            val res = response.toString()
            val first = res.indexOf("field", 0, true) - 1
            val fields = " { ${res.substring(first)} "
            Log.d(TAG, "fields is [ $fields ]")
        }
        // answer on request
        """ {"created_at":"2020-11-10T14:25:39Z","entry_id":822,"field1":"20","field2":"3","field3":"56","field4":"749"} """
    }

}

/*
 Check network connection

private fun isNetworkConnected(): Boolean {
  //1
  val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  //2
  val activeNetwork = connectivityManager.activeNetwork
  //3
  val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
  //4
  return networkCapabilities != null &&
      networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

//  whu handle connect
if (isNetworkConnected()) {
    ...
    some do witch internet
    ...
  }
} else {
  AlertDialog.Builder(this).setTitle("No Internet Connection")
      .setMessage("Please check your internet connection and try again")
      .setPositiveButton(android.R.string.ok) { _, _ -> }
      .setIcon(android.R.drawable.ic_dialog_alert).show()
}




 */
