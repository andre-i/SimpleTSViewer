package com.bezwolos.simplets.help

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bezwolos.simplets.MainActivity
import com.bezwolos.simplets.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * A simple [Fragment] subclass.
 * Use the [HelpFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HelpFragment : Fragment() {
    private val TAG = "simplets.help"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);
        (activity as MainActivity).setTitleInActionBar(R.string.help_fragment_title)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_help, container, false)
        // back button action
        view.findViewById<FloatingActionButton>(R.id.fab_back)?.setOnClickListener {
                    Log.d(TAG, "tap on 'fabBack' button ")
                    getFragmentManager()?.popBackStackImmediate();
                    //  findNavController().navigate(R.id.action_Help_to_ShowFields)
                }

        return view
    }

    override fun onDestroy() {
        activity?.actionBar?.setDisplayHomeAsUpEnabled(true)
        super.onDestroy()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HelpFragment.
         */
        @JvmStatic
        fun newInstance(/*param1: String, param2: String*/) = HelpFragment()

          /*  HelpFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
           */
    }
}