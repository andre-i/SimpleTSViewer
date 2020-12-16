package com.bezwolos.simplets

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.show.create.PropsChannelFragment
import com.bezwolos.simplets.show.fields.FieldsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

internal class MainActivity : AppCompatActivity() {
    private val TAG = "simplets.main"

    private val IS_RESTART = "isRestart"

    // navigation
    lateinit var navController: NavController
    lateinit var dHandler: DataHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.w(TAG, "APP onCreate()")
        setContentView(R.layout.activity_main)
        // navigation
        navController = this.findNavController(R.id.nav_host_fragment)
        //  data handler
        dHandler = (this.application as MyApp).getDataHandler()
        Log.d(TAG, application.toString())
        // check first start on start from widget
        // if it is -> open channels screen
        val intent = intent
        // handle first start app
        if(savedInstanceState?.getBoolean(IS_RESTART) == null){
            val isFromWidget = intent.extras?.getBoolean(CALL_FROM_WIDGET, false) ?: false
            if(isFromWidget){
                Log.i(TAG, "start activity FROM WIDGET")
                showListChannelsFragment()
            }else{
                onStartAction()
            }
        }
        //  set back button
        val sBar = getSupportActionBar()
        if(sBar != null) sBar.setDisplayHomeAsUpEnabled(true)
        else Log.d(TAG, "CAN`t support action bar!!!")

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_RESTART, true)
    }

    /*  ==============================================================
            =============   Options Menu  ========================
        ==============================================================
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.d(TAG, "user call menu item ")
        return when (item.itemId) {
            R.id.all_channels -> showListChannelsFragment()
            R.id.help -> showHelpFragment()
            R.id.exit -> quit()
            android.R.id.home -> goBack()
            else -> {
                Log.d(TAG, "choose menu item from fragment")
                return super.onOptionsItemSelected(item)
            }
        }
    }

    /*     ==============================================================
        ======================  OWN fun ===================================
           ==============================================================
     */

    // =========  Option handlers  ========

    private fun showHelpFragment(): Boolean {
        Log.d(TAG, "tap on 'help")
        navController.navigate(R.id.action_to_Help)
        // HelpFragment.newInstance()
        return true
    }


    private fun showListChannelsFragment(): Boolean {
        Log.d(TAG, "tap on 'all_channels")
        GlobalScope.launch(Dispatchers.IO) {
            dHandler.refreshChannelsFromDB()
            GlobalScope.launch(Dispatchers.Main) {
                navController.navigate(R.id.action_to_Channels)
            }
        }
        return true
    }

    private fun quit(): Boolean {
        Log.d(TAG, "Try exit")
        this.onDestroy()
        exitProcess(0)
        return true
    }

    /*  handler back button on ActionBar  */
    private fun goBack():Boolean{
        if(navController.popBackStack() == false){
            Toast.makeText(this, "YOU IN MAIN SCREEN APPLICATION", Toast.LENGTH_SHORT).show()
        }
        return true
    }


    //  =============  channel dialog handler  =================

    private fun onStartAction() {
        GlobalScope.launch(Dispatchers.IO) {
            if ((application as MyApp).getDataBase().channelsDao.getIdValues().isEmpty()) {
                GlobalScope.launch(Dispatchers.Main) {
                    showDialog()
                }
            } else {
                GlobalScope.launch(Dispatchers.IO) {
                    dHandler.updateFieldsFromTS(dHandler.getCurrentChannelId())
                    GlobalScope.launch(Dispatchers.Main) {
                        showCheckedChannel()
                    }
                }
            }
        }
    }

    private fun showCheckedChannel() {
        val id = dHandler.getCurrentChannelId()
        val bundle = Bundle()
        bundle.putLong(FieldsFragment.CHANNEL_ID_VALUE, id)
        navController.navigate(R.id.action_to_Fields, bundle)
    }


    fun showDialog() {
        val channIdView = LayoutInflater.from(this).inflate(R.layout.add_channel_dialog, null)
        val mBuilder = AlertDialog.Builder(this)
            .setView(channIdView)
            .setTitle(resources.getString(R.string.enter_channel_id))
        val dialog = mBuilder.show()
        channIdView.findViewById<Button>(R.id.button_ok).setOnClickListener {
            val channelId =
                channIdView.findViewById<EditText>(R.id.channel_id_value).text.toString().toLong()
            val apiKey = channIdView.findViewById<EditText>(R.id.add_api_key_value).text.toString()
            Log.i(TAG, "add channel ID=$channelId   KEY=$apiKey ")
            dialog.dismiss()
            GlobalScope.launch(Dispatchers.IO) {
                (application as MyApp).getDataHandler().getFields(channelId, apiKey)
                val error = dHandler.getLastError()
                GlobalScope.launch(Dispatchers.Main) {
                    if (error.length > 1) {
                        Toast.makeText(
                            this@MainActivity,
                            "${resources.getString(R.string.wrong_get_data_header)} $error",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.navigate(R.id.action_to_Channels)
                    } else {
                        val bundle = Bundle()
                        bundle.putLong(PropsChannelFragment.ARG_CHANNEL_ID, channelId)
                        bundle.putString(PropsChannelFragment.ARG_CHANNEL_API_KEY, apiKey)
                        navController.navigate(R.id.action_to_Props, bundle)
                    }
                }
            }
        }
        channIdView.findViewById<Button>(R.id.button_no).setOnClickListener {
            dialog.dismiss()
        }

    }

    //  ===================   set main Activity action bar title  ========================== */

    fun setTitleInActionBar(newTitle: String) {
        title = newTitle
    }

    fun setTitleInActionBar(stringId: Int, param: String = "") {
        if (param == "") setTitle(stringId)
        else {
            val title = resources.getString(stringId, param)
            setTitleInActionBar(title)
        }
    }
}