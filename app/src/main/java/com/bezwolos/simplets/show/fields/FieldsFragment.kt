package com.bezwolos.simplets.show.fields

import android.content.Context
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
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bezwolos.simplets.MainActivity
import com.bezwolos.simplets.MyApp
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.data.Field

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
    ): View?{
        val that = this
        val view = inflater.inflate(R.layout.fragment_fields_list, container, false)
        prepareRecyclerView(view.findViewById(R.id.list))
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
        AlertDialog.Builder(requireContext()).setTitle(R.string.wrong_get_data_header)
            .setMessage(Html.fromHtml("<span style='color: #red'> warnMessage </span>\n<br>$warnMessage"))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                findNavController().navigate(R.id.action_to_Channels)
            }
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













