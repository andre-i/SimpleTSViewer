package com.bezwolos.simplets.show.channels


import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.Channel
import com.bezwolos.simplets.show.ChannelsActionListener

/**
 * [RecyclerView.Adapter] that can display a [ChannelItem].
 *
 */
class ChannelRecyclerViewAdapter(
    private val values: Array<Channel>,
    private val context: Fragment
) : RecyclerView.Adapter<ChannelRecyclerViewAdapter.ViewHolder>() {

    private lateinit var _listener : ChannelsActionListener
   // private lateinit var context : Fragment

    private val TAG = "simplets.Chann_RV_adapt"

    private lateinit var viewModel : ShowChannelViewModel
    private lateinit var liveData : LiveData<Boolean>
    private lateinit var edit: TextView
    private lateinit var show: TextView
    private lateinit var del: TextView

    val EDIT = "edit"
    val DELETE = "delete"
    val SHOW = "show"


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        viewModel = ViewModelProvider(context).get(ShowChannelViewModel::class.java)
        liveData = viewModel.getButtonEnabled()
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_show_channel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        edit = holder.edit
        del = holder.del
        show =  holder.show
        val item = values[position]
        val isChecked = if(item.isChecked)android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background
        holder.isCheckedView.background = context.resources.getDrawable( isChecked)
        holder.idView.text = item.channelId.toString()
        holder.idName.text = item.channelName
        edit.setOnClickListener{
            makeAction(EDIT, item.channelId)
        }
       del.setOnClickListener{
           makeAction(DELETE, item.channelId)
       }
       show.setOnClickListener{
            Log.d(TAG, "Click on SHOW")
            viewModel.setButtonEnabled(false)
           item.isChecked = true
            makeAction(SHOW, item.channelId)
           // holder.show.alpha = 0.3F
        }
        observeButtons(context)
    }


    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //  channel text props
        val idView: TextView = view.findViewById(R.id.channel_id_value)     // channel Id
        val idName: TextView = view.findViewById(R.id.channel_name_value)   // channel name
        //  channel is check to show data from channel
        val isCheckedView : CheckBox = view.findViewById<CheckBox>(R.id.channel_is_checked_checkBox)
        //  bottom is action buttons
         val edit: TextView  = view.findViewById<TextView>(R.id.button_edit_channel)
        val show : TextView = view.findViewById<TextView>(R.id.button_show_channel)
        val del : TextView = view.findViewById<TextView>(R.id.button_del_channel)

        override fun toString(): String {
            return super.toString() + "items reflect channel"
        }
    }

/*  ==============================  own  fun   ================================================*/

    /*
     ( there get channel id and action name( edit, delete, show )
     */
    private fun makeAction(actionName : String, channelId : Long) {
        Log.d(TAG, "[ Press on $actionName button for $channelId  channel ]")
        _listener.onAction(actionName, channelId)
/*        when(actionName){
            "edit" -> Log.d(TAG, "not impl")
            "delete" -> Log.d(TAG, "not impl")
            "show" -> "hello"
            else -> throw IllegalStateException(" Can`t supported operation")
        }*/
    }



    /*
     listener for views make as buttom
     */
    fun setChannelsListener(listener : ChannelsActionListener) {
        val mess = "call ChangeActionListener on set Link"
        Log.d(TAG, mess)
        _listener = listener
    }

    /*
            observe buttons state on make internet request
     */
    private fun observeButtons(context : LifecycleOwner){

        liveData.observe(context, Observer { isEnabled ->
            Log.d(TAG, "liveData<Boolean>.observe change boolean")
            edit.setEnabled(isEnabled)
            show.setEnabled(isEnabled)
            del.setEnabled(isEnabled)
            if(isEnabled){
                edit.alpha = 1F
                show.alpha = 1F
                del.alpha = 1F
            }else{
                edit.alpha = 0.3F
                show.alpha = 0.3F
                del.alpha = 0.3F
            }
        })
    }
}