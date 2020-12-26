package com.bezwolos.simplets.chart

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.bezwolos.simplets.MainActivity
import com.bezwolos.simplets.MyApp
import com.bezwolos.simplets.R
import com.bezwolos.simplets.data.Channel
import com.bezwolos.simplets.data.Field
import com.bezwolos.simplets.data.TSResult
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.LegendRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.fixedRateTimer


// the fragment initialization parameters
internal const val CHANNEL_ID = "channelId"
internal const val FIELD_ID = "fieldId"

// TODO - on make period button action do not omit for ishandleRequest

/**
 * A simple [Fragment] subclass.
 * Use the [ChartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChartFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private val TAG = "simplets.ChartFrg"

    // chartDataHandler
    private val chartDataHandler = ChartDataHandler()

    // viewModel
    private lateinit var viewModel: ChartViewModel
    private lateinit var chartData: LiveData<Pair<Array<DataPoint>, Int>>

    // views
    private lateinit var hourSpinner: Spinner
    private lateinit var daySpinner: Spinner
    private lateinit var periodButton: TextView
    private lateinit var graphView: GraphView

    // for delay on get database data
    private var isReady = false

    // request for
    private val HOURS = false
    private val DAYS = true

    // freeze for wait
    private var isHandleRequest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var channelId = 0L
        var fieldId = ""
        arguments?.let {
            channelId = it.getLong(CHANNEL_ID)
            fieldId = it.getString(FIELD_ID) ?: ""
        }
        setParam(channelId, fieldId)
        viewModel = ViewModelProvider(this).get(ChartViewModel::class.java)
        Log.v(
            TAG,
            "onCreate have parameters channelId = $channelId , fieldId = $fieldId"
        )
    }

    /*
     set values for channel and field (get channel and field from database) construct title
     */
    private fun setParam(channelId: Long, fieldId: String) {
        Log.d(TAG, "onCreate()")
        isReady = false
        lifecycleScope.launch(Dispatchers.IO) {
            val db = (context?.applicationContext as MyApp).getDataBase()
            val channel = db.channelsDao.getChannel(channelId) ?: Channel(channelId)
            val field = db.fieldsDao.getField(channelId, fieldId) ?: Field(channel.channelId, "")
            if (field.fieldId == "") {
                lifecycleScope.launch(Dispatchers.Main) {
                    // on some Wrong
                    showWrong("ERROR: wrong Channel id or field")
                    isReady = true
                    findNavController().navigate(R.id.action_to_Channels)
                }
            } else {
                viewModel.setStartParameters(requireContext(), channel, field)
                chartData = viewModel.getData()
                lifecycleScope.launch(Dispatchers.Main) {
                    setLabels()
                    //  isReady = false

                    //  isReady = true
                }
            }

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView()")
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chart, container, false)
        //  views
        periodButton = view.findViewById(R.id.chart_period_button)
        periodButton.setOnClickListener {
            val periodChooser = PeriodChooser()
            periodChooser.setOnPeriodChooseListener(viewModel as PeriodChooser.PeriodChooserListener)
            this!!.fragmentManager?.let { it1 -> periodChooser.show(it1, "periodChooser") }
        }
        graphView = view.findViewById(R.id.graph)
        hourSpinner = view.findViewById(R.id.chart_hour_spinner)
        hourSpinner.onItemSelectedListener = this
        daySpinner = view.findViewById(R.id.chart_day_spinner)
        daySpinner.onItemSelectedListener = this
        //  observer for graphView
        chartData.observe(
            viewLifecycleOwner,
            Observer { data -> drawSimpleChart(data.first, data.second) })
        return view
    }

    private fun setLabels() {
        Log.d(TAG, "Set labels")
        // set labels
        (activity as MainActivity).setTitleInActionBar(
            R.string.chart_fragment_title,
            viewModel.getChannelName()
        )
        view?.findViewById<TextView>(R.id.chart_name)
            ?.setText(
                resources.getString(
                    R.string.chart_name_label,
                    viewModel.getFieldDescription()
                )
            )

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param channelName
         * @param channelId
         * @param fieldDescription
         * @param fieldId
         * @return A new instance of fragment ChartFragment.
         */
        @JvmStatic
        fun newInstance(
            channelName: String,
            channelId: Long,
            fieldDescription: String,
            fieldId: String,
            proto: String
        ) =
            ChartFragment().apply {
                arguments = Bundle().apply {
                    //   putString(CHANNEL_NAME, channelName)
                    //   putString(FIELD_DESCRIPTION, fieldDescription)
                    //   putString(CHANNEL_PROTO, proto)
                    putLong(CHANNEL_ID, channelId)
                    putString(FIELD_ID, fieldId)
                }
            }
    }

    /*  ----------------------------  IMPLEMENT interface  -----------------------------------*/

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }


    /*
     handle change item
     */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        // if in it time handle request - do nothing
        if (viewModel.isSpinnerDelay()) return
        if (isHandleRequest == true) {
            Log.v(TAG, "device is Busy - do nothing")
            return
        }
        isHandleRequest = true
        hourSpinner.setEnabled(false)
        daySpinner.setEnabled(false)
        // handle change values
        when (parent.id) {
            //  hour spinner handler
            hourSpinner.id -> {
                Log.v(TAG, "choose item in Hour spinner")
                val hours = when (position) {
                    0 -> 1
                    1 -> 2
                    2 -> 6
                    else -> 12
                }
                viewModel.handleUserChoose(hours, HOURS)
            }
            // day spinner handler
            daySpinner.id -> {
                Log.v(TAG, "choose item in Day spinner")
                val days = when (position) {
                    0 -> 1
                    1 -> 3
                    2 -> 10
                    else -> 30
                }
                viewModel.handleUserChoose(days, DAYS)
            }
        }
        isHandleRequest = false
        hourSpinner.setEnabled(true)
        daySpinner.setEnabled(true)
    }


    /* ==============================  own  fun  ================================================*/


    private fun showWrong(mess: CharSequence) =
        Toast.makeText(context, mess, Toast.LENGTH_LONG).show()


    /*
    draw chart with only values
     */
    private fun drawSimpleChart(points: Array<DataPoint>, hasResult: Int) {
        Log.d(
            TAG,
            "call Draw Chart with points size=${points.size}  request result=$hasResult"
        )
        graphView.removeAllSeries()
        prepareCanvas(graphView, points)
        val series: LineGraphSeries<DataPoint> = LineGraphSeries<DataPoint>(points)
        // if thingspeak send null for some values - create warning for user(we can`t show it)
        val chartColor = when (hasResult) {
            //  some  wrong witch data handle
            WRONG_WEB -> {
                showWrong(context?.getString(R.string.wrong_networking_message).toString())
                return
            }
            WRONG_VALUE -> {
                showWrong(context?.getString(R.string.wrong_data_format).toString())
                return
            }
            EMPTY_ANSWER -> {
               // Log.v(TAG, " show message on Empty answer -")
                showWrong(resources?.getString(R.string.chart_on_empty_mess).toString())
                return
            }
            //  good data handle
            SUCCESS_RESULT -> {
                resources.getColor(R.color.chart_normal_color)
            }
            else -> {
                graphView.getLegendRenderer().setVisible(true)
                graphView.getLegendRenderer().backgroundColor = Color.WHITE
                graphView.getLegendRenderer().textColor = Color.RED
                graphView.getLegendRenderer().align = LegendRenderer.LegendAlign.TOP
                series.setTitle(resources.getString(R.string.chart_drop_null))
                resources.getColor(R.color.chart_with_wrong)
            }
        }
        series.color = chartColor
        series.thickness = 4
        series.isDrawDataPoints = true
        series.dataPointsRadius = 4F
        graphView.addSeries(series)
    }

    /*
    prepare graphView for draw
     */
    private fun prepareCanvas(graphView: GraphView, points: Array<DataPoint>) {
        graphView.getLegendRenderer().setVisible(false)
        // can`t set label for x-axis
        graphView.getGridLabelRenderer().setLabelFormatter(MyFormatter())
        // compute values for x axis and prepare viewport
        graphView.getViewport().setXAxisBoundsManual(true)
        graphView.getViewport().setMinX(0.0)
        val maxX = points[points.size - 1].x
        graphView.getViewport().setMaxX(maxX + maxX / 20)
        graphView.getViewport().setScrollable(true)
/*        var str = ""
        for (n in 0 until points.size) str += " ${points[n].x}, ${points[n].y} :"
        Log.v(TAG, "to draw $str")*/
    }

    /*
     omit all x-axis labels
     */
    class MyFormatter : DefaultLabelFormatter() {

        override fun formatLabel(value: Double, isValueX: Boolean): String {
            if (isValueX) {
                // show empty x values
                return ""
            } else {
                // show currency for y values
                return super.formatLabel(value, isValueX);
            }
        }
    }

}