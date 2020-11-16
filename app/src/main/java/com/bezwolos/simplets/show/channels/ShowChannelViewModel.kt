package com.bezwolos.simplets.show.channels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ShowChannelViewModel : ViewModel() {

    private val isButtonsEnabled : MutableLiveData<Boolean> = MutableLiveData(true)

    fun setButtonEnabled( isEnabled : Boolean){
        isButtonsEnabled.value = isEnabled
    }

    fun getButtonEnabled(): LiveData<Boolean>{
        return isButtonsEnabled;
    }


}