package com.bezwolos.simplets.show.create


import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bezwolos.simplets.MainActivity
import com.bezwolos.simplets.MyApp
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.Channel
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.data.Field
import com.bezwolos.simplets.hideKeyboard
import com.bezwolos.simplets.show.create.dummy.DummyContent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.StringBuilder

/**
 * A fragment representing a list of Channels.
 */
class PropsChannelFragment : Fragment() {
    private val TAG = "simplets.PropsChannFrg"


    //  start args
    private var columnCount = 1
    private var channelId = 0L

    //  data
    private lateinit var viewModel: PropsViewModel

    // view
    private lateinit var _view: View //  text field for insert channel name
    private var channelName = ""

    //  fields
    private lateinit var fields: Array<Field>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "call onCreate() in propsChannelFragment")
        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
            channelId = it.getLong(ARG_CHANNEL_ID)
        }
        //  viewModel
        viewModel = ViewModelProvider(this).get(PropsViewModel::class.java)
        fields = (activity?.application as MyApp).getDataHandler().getFields()  //.getFields()
        viewModel.setFields(fields)
        viewModel.channelId = channelId
        viewModel.channelName = (activity?.application as MyApp).getDataHandler().getChannelName()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fields = viewModel.getFields()
        channelId = viewModel.channelId
        val view = inflater.inflate(R.layout.fragment_props_channel_list, container, false)
        // get RECYCLER_VIEW
        val recyclerView = view.findViewById<RecyclerView>(R.id.fields_list)
        // Set the adapter
        tuneRecyclerView(recyclerView)
        _view = view.findViewById<EditText>(R.id.edit_channel_name)
        tuneChannelNameEditText(_view as EditText)
        view.findViewById<FloatingActionButton>(R.id.button_save_channel).setOnClickListener {
            hideKeyboard(view)
            checkOnSaveChannel()
        }
        // set channelId as title
        (activity as MainActivity).setTitleInActionBar(R.string.props_fragment_title," Id : $channelId" )
        return view
    }


    /*
       Set RecyclerViewAdapter
     */
    private fun tuneRecyclerView(view: RecyclerView) {
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = AddChannelRecyclerViewAdapter(fields)
                (adapter as AddChannelRecyclerViewAdapter).setVievModel(viewModel)
            }
        }

    }

    companion object {

        // fragment args
        const val ARG_COLUMN_COUNT = "column-count"
        const val ARG_CHANNEL_ID = "channel-id"

        @JvmStatic
        fun newInstance(columnCount: Int, channelId: Long) =
            PropsChannelFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                    putLong(ARG_CHANNEL_ID, channelId)
                }
            }
    }

    /*  ============================================================================================
        ================================  inner fun  ===============================================
        ============================================================================================
     */

    /*
      on focus lost -> set text from view to channelName var
      view - EditText for enter to user the channel name
     */
    private fun tuneChannelNameEditText(view: EditText) {
        view.setText(viewModel.channelName)
        view.setOnFocusChangeListener { v, hasFocus ->
            //  Log.d(TAG, "change focus hasFocus = $hasFocus text = \"${(v as EditText).text}\"")
            if (!hasFocus && v != null) {
                channelName = view.text.toString()
                Log.d(TAG, "get channelName = \"$channelName\"")
                viewModel.channelName = channelName
            }
        }
    }


    private fun checkOnSaveChannel() {
        val channel = Channel(channelId, viewModel.channelName)
        val fields = viewModel.getFields()
        createDialog(createQuestMessqge(channel, fields))
    }

    private fun createQuestMessqge(channel: Channel, fields: Array<Field>): String {
        val yes = resources.getString(R.string.ok_label)
        val no = resources.getString(R.string.no_label)
        val quest = StringBuilder("")
        quest.append(" Id: <u>$channelId</u> ${resources.getString(R.string.name)} <u>${channel.channelName}</u>")
        for (item in fields) {
            quest.append("<h3>${item.fieldId}</h3>")
            quest.append(" ${resources.getString(R.string.name)} :        \"${item.fieldName}\" <br>")
            quest.append(" ${resources.getString(R.string.field_measure_label)} : \"${item.measureUnit}\"<br>")
            quest.append(" ${resources.getString(R.string.is_show_label)} - \"${if(item.isShow) yes else no}\"<br>")
            quest.append("        _______________________<br>")
        }
        quest.append("<b align=\"center\">${resources.getString(R.string.save_it)}</b>")
        return quest.toString()
    }

    /*
    show dialog to save channel and set it Listeners
     */
    private fun createDialog(mess: String) {
        AlertDialog.Builder(context!!).setTitle(R.string.save_channel_confirm)
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
            }
            .setCancelable(true)
            .setIcon(android.R.drawable.ic_dialog_alert).show()
    }

    private fun writeToDatabase() {
        val channel = Channel(channelId, viewModel.channelName)
        val fields = viewModel.getFields()
        val dHandler = (activity?.application as MyApp).getDataHandler()
        //  log
        Log.i(TAG, "ON SAVE CHANNEL have fields")
        for( item in fields)Log.i(TAG, "[  ${item}  ]")
        GlobalScope.launch(Dispatchers.IO) {
            val success = dHandler.writeChannel(channel)
            if(success){
                dHandler.refreshChannelsFromDB()   // update channels list
                dHandler.saveChannelFields(channelId, fields)
            }
            Log.i(TAG, "on save Channel return : $success")
            GlobalScope.launch(Dispatchers.Main) {
                if (success) showChannelFragment()
                else Toast.makeText(
                    context,
                    activity?.resources?.getText(R.string.fail_write_mess),
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }

    private fun  showChannelFragment(){
        findNavController().navigate(R.id.action_PropsChannel_to_Channels)
    }
}