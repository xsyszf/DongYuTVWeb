package xyz.jdynb.tv.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.launch
import xyz.jdynb.tv.BR

class MainModel : BaseObservable() {

  var liveItems: List<YspLiveChannelModel> = listOf()

  @Bindable
  var currentIndex: Int = 0
    set(value) {
      field = value
      notifyPropertyChanged(BR.currentIndex)
    }

  @get:Bindable
  var showStatus: Boolean = false
    set(value) {
      field = value
      notifyPropertyChanged(BR.showStatus)
    }

  @get:Bindable("currentIndex")
  val currentLiveItem get() =  liveItems.getOrNull(currentIndex) ?: YspLiveChannelModel()

  fun up() {
    if (currentIndex <= 0) {
      currentIndex = liveItems.size - 1
    } else {
      currentIndex--
    }
  }

  fun down() {
    if (currentIndex >= liveItems.size - 1) {
      currentIndex = 0
    } else {
      currentIndex++
    }
  }
}