package com.bezwolos.simplets.show.fields

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.Field


/**
 * [RecyclerView.Adapter] that can display a [DummyItem].
 *
 */
class FieldsRecyclerViewAdapter(
    private val values: Array<Field>
) : RecyclerView.Adapter<FieldsRecyclerViewAdapter.ViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_fields, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.nameView.text = item.fieldName
        holder.measureUnitView.text = item.measureUnit
        holder.valueView.text = item.value
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(R.id.field_name)
        val valueView: TextView = view.findViewById(R.id.field_value)
        val measureUnitView : TextView = view.findViewById(R.id.field_measure)

        override fun toString(): String {
            return super.toString() + " 'holder for fields'"
        }
    }

}