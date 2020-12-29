package com.bezwolos.simplets.show.create


import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.bezwolos.simplets.MainActivity
import com.bezwolos.simplets.MyApp
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.Channel
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.data.Field
import com.bezwolos.simplets.hideKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.StringBuilder

/**
 * A fragment representing a list of Channels.
 */
internal class PropsChannelFragment : Fragment() {
    private val TAG = "simplets.PropsChannFrg"


    //  start args
    private var columnCount = 1
    private var channelId = 0L
    private var apiKey = ""

    //  data
    private lateinit var dHandler: DataHandler
    private lateinit var viewModel: PropsViewModel

    // view
    private lateinit var recyclerView: RecyclerView
    private lateinit var idLabel: TextView
    private lateinit var nameLabel: TextView

    //  fields
    private lateinit var fields: Array<Field>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "call onCreate() in propsChannelFragment")
        // leave create own menu item
        setHasOptionsMenu(true);
        // prepare for recyclerView
        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
            channelId = it.getLong(ARG_CHANNEL_ID)
            apiKey = it.getString(ARG_CHANNEL_API_KEY) ?: ""
        }
        Log.d(TAG, "On create PropsCF have channelID=$channelId  apiKEY=$apiKey")
        if (channelId == 0L) showChannelFragment()  //  if not channel
        dHandler = (activity?.application as MyApp).getDataHandler()
        fields = dHandler.getFieldsOnCreate()
        //  viewModel
        viewModel = ViewModelProvider(this).get(PropsViewModel::class.java)
        viewModel.setFields(fields)
        //  fill channel properties
        setChannelProps()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.save_channel_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "Click on menuItem")
        if (item.itemId == R.id.save_channel_item) {
            Log.d(TAG, "click on SAVE item")
            hideKeyboard(idLabel)
            makeOnSaveAction()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "Start onCreateView(...)")
        fields = viewModel.getFields()
        if (fields.isEmpty()) showWebDataWarning()
        val view = inflater.inflate(R.layout.fragment_props_channel_list, container, false)
        //  name and ID
        idLabel = view.findViewById<TextView>(R.id.dialog_channel_ID_label)
        idLabel.setText(channelId.toString())
        nameLabel = view.findViewById<TextView>(R.id.channel_name_label)
        // get RECYCLER_VIEW
        recyclerView = view.findViewById<RecyclerView>(R.id.fields_list)
        // Set the adapter
        tuneRecyclerView(recyclerView)
/*        view.findViewById<FloatingActionButton>(R.id.button_save_channel).setOnClickListener {
            hideKeyboard(view)
            makeOnSaveAction()
        }*/

        // set channelId as title
        (activity as MainActivity).setTitleInActionBar(
            R.string.props_fragment_title,
            " Id : $channelId"
        )
        return view
    }


    companion object {

        // fragment args
        const val ARG_COLUMN_COUNT = "column-count"
        const val ARG_CHANNEL_ID = "channel-id"
        const val ARG_CHANNEL_API_KEY = "TS_read_key"

        @JvmStatic
        fun newInstance(columnCount: Int, channelId: Long, apiKey: String = "") =
            PropsChannelFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                    putLong(ARG_CHANNEL_ID, channelId)
                    putString(ARG_CHANNEL_API_KEY, apiKey)
                }
            }
    }

    /*  ============================================================================================
        ================================  own  fun  ===============================================
        ============================================================================================
     */

    /* =======================   channel   ======================================================*/

    /*
    on create or edit channel  get channel from database
    after be call dialog witch channel properties
     */
    private fun setChannelProps() {
        this.lifecycleScope.launch(Dispatchers.IO) {
            var isExist = false
            var channel = dHandler.getChannel(channelId)
            if (channel == null) channel = Channel(channelId)
            else isExist = true
            GlobalScope.launch(Dispatchers.Main) {
                showTuneChannelDialog(channel, isExist)
            }
        }
        //
    }


    /*
        show dialog for set channel settings be show before  fields values list appears
        on "OK" answer - fill channel values and save channel in viewModel, otherwise
        go to channels screen
     */
    private fun showTuneChannelDialog(channel: Channel, isExist: Boolean) {
        val title = getString(R.string.props_fragment_title, channel.channelId.toString())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_channel_props, null)
        idLabel.setText(channel.channelId.toString())
        view.findViewById<TextView>(R.id.channel_props_label).setText(
            "${resources.getString(R.string.channel_id_label)} ${channel.channelId}"
        )
        view.findViewById<EditText>(R.id.channel_api_key_value).setText(apiKey)
        if (isExist) fillValuesInChannelDialog(view, channel)
        val mBuilder = this!!.context?.let {
            AlertDialog.Builder(it)
                .setView(view)
                .setTitle(title)
                .setPositiveButton(R.string.ok_label) { dialog, id ->
                    fillChannelPropsFromDialog(view, channel)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel_label) { dialog, id ->
                    dialog.dismiss()
                    showChannelFragment()

                }
        }?.show()
    }

    /*
        after press on "ok" button get values from views and fill channel props
        @view - root dialog view
        @channel - channel to fill
     */
    private fun fillChannelPropsFromDialog(view: View, channel: Channel) {
        // channel name
        val text =
            view.findViewById<EditText>(R.id.channel_name_text_edit).getText().toString().trim()
        channel.channelName = if (text.length < 1) channel.channelId.toString() else text
        // api key
        val key = view.findViewById<EditText>(R.id.channel_api_key_value).getText().toString()
        channel.readTSKey = if (key.length != 16) {
            Toast.makeText(
                context,
                resources.getString(R.string.wrong_api_key, key.length.toString()),
                Toast.LENGTH_LONG
            ).show()
            ""
        } else key
        //  web protocol type (http or https)
        val proto = view.findViewById<Spinner>(R.id.channel_web_proto).getSelectedItem().toString()
        channel.protocolName = proto
        // request frequency
        val freq = view.findViewById<EditText>(R.id.channel_request_frequency_value).text.toString()
        val time = if (freq.length < 1) 2L else freq.toLong()
        channel.requestFrequency = if (time < 1) 2L else if (time > 300) 300L else time
        Log.d(TAG, "on close channel dialog values [ ${channel.toString()}]")
        viewModel.setChannel(channel)
    }

    /*
     if channel exist - dialog values be fill channel values
     */
    private fun fillValuesInChannelDialog(view: View, channel: Channel) {
        nameLabel.setText(channel.channelName)
        view.findViewById<EditText>(R.id.channel_name_text_edit).setText(channel.channelName)
        view.findViewById<EditText>(R.id.channel_api_key_value).setText(channel.readTSKey)
        view.findViewById<EditText>(R.id.channel_request_frequency_value)
            .setText(channel.requestFrequency.toString())
    }


    /*   =================  on connect exception  ================================*/
    /*
            show messaget about some connect error
            exception data get from DataHandler
     */
    private fun showWebDataWarning() {
        val err = dHandler.getLastError()
        val mess = when (err) {
            DataHandler.WRONG_REQUEST -> resources.getString(R.string.wrong_request_to_thingspeak)
            DataHandler.NETWORKING_ERROR -> resources.getString(R.string.wrong_networking_message)
            else -> resources.getString(R.string.wrong_data_format)
        }
        AlertDialog.Builder(requireContext()).setTitle(R.string.wrong_get_data_header)
            .setMessage(Html.fromHtml("<span color='red'>$mess</span>"))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                findNavController().navigate(R.id.action_to_Channels)
            }
            .setCancelable(true)
            .setIcon(R.drawable.ic_warning).show()
    }


    /*
       Tune RecyclerViewAdapter
       1. set layout
       2. set viewModel
     */
    private fun tuneRecyclerView(view: RecyclerView) {
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = AddChannelRecyclerViewAdapter(fields)
                (adapter as AddChannelRecyclerViewAdapter).setViewModel(viewModel)
            }
        }

    }

    /*
       get stored in viewModel channel and fields
       get fields values from RecyclerView adapter
       call dialog to save values
     */
    private fun makeOnSaveAction() {
        // get RECYCLER_VIEW
        val v = view?.findViewById<RecyclerView>(R.id.fields_list)
        if (v != null) {
            recyclerView = v
            val curFields = (v.adapter as AddChannelRecyclerViewAdapter).getCurrentFields()
            createDialog(createQuestMessqge(viewModel.getChannel(), curFields))
        }
    }


    /*
            channel and fields be gotten from user input  connect_proto_label
     */
    private fun createQuestMessqge(channel: Channel, fields: Array<Field>): String {
        val yes = resources.getString(R.string.ok_label)
        val no = resources.getString(R.string.no_label)
        val quest = StringBuilder("<big>")
        quest.append(" Id: <u>$channelId</u>   ${resources.getString(R.string.name)}  <u>${channel.channelName}</u><br /><br />")
        quest.append(" ${resources.getString(R.string.frequency_duration)}  :   <u>${channel.requestFrequency}</u><br />")
        quest.append(" ${resources.getString(R.string.connect_proto_label)}  :  <u>${channel.protocolName}</u><br />")
        quest.append(" API KEY :  <u>${channel.readTSKey}</u><br /></big><br />  ^^^^^ ^^^^^^^^^^^^^^^^^^ ^^^^^<br /> ")
        for (item in fields) {
            quest.append("<h3>${item.fieldId}</h3>")
            quest.append(" ${resources.getString(R.string.name)}        ${item.fieldName} <br>")
            quest.append(" ${resources.getString(R.string.field_measure_label)} :  ${item.measureUnit}<br>")
            quest.append(" ${resources.getString(R.string.is_show_label)} - ${if (item.isShow) yes else no}<br>")
            quest.append("        _______________________<br>")
        }
        quest.append("<b align=\"center\">${resources.getString(R.string.save_it)}</b>")
        return quest.toString()
    }

    /*
    show dialog to save channel and set it Listeners
     */
    private fun createDialog(mess: String) {
        if (context != null) AlertDialog.Builder(requireContext())
            .setTitle(R.string.save_channel_confirm)
            .setMessage(Html.fromHtml(mess))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                writeToDatabase()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                Toast.makeText(
                    context,
                    activity?.resources?.getString(R.string.cancel_label),
                    Toast.LENGTH_LONG
                ).show()
                showChannelFragment()
            }
            .setCancelable(true)
            .setIcon(android.R.drawable.ic_dialog_alert).show()
    }

    private fun writeToDatabase() {
        val channel = viewModel.getChannel()
        Log.i(TAG, "On Save Channel have channel : [ ${channel.toString()} ]")
        val fields = viewModel.getFields()
        //  log
        Log.i(TAG, "ON SAVE CHANNEL have fields")
        for (item in fields) Log.i(TAG, "[  ${item}  ]")
        GlobalScope.launch(Dispatchers.IO) {
            val success = dHandler.writeChannel(channel)
            if (success) {
                dHandler.refreshChannelsFromDB()   // update channels list
                dHandler.saveChannelFields(channelId, fields)
            }
            Log.i(TAG, "on save Channel return : $success")
            GlobalScope.launch(Dispatchers.Main) {
                if (!success) Toast.makeText(
                    context,
                    activity?.resources?.getText(R.string.fail_write_mess),
                    Toast.LENGTH_SHORT
                ).show()

                showChannelFragment()
            }
        }
    }


    private fun showChannelFragment() {
        findNavController().navigate(R.id.action_to_Channels)
    }

}