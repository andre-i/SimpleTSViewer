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
import com.bezwolos.simplets.MainActivity
import com.bezwolos.simplets.MyApp
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.data.Field
import com.bezwolos.simplets.show.fields.dummy.DummyContent

/**
 * A fragment representing a list of Items.
 */
class FieldsFragment : Fragment() {
    private val TAG = "simplets.FieldFrg"

    private var columnCount = 1
    private var channelName = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT, 1)
            channelName = it.getString(CHANNEL_NAME_VALUE, "No name")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fields_list, container, false)
        prepareRecyclerView(view.findViewById(R.id.list))
        // Set the adapter
        (activity as MainActivity).setTitleInActionBar(R.string.fields_fragment_title, channelName)
        return view
    }

    /*   ============================   own   fun   =============================================*/

    private  fun  prepareRecyclerView(view :View){
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = FieldsRecyclerViewAdapter(
                    prepareFields(
                        (activity?.application as MyApp).getDataHandler().getFields()
                    )
                )
            }
        }
    }

    /*
        set data from checked fields in array for show
     */
    private fun prepareFields(fields: Array<Field>): Array<Field> {
        if(fields.isEmpty())showWarningMessage()
        var res = emptyArray<Field>()
        var n = 0
        for (item in fields) {
            if (item.isShow) {
                res += item
            }
        }
        //   Logger
        Log.d(TAG, "ARRAY TO SHOW       ")
        for(item in res) Log.d(TAG, item.toString() )
        return res
    }

    /*
            alert message on some wrong at time get data from network
     */
    private fun showWarningMessage() {
        val warnMessage = when((activity?.application as MyApp).getDataHandler().getLastError()){
            DataHandler.NETWORKING_ERROR -> resources.getString(R.string.wrong_networking_message)
            DataHandler.WRONG_REQUEST -> resources.getString(R.string.wrong_request_to_thingspeak)
            else -> resources.getString(R.string.wrong_data_format)
        }
        AlertDialog.Builder(context!!).setTitle(R.string.wrong_get_data_header)
            .setMessage(Html.fromHtml("<span style='color: #red'> warnMessage </span>\n<br>$warnMessage" ))
            .setPositiveButton(android.R.string.ok) { _, _ ->}
            .setCancelable(true)
            .setIcon(android.R.drawable.ic_dialog_alert).show()
    }

    companion object {

        // fragment args
        const val ARG_COLUMN_COUNT = "column-count"
        const val CHANNEL_NAME_VALUE = "channelName"

        @JvmStatic
        fun newInstance(columnCount: Int, channelName: String) =
            FieldsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                    putString(CHANNEL_NAME_VALUE, channelName)
                }
            }
    }

    /* ===============================  own fun ============================================== */

    /*
     get fields values from network and make array of CurrentFieldData
     */


}













