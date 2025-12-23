package xyz.jdynb.tv

import android.Manifest
import android.annotation.SuppressLint
import android.app.appsearch.observer.ObserverCallback
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.jdynb.tv.databinding.ActivityMainBinding
import xyz.jdynb.tv.enums.LiveSource
import xyz.jdynb.tv.fragment.CCTVVideoFragment
import xyz.jdynb.tv.fragment.LivePlayerFragment
import xyz.jdynb.tv.fragment.VideoFragment
import xyz.jdynb.tv.fragment.YspVideoFragment
import xyz.jdynb.tv.model.LiveItem
import xyz.jdynb.tv.model.MainModel
import xyz.jdynb.tv.model.YspLiveChannelModel
import xyz.jdynb.tv.utils.JsManager
import xyz.jdynb.tv.utils.setSerializableArguments
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

  companion object {

    private const val TAG = "MainActivity"

  }

  private lateinit var binding: ActivityMainBinding

  private lateinit var livePlayerFragment: LivePlayerFragment

  private var beforeIndex = 0

  private val mainModel = MainModel()

  private val numberStringBuilder = StringBuilder()

  private val liveItems = mutableListOf<YspLiveChannelModel>()

  private val handler = Handler(Looper.getMainLooper())

  private val timeRunnable = Runnable {
    mainModel.showStatus = false
  }

  private val numberRunnable = Runnable {
    mainModel.showStatus = false
    numberStringBuilder.clear()

    if (mainModel.currentIndex < 0 || mainModel.currentIndex >= liveItems.size) {
      // 回滚
      mainModel.currentIndex = beforeIndex
      return@Runnable
    }

    mainModel.notifyPropertyChanged(BR.currentIndex)

    Log.i(TAG, "seekTo number: ${mainModel.currentIndex}")
  }

  private lateinit var audioManager: AudioManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.hide(WindowInsetsCompat.Type.systemBars())

    audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    val config = getSharedPreferences("config", MODE_PRIVATE)

    binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    binding.m = mainModel

    ActivityCompat.requestPermissions(
      this, arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ), 1
    )

    lifecycleScope.launch {
      withContext(Dispatchers.IO) {
        JsManager.init(this@MainActivity)
        assets.open("lives_ysp.json").use {
          val liveJsonContent = it.readBytes().toString(StandardCharsets.UTF_8)
          val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
          }
          liveItems.addAll(json.decodeFromString<List<YspLiveChannelModel>>(liveJsonContent))
          mainModel.liveItems = liveItems
        }
      }

      mainModel.currentIndex = config.getInt("currentIndex", 0)
      livePlayerFragment = LivePlayerFragment.newInstance(mainModel.currentLiveItem)

      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment, livePlayerFragment)
        .commitNow()
    }

    mainModel.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
      override fun onPropertyChanged(sender: Observable, propertyId: Int) {
        if (propertyId == BR.currentIndex) {

          if (numberStringBuilder.isNotEmpty()) {
            // 表示正在输入中...
            return
          }

          val currentIndex = mainModel.currentIndex

          beforeIndex = currentIndex

          config.edit {
            putInt("currentIndex", currentIndex)
          }
          mainModel.showStatus = true
          handler.removeCallbacks(timeRunnable)
          handler.postDelayed(timeRunnable, 5000)

          if (::livePlayerFragment.isInitialized)
            livePlayerFragment.play(mainModel.currentLiveItem)
        }
      }
    })
  }

  @SuppressLint("GestureBackNavigation")
  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {

    when (keyCode) {
      /**
       * 上
       */
      KeyEvent.KEYCODE_DPAD_UP -> {
        mainModel.up()
      }

      /**
       * 下
       */
      KeyEvent.KEYCODE_DPAD_DOWN -> {
        mainModel.down()
      }

      // ENTER、OK（确认）
      KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> {
        if (::livePlayerFragment.isInitialized) {
          livePlayerFragment.playOrPause()
        }
      }

      // 静音
      KeyEvent.KEYCODE_MUTE -> {
        try {
          audioManager.setStreamVolume(
            AudioManager.STREAM_SYSTEM,
            0,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
          )
        } catch (_: SecurityException) {}
      }

      KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_LEFT -> {
        try {
          audioManager.adjustStreamVolume(
            AudioManager.STREAM_SYSTEM,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
          )
        } catch (_: SecurityException) {
        }
      }

      KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> {
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
        if (volume < audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)) {
          try {
            audioManager.adjustStreamVolume(
              AudioManager.STREAM_SYSTEM,
              AudioManager.ADJUST_RAISE,
              AudioManager.FLAG_SHOW_UI
            )
          } catch (_: SecurityException) {
          }
        }
      }

      // 返回
      KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
        if (mainModel.showStatus) {
          mainModel.showStatus = false
          handler.removeCallbacks(numberRunnable)
        }
      }

      // 主页
      KeyEvent.KEYCODE_HOME -> {

      }

      // 菜单
      KeyEvent.KEYCODE_MENU -> {

      }

      // 0
      // 数字
      KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
      KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7,
      KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
      KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2,
      KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5,
      KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8,
      KeyEvent.KEYCODE_NUMPAD_9 -> {
        val num = getNumForKeyCode(keyCode)

        Log.i(TAG, "input number: $num")

        numberStringBuilder.append(num)

        if (!mainModel.showStatus) {
          mainModel.showStatus = true
        }
        val number = numberStringBuilder.toString().toIntOrNull() ?: return super.onKeyUp(keyCode, event)
        mainModel.currentIndex = number - 1

        handler.removeCallbacks(numberRunnable)
        handler.postDelayed(numberRunnable, 4000)
      }
    }
    return super.onKeyUp(keyCode, event)
  }

  private fun getNumForKeyCode(keyCode: Int): String {
    return when (keyCode) {
      KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> "0"
      KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> "1"
      KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> "2"
      KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> "3"
      KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> "4"
      KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> "5"
      KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> "6"
      KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> "7"
      KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> "8"
      KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> "9"
      else -> ""
    }
  }
}