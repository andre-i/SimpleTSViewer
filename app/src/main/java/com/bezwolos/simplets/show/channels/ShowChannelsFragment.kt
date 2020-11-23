package com.bezwolos.simplets.show.channels

//
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.bezwolos.simplets.MainActivity
import com.bezwolos.simplets.MyApp
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.Channel
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.show.ChannelsActionListener
import com.bezwolos.simplets.show.create.PropsChannelFragment
import com.bezwolos.simplets.show.fields.FieldsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * A fragment representing a list of Items.
 */
internal class ShowChannelsFragment : Fragment(), ChannelsActionListener {

    private val TAG = "simplets.ShowChanFrag"

    private val listener = this
    private var columnCount = 1
    private lateinit var channels: Array<Channel>

    lateinit var viewModel: ShowChannelViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);
        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
        channels = getDataHandler().getChannels()
        viewModel = ViewModelProvider(this).get(ShowChannelViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_show_channels_list, container, false)
        (activity as MainActivity).setTitleInActionBar(R.string.channels_fragment_title)
        // Set the adapter
        if (view is RecyclerView) {
            val self = this
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = ChannelRecyclerViewAdapter(channels, self)
                with((adapter as ChannelRecyclerViewAdapter)) {
                    setChannelsListener(listener)
                }
            }
        }
        checkWaitForAnswer()
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.show_channel_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if (item.itemId == R.id.add_channel) {
            Log.d(TAG, "tap on 'add_channel")
            (activity as MainActivity).showDialog()
        }
        return true
    }


    /*
    *  implement click in channels fragment adapter
     */
    override fun onAction(type: String, channelId: Long) {
        Log.d(TAG, "onAction [ typeAction = $type  channel = ${channelId.toString()} ]")
        when (type) {
            "edit" -> editChannel(channelId)
            "delete" -> showOnDeleteConfirm(channelId)
            "show" -> showChannel(channelId)
            else -> Log.w(
                TAG,
                "get typeAction = $type"
            ) //throw IllegalStateException(" Can`t supported operation")
        }
    }

    /*  ===================  own  fun  ================================================*/


    private fun getDataHandler(): DataHandler {
        return (activity?.application as MyApp).getDataHandler()
    }

    /* on delete confirm */
    private fun showOnDeleteConfirm(channelId: Long) {
        AlertDialog.Builder(context!!).setTitle(R.string.delete_channel_header)
            .setMessage(resources.getString(R.string.on_delete_channel_confirm, ": $channelId"))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                deleteChannel(channelId)
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

    /*
        delete channel record from database
     */
    private fun deleteChannel(channelId: Long) {
        Log.i(TAG, "delete channel $channelId")
        GlobalScope.launch(Dispatchers.IO) {
            with((activity?.application as MyApp).getDataHandler()) {
                deleteChannel(channelId)
                refreshChannelsFromDB()
            }
            GlobalScope.launch(Dispatchers.Main) {
                findNavController().navigate(R.id.action_to_Channels)
            }
        }


    }


    private fun showChannel(channelId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            with((activity?.application as MyApp).getDataHandler()) {
                updateFieldsFromTS(channelId)
                makeChannelVisible(channelId)
            }
            lifecycleScope.launch(Dispatchers.Main) {
                val bundle = Bundle()
                bundle.putLong(FieldsFragment.CHANNEL_ID_VALUE, channelId)
                findNavController().navigate(R.id.action_to_Fields, bundle)

            }
        }
    }

    private fun editChannel(channelId: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            with((activity?.application as MyApp).getDataHandler()) {
                getFields(channelId)
                getChannelName(channelId)
            }
            GlobalScope.launch(Dispatchers.Main) {
                findNavController().navigate(
                    R.id.action_to_Props,
                    Bundle().apply() { putLong(PropsChannelFragment.ARG_CHANNEL_ID, channelId) })
            }
        }

    }

    private fun checkWaitForAnswer() {
        val busy = getDataHandler().isWaitForSiteAnswer()
        viewModel.setButtonEnabled(!busy)
    }


    /* =============================================================================================  */

    companion object {

        // Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        @JvmStatic
        fun newInstance(columnCount: Int) =
            ShowChannelsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }

}