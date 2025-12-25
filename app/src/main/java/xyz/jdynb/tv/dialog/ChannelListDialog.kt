package xyz.jdynb.tv.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineDialog
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.DialogChannelListBinding
import xyz.jdynb.tv.model.LiveChannelGroupModel
import xyz.jdynb.tv.model.LiveChannelModel

class ChannelListDialog(context: Context) :
  EngineDialog<DialogChannelListBinding>(context, R.style.ChannelDialogStyle) {

  private val liveChannelGroupList = mutableListOf<LiveChannelGroupModel>()

  private var currentIndex = 0

  private var currentLiveChannel = LiveChannelModel()

  var onChannelChange: ((liveChannelModel: LiveChannelModel) -> Boolean)? = null

  fun setLiveChannelList(channelList: List<LiveChannelModel>) {
    liveChannelGroupList.clear()
    liveChannelGroupList.addAll(channelList.groupBy { it.channelType }.map {
      LiveChannelGroupModel(it.key, it.value)
    })
  }

  fun setCurrentLiveChannel(liveChannelModel: LiveChannelModel) {
    currentLiveChannel = liveChannelModel
  }

  private fun getCurrentIndex(): Int {
    val index =
      liveChannelGroupList.indexOfFirst { it.channelType == currentLiveChannel.channelType }
    return if (index == -1) 0 else index
  }

  @SuppressLint("NotifyDataSetChanged")
  private fun updateCurrentChannelList() {
    val currentChannelGroup = liveChannelGroupList[currentIndex]
    val currentChannelList = currentChannelGroup.channelList.onEach {
      it.isSelected = false
    }
    binding.tvChannel.text = liveChannelGroupList[currentIndex].channelType
    binding.rvChannel.models = currentChannelList
    val position = if (currentChannelGroup.channelType == currentLiveChannel.channelType) {
      currentChannelList.indexOfFirst { it.channelName == currentLiveChannel.channelName }
    } else {
      0
    }
    binding.rvChannel.bindingAdapter.setChecked(position, true)
    scrollToPositionWithCenter(position)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_channel_list)
  }

  override fun initView() {
    val attrs = window!!.attributes
    attrs.gravity = Gravity.START

    binding.rvChannel.dividerSpace(10).setup {
      singleMode = true

      addType<LiveChannelModel>(R.layout.item_list_channel)

      onChecked { position, checked, allChecked ->
        val model = getModel<LiveChannelModel>(position)
        model.isSelected = checked
        model.notifyChange()
      }

      R.id.tv_channel.onClick {
        setChecked(modelPosition, true)
        onChannelChange?.invoke(getModel())
        dismiss()
      }
    }

    binding.btnLeft.setOnClickListener {
      onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent(KeyEvent.ACTION_DOWN, 0))
    }

    binding.btnRight.setOnClickListener {
      onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_DOWN, 0))
    }
  }

  override fun initData() {
    currentIndex = getCurrentIndex()
    updateCurrentChannelList()
  }

  private fun scrollToPositionWithCenter(position: Int) {
    val layoutManager = binding.rvChannel.layoutManager as LinearLayoutManager
    // 先获取RecyclerView和item的尺寸
    binding.rvChannel.post {
      val recyclerViewHeight: Int = binding.rvChannel.height
      // 获取指定位置的item view（必须等待布局完成）
      val child: View? = layoutManager.findViewByPosition(position)
      if (child != null) {
        val itemHeight = child.height
        val offset = recyclerViewHeight / 2 - itemHeight / 2
        layoutManager.scrollToPositionWithOffset(position, offset)
      } else {
        // 如果item还没显示，先滚动到大致位置
        layoutManager.scrollToPosition(position)
        // 再次尝试获取view并调整位置
        binding.rvChannel.post {
          val childAgain: View? = layoutManager.findViewByPosition(position)
          if (childAgain != null) {
            val itemHeight = childAgain.height
            val offset = recyclerViewHeight / 2 - itemHeight / 2
            layoutManager.scrollToPositionWithOffset(position, offset)
          }
        }
      }
    }
  }

  @SuppressLint("GestureBackNavigation")
  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    return when (keyCode) {
      KeyEvent.KEYCODE_DPAD_UP -> {
        var checkedPosition = binding.rvChannel.bindingAdapter.checkedPosition[0]
        if (checkedPosition == 0) {
          checkedPosition = binding.rvChannel.bindingAdapter.itemCount - 1
        } else {
          checkedPosition--
        }
        binding.rvChannel.bindingAdapter.setChecked(checkedPosition, true)
        binding.rvChannel.scrollToPosition(checkedPosition)
        true
      }

      KeyEvent.KEYCODE_DPAD_DOWN -> {
        var checkedPosition = binding.rvChannel.bindingAdapter.checkedPosition[0]
        if (checkedPosition == binding.rvChannel.bindingAdapter.itemCount - 1) {
          checkedPosition = 0
        } else {
          checkedPosition++
        }
        binding.rvChannel.bindingAdapter.setChecked(checkedPosition, true)
        binding.rvChannel.scrollToPosition(checkedPosition)
        true
      }

      KeyEvent.KEYCODE_DPAD_LEFT -> {
        if (currentIndex == 0) {
          currentIndex = liveChannelGroupList.size - 1
        } else {
          currentIndex--
        }
        updateCurrentChannelList()
        true
      }

      KeyEvent.KEYCODE_DPAD_RIGHT -> {
        if (currentIndex == liveChannelGroupList.size - 1) {
          currentIndex = 0
        } else {
          currentIndex++
        }
        updateCurrentChannelList()
        true
      }

      KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
        val model = binding.rvChannel.bindingAdapter.getModel<LiveChannelModel>(
          binding.rvChannel.bindingAdapter.checkedPosition[0]
        )
        onChannelChange?.invoke(model)
        dismiss()
        true
      }

      KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> {
        dismiss()
        true
      }

      else -> false
    }
  }
}