package com.bezwolos.simplets.chart


import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bezwolos.simplets.R
import java.util.*

class PeriodChooser : DialogFragment() {
    private val TAG = "simplets.PerChoos"

    // for return choosed dates
    internal lateinit var listener: PeriodChooserListener

    // calendar
    val dateAndTime = Calendar.getInstance();

    //  date viewers
    private lateinit var startTextView: TextView
    private lateinit var endTextView: TextView

    // date holders
    private var startDate = ""
    private var endDate = ""

    // dates
    private val START = true
    private val END = false


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            val view = inflater.inflate(R.layout.dialog_period_chooser, null)
            //  prepare field for show results
            startTextView = view.findViewById(R.id.chart_chooser_start_result)
            endTextView = view.findViewById(R.id.chart_chooser_end_result)
            //  buttons for choose dates
            view.findViewById<Button>(R.id.chart_chooser_start_date_button).setOnClickListener {
                chooseDate(START);
            }
            view.findViewById<Button>(R.id.chart_chooser_end_date_button).setOnClickListener {
                chooseDate(END)
            }
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view)
                .setPositiveButton(R.string.ok_label) { dialog, id ->
                    handleUserChoose()
                    dismiss()
                }
                .setNegativeButton(R.string.cancel_label) { dialog, id ->
                    showToast(resources.getString(R.string.cancel_label))
                    getDialog()?.cancel()
                }
            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun showToast(message: CharSequence) =
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    /*
            show datePickerDialog and set listener
     */
    private fun chooseDate(isStart: Boolean) {
        context?.let {
            DatePickerDialog(
                requireContext(),
                { view, year, monthOfYear, dayOfMonth ->
                    setDate(year, monthOfYear, dayOfMonth, isStart)
                },
                dateAndTime[Calendar.YEAR],
                dateAndTime[Calendar.MONTH],
                dateAndTime[Calendar.DAY_OF_MONTH]
            ).show()
        }
    }

    /*
      format start date for ThingSpeak
      show end user choosed date
   */
    private fun setDate(year: Int, monthOfYear: Int, dayOfMonth: Int, isStart: Boolean) {
        if(checkHasWrong(year,monthOfYear,dayOfMonth))return
        val month = if (monthOfYear < 10) "0$monthOfYear" else "$monthOfYear"
        val day = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
        if (isStart == START) {
            startDate = "${year}-${month}-${day}%2000:00:00"
            startTextView.setText("${year}-${month}-${day}")
        } else {
            endDate = "${year}-${month}-${day}%2023:59:59"
            endTextView.setText("${year}-${month}-${day}")
        }
    }

    /*
    check what date is small than now
    */
    private fun checkHasWrong(year: Int, monthOfYear: Int, dayOfMonth: Int): Boolean {
        if(year < dateAndTime[Calendar.YEAR]){
            if(dateAndTime[Calendar.YEAR] - year > 1){
                showToast(context?.getString(R.string.long_period).toString())
                return true
            }
            return false
        }
        if(year == dateAndTime[Calendar.YEAR] &&
            monthOfYear < dateAndTime[Calendar.MONTH]){
            return false
        }
        if(year == dateAndTime[Calendar.YEAR] &&
            monthOfYear == dateAndTime[Calendar.MONTH] &&
            dayOfMonth <= dateAndTime[Calendar.DAY_OF_MONTH]){
            return false
        }
        showToast(context?.getString(R.string.choose_wrong_date).toString())
        return true
    }



    // ======================= check and set choose dates     ===================================

    /*
    who need get results of choose -> must implement this interface
    and set "this" as listener
     */
    interface PeriodChooserListener {
        fun onPeriodChoose(start: String, end: String, median: Int)
    }


    /*
    set listener for -get computed period from date chooser after verifu and compute median value
     */
    fun setOnPeriodChooseListener(_listener: PeriodChooserListener) {
        listener = _listener
    }

    /*
     check gotten dates, compute median, and emit event for listener
     */
    private fun handleUserChoose() {
        // check on empty period
        if (startDate.length < 2) {
            showToast(context?.getString(R.string.empty_date).toString())
            return
        }
        if (endDate.length < 2) {
            endDate = startDate.replace("00:00:00", "23:59:59")
            // set median = 10
            listener.onPeriodChoose(startDate, endDate, 10)
            return
        }
        //
        if (startDate > endDate) {
            Log.i(TAG, "End date < Start date !! ")
            showToast(context?.getString(R.string.start_great_end).toString())
            return
        } else {
            Log.d(TAG, "It is Ok : end date > start date")
            // showToast("OK : End > Start")
        }
        val median = computeMedian()
        Log.v(TAG, "call listener")
        listener.onPeriodChoose(startDate, endDate, median)
    }

    /*
    compute median for show chart from gotten period
     */
    private fun computeMedian(): Int {
        val startsList = startDate.split('%')[0].split('-')
        Log.v(TAG, "startList size = ${startsList.size}")
        val endsList = (endDate.split('%')[0]).split('-')
        val years = endsList[0].toInt() - startsList[0].toInt()
        val mounths = endsList[1].toInt() - startsList[1].toInt()
        val days = endsList[2].toInt() - startsList[2].toInt()
        Log.v(TAG, "after parse have years=$years, mounths=$mounths, days=$days")
        if (years == 0 && mounths == 0) return computeForDays(days)
        if (years == 0) return computeForMounths(mounths)
        return 0
    }

    private fun computeForDays(days: Int): Int {
        val res = when (days) {
            1 -> 10
            2 -> 15
            3, 4 -> 20
            in 5..10 -> 60
            in 10..20 -> 240
            else -> 720
        }
        Log.v(TAG, "computed for days median=$res")
        return res
    }


    private fun computeForMounths(months: Int): Int {
        val res = when (months) {
            1, 2, 3 -> 720
            else -> 1440
        }
        Log.v(TAG, "computed for months median=$res")
        return res
    }
}
