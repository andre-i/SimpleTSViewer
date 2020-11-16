package com.bezwolos.simplets

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.show.create.PropsChannelFragment
import com.bezwolos.simplets.show.fields.FieldsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.zip.Inflater

class MainActivity : AppCompatActivity() {
    private val TAG = "simplets.main"

    private val IS_RESTART = "isRestart"

    // navigation
    lateinit var navController: NavController
    lateinit var dHandler: DataHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // navigation
        navController = this.findNavController(R.id.nav_host_fragment)
        //  data handler
        dHandler = (this.application as MyApp).getDataHandler()
        Log.w(TAG, "APP onCreate()")
        Log.d(TAG, application.toString())
        if (savedInstanceState?.getBoolean(IS_RESTART) == null) onStartAction()

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
            else -> super.onOptionsItemSelected(item)
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


    //  =============  channel dialog handler  =================

    private fun onStartAction() {
        GlobalScope.launch(Dispatchers.IO) {
            if ((application as MyApp).getDataBase().channelsDao.getIdValues().isEmpty()) {
                GlobalScope.launch(Dispatchers.Main) {
                    showDialog()
                }
            } else {
                dHandler.updateFieldsFromTS(dHandler.getCurrentChannelId())
                GlobalScope.launch(Dispatchers.Main) {
                    showCheckedChannel()
                }
            }
        }
    }

    private fun showCheckedChannel() {
        var name = dHandler.getChannelName()
        if (name.length < 1) name = "${dHandler.getCurrentChannelId()}"
        val bundle = Bundle()
        bundle.putCharSequence(FieldsFragment.CHANNEL_NAME_VALUE, name)
        navController.navigate(R.id.action_Channel_to_Fields, bundle)
    }


    fun showDialog() {
        val channIdView = LayoutInflater.from(this).inflate(R.layout.add_channel_dialog, null)
        val mBuilder = AlertDialog.Builder(this)
            .setView(channIdView)
            .setTitle(resources.getString(R.string.enter_channel_id))
        val dialog = mBuilder.show()
        channIdView.findViewById<Button>(R.id.button_ok).setOnClickListener {
            val res =
                channIdView.findViewById<EditText>(R.id.channel_id_value).text.toString().toLong()
            dialog.dismiss()
            GlobalScope.launch(Dispatchers.IO) {
                (application as MyApp).getDataHandler().getFields(res)
                GlobalScope.launch(Dispatchers.Main) {
                    val bundle = Bundle()
                    bundle.putLong(PropsChannelFragment.ARG_CHANNEL_ID, res)
                    navController.navigate(R.id.action_to_Props, bundle)
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