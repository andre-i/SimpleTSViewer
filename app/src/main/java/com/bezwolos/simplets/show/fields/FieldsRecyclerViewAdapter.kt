package com.bezwolos.simplets.show.fields

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.bezwolos.simplets.R
import com.bezwolos.simplets.chart.CHANNEL_ID
import com.bezwolos.simplets.chart.FIELD_ID
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
    private lateinit var navController : NavController


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        navController = parent.findNavController()
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_fields, parent, false)
        val holder = ViewHolder(view)
        val item = values[itemNum]
        // set values
        holder.nameView.text = item.fieldName
        holder.measureUnitView.text = item.measureUnit
        observeFieldValue(holder.valueView, itemNum)
        holder.valueView.text = item.value
        //  button
        holder.chartButton.setOnClickListener {
            goToChart(item, it)
        }
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
        val chartButton : ImageButton = view.findViewById(R.id.field_chart_button)

        override fun toString(): String {
            return super.toString() + " 'holder for fields'"
        }
    }

    /*  =================================  own  fun   =========================================*/

    /*
     on click to Chart button - go to chart fragment
     */
    private fun goToChart(item: Field, butt :View) {
        val bundle = Bundle()
        bundle.putLong( CHANNEL_ID ,item.channelId)
        bundle.putString(FIELD_ID, item.fieldId)
        navController.navigate(R.id.action_to_Chart, bundle)
    }

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