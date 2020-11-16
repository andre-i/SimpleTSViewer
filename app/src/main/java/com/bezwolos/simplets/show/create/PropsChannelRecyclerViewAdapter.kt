package com.bezwolos.simplets.show.create

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.Field
import com.bezwolos.simplets.hideKeyboard

/**
 * [RecyclerView.Adapter] that can display a Channels.
 *
 */
class AddChannelRecyclerViewAdapter(
    private val values: Array<Field>
) : RecyclerView.Adapter<AddChannelRecyclerViewAdapter.ViewHolder>() {
    private val TAG = "simplets.AddChannVA"
    private lateinit var model: PropsViewModel


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_props_channel, parent, false)
        view.visibility = View.VISIBLE
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.idView.text = item.fieldId
        holder.checkVisible.isChecked = item.isShow
        holder.measureValue.setText(item.measureUnit)
        holder.nameValue.setText(item.fieldName)
        holder.okButton.setOnClickListener { changeField(holder, position) }
    }

    private fun changeField(holder: ViewHolder, i: Int) {
        if (model.getFields().isEmpty()) return
        Log.d(TAG, "Press on ok button")
        // Check if no view has focus:
        val v = holder.okButton
        val cont = holder.checkVisible.rootView.context
     //   hideKeyboard(cont, v)
    hideKeyboard(v)
        val new = Field(
            model.channelId,
            holder.idView.text.toString(),
            holder.nameValue.text.toString(),
            holder.measureValue.text.toString(),
            "NaN",
            holder.checkVisible.isChecked
        )

  /*
        val imm = cont?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm?.let { it.hideSoftInputFromWindow(v.windowToken, 0) }
        // imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);*/

        model.replaceField(new, i)
        holder.itemView.visibility = View.INVISIBLE
       // (holder.itemView as View).visibility = View.INVISIBLE
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idView: TextView = view.findViewById(R.id.field_id_text)
        val nameValue: EditText = view.findViewById(R.id.field_name_value)
        val measureValue: EditText = view.findViewById(R.id.measure_unit_value)
        val checkVisible: CheckBox = view.findViewById(R.id.check_is_visible)
        val okButton: TextView = view.findViewById(R.id.field_button_ok)

        override fun toString(): String {
            return super.toString() + " '" + idView.text + "'"
        }


    }

    /*  ==============  own methods  ==================*/
    fun setVievModel(viewModel: PropsViewModel) {
        model = viewModel
    }
}