package com.bezwolos.simplets.show.create

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.Field
import com.bezwolos.simplets.hideKeyboard

/**
 * [RecyclerView.Adapter] that can display a Channels.
 *
 */
internal class AddChannelRecyclerViewAdapter(
    private val values: Array<Field>
) : RecyclerView.Adapter<AddChannelRecyclerViewAdapter.ViewHolder>() {
    private val TAG = "simplets.AddChannVA"
    private lateinit var model: PropsViewModel
    private var pos = 0


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_props_channel, parent, false)
        val holder = ViewHolder(view)
        holder.okButton.setOnClickListener {
            Log.d(TAG, "click on OK button")
            changeField(holder)
        }
        val item = values[pos]
        holder.idView.text = item.fieldId
        holder.checkVisible.isChecked = item.isShow
        holder.measureValue.setText(item.measureUnit)
        holder.nameValue.setText(item.fieldName)
        pos++
        //  view.visibility = View.VISIBLE
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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


    private fun changeField(holder: ViewHolder) {
        Log.d(TAG, "Press on ok button")
        if (model.getFields().isEmpty()) return
        //   Hide keyboard
        hideKeyboard(holder.okButton)
        val newField = Field(
            model.channelId,
            holder.idView.text.toString(),
            holder.nameValue.text.toString(),
            holder.measureValue.text.toString(),
            "NaN",
            holder.checkVisible.isChecked
        )
        model.replaceField(newField)
       // holder.pressItem()
        holder.itemView.visibility = View.GONE
        val lParams = holder.itemView.getLayoutParams()
        lParams.height = 0
        lParams.width = 0
        holder.itemView.setLayoutParams(lParams)
    }

}