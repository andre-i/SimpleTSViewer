package com.bezwolos.simplets.show.fields

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.Field
import kotlin.coroutines.coroutineContext


/**
 * [RecyclerView.Adapter] that can display a [DummyItem].
 *
 */
internal class FieldsRecyclerViewAdapter(
    private val values: Array<Field>,
    private val viewModel : FieldFragmentViewModel,
private val lifecycleOwner : LifecycleOwner
) : RecyclerView.Adapter<FieldsRecyclerViewAdapter.ViewHolder>() {

    private val TAG = "simplets.FieldsRVA"

   // private lateinit var viewModel : FieldFragmentViewModel
   //  private lateinit var fields : LiveData<Array<Field>>

    private var itemNum = 0


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_fields, parent, false)
        val holder = ViewHolder(view)
        val item = values[itemNum]
        holder.nameView.text = item.fieldName
        holder.measureUnitView.text = item.measureUnit
        observeFieldValue(holder.valueView, itemNum)
        holder.valueView.text = item.value
        itemNum++
        return holder
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

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

    /*  =================================  own  fun   =========================================*/


    private fun observeFieldValue(valueView: TextView, itemNum: Int) {
        val liveFields = viewModel.getFieldValues()
        liveFields.observe(lifecycleOwner , Observer { curFields ->
            // TODO comment in production
            Log.d(TAG, "Set new value for ${curFields[itemNum].fieldName}")
            valueView.text = curFields[itemNum].value
        })
    }


   /* fun setViewModel(model : FieldFragmentViewModel){
        viewModel = model
    }

    fun initLiveData(){
        fields = viewModel.getFieldValues()
    }*/

}