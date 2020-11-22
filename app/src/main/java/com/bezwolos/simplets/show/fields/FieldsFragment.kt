package com.bezwolos.simplets.show.fields

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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bezwolos.simplets.MainActivity
import com.bezwolos.simplets.MyApp
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.data.Field
import com.bezwolos.simplets.show.fields.dummy.DummyContent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A fragment representing a list of Items.
 */
internal class FieldsFragment : Fragment() {
    private val TAG = "simplets.FieldFrg"

    private var columnCount = 1
    private var channelId = 0L
    private lateinit var dHandler: DataHandler
    private var isWatch = false

    private lateinit var viewModel: FieldFragmentViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dHandler = (activity?.application as MyApp).getDataHandler()
        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT, 1)
            channelId = it.getLong(CHANNEL_ID_VALUE, 0L)
        }
        viewModel = ViewModelProvider(this).get(FieldFragmentViewModel::class.java)
        viewModel.prepare(dHandler)
        (activity as MainActivity).setTitleInActionBar(
            R.string.fields_fragment_title,
            viewModel.getChannelName()
        )
        // TO DO  comment in production bottom string
        // viewModel.requestDataFromSite(5)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val that = this
        // val context = this.context
        val view = inflater.inflate(R.layout.fragment_fields_list, container, false)
        prepareRecyclerView(view.findViewById(R.id.list))
        val watchButton = view.findViewById<FloatingActionButton>(R.id.button_start_watch)
        watchButton.setOnClickListener {
            Log.d(TAG, "Try Start watch of channel")
            isWatch = !isWatch
            if (viewModel.flipWatch(isWatch)) {
                if(isWatch)Toast.makeText(context, resources.getText(R.string.start_watch), Toast.LENGTH_SHORT).show()
                else Toast.makeText(context, resources.getText(R.string.stop_watch), Toast.LENGTH_SHORT).show()
                watchButton.setImageResource(if (isWatch) R.drawable.ic_watch_no_color else R.drawable.ic_play_button)
                watchButton.alpha = if (isWatch) 0.45F else 0.7F
            } else {
                if (context != null) {
                    AlertDialog.Builder(that.context!!)
                        .setTitle(resources.getText(R.string.warning))
                        .setMessage(resources.getString(R.string.wrong_watch))
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .setIcon(android.R.drawable.ic_dialog_alert).show()
                }
            }
            Log.d(TAG, "Watch for channel Started")
        }
        return view
    }

    /*   ============================   own   fun   =============================================*/

    private fun prepareRecyclerView(view: View) {
        val lifecycleOwner = this as LifecycleOwner
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = FieldsRecyclerViewAdapter(
                    getFields(),
                    viewModel,
                    lifecycleOwner
                )
            }
        }
    }

    /*
        set data from checked fields in array for show
     */
    private fun getFields(): Array<Field> {
        val res = viewModel.getFieldsToShow()
        if (res.isEmpty()) showWarningMessage()
        return res
    }

    /*
            alert message on some wrong at time get data from network
     */
    private fun showWarningMessage() {
        val warnMessage = when ((activity?.application as MyApp).getDataHandler().getLastError()) {
            DataHandler.NETWORKING_ERROR -> resources.getString(R.string.wrong_networking_message)
            DataHandler.WRONG_REQUEST -> resources.getString(R.string.wrong_request_to_thingspeak)
            else -> resources.getString(R.string.wrong_data_format)
        }
        AlertDialog.Builder(context!!).setTitle(R.string.wrong_get_data_header)
            .setMessage(Html.fromHtml("<span style='color: #red'> warnMessage </span>\n<br>$warnMessage"))
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setCancelable(true)
            .setIcon(android.R.drawable.ic_dialog_alert).show()
    }

    companion object {

        // fragment args
        const val ARG_COLUMN_COUNT = "column-count"
        const val CHANNEL_ID_VALUE = "channelId"

        @JvmStatic
        fun newInstance(columnCount: Int, channelId: Long) =
            FieldsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                    putLong(CHANNEL_ID_VALUE, channelId)
                }
            }
    }

    /* ===============================  own fun ============================================== */

    /*
     get fields values from network and make array of CurrentFieldData
     */


}













