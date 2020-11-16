package com.bezwolos.simplets

import android.app.Application
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.room.Room
import com.bezwolos.simplets.data.DataHandler
import com.bezwolos.simplets.data.db.DatabaseSimpleTS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MyApp : Application() {

    lateinit var instance : MyApp

    //  DataBase
    private lateinit var database : DatabaseSimpleTS
    private var DB : DatabaseSimpleTS? = null

    // DataHandle (data base + network
    private lateinit var dataHandler : DataHandler


    override fun onCreate() {
        super.onCreate()
        getDataBase()
        dataHandler = DataHandler(database)
        populateChannels()
        instance = this
    }

    override fun toString(): String {
        return "My Custom Application Context"
    }

    /* ================================  own fun ================================================  */

    fun getDataBase() : DatabaseSimpleTS{
        if(DB == null) {
            DB = Room.databaseBuilder<DatabaseSimpleTS>(
                this,
                DatabaseSimpleTS::class.java,
                "database"
            )
                .build()
            database = DB as DatabaseSimpleTS
        }
        return database
    }

    fun getDataHandler(): DataHandler{
        return dataHandler
    }

    /*
      populate start array 0f Channel
     */
    private fun populateChannels(){
        GlobalScope.launch(Dispatchers.IO){
            dataHandler.refreshChannelsFromDB()
        }
    }



}

/*
      rootviewContext - context gotten from view.getRootView()
      childEditText  - some view from rootView
      actually - val v = some
                 val rootContext = some.rootView.context
 */
/*
fun hideKeyboard( rootViewContext : Context, childEditText : View){
    val imm = rootViewContext?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm?.let { it.hideSoftInputFromWindow(childEditText.windowToken, 0) }
    // imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
}
*/



/*
        hide soft keyboard
        view be View from window that have views with input
 */
fun hideKeyboard( view : View){
    val imm = view.rootView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm?.let{it.hideSoftInputFromWindow(view.windowToken, 0) }
}





