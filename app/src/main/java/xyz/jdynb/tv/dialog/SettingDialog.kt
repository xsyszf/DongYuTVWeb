package xyz.jdynb.tv.dialog

import android.content.Context
import android.os.Bundle
import com.drake.engine.base.EngineDialog
import com.drake.engine.utils.NetworkUtils
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.music.utils.SpUtils.put
import xyz.jdynb.tv.MainViewModel
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogSettingBinding

class SettingDialog(context: Context, private val mainViewModel: MainViewModel) :
  EngineDialog<DialogSettingBinding>(context, R.style.Theme_BaseDialog) {

  private var serverThread: Thread? = null

  @OptIn(ExperimentalSerializationApi::class)
  private val json = Json {
    ignoreUnknownKeys = true
    allowComments = true
    encodeDefaults = true
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_setting)
  }

  override fun initView() {
    binding.btnBack.setOnClickListener {
      dismiss()
    }

    binding.btnBack.requestFocus()

    binding.swReverseDirection.isChecked = SPKeyConstants.REVERSE_DIRECTION.getRequired(false)
    binding.swBoot.isChecked = SPKeyConstants.BOOT_AUTO_START.getRequired(true)

    binding.swBoot.setOnCheckedChangeListener { buttonView, isChecked ->
      SPKeyConstants.BOOT_AUTO_START.put(isChecked)
    }

    binding.swReverseDirection.setOnCheckedChangeListener { buttonView, isChecked ->
      SPKeyConstants.REVERSE_DIRECTION.put(isChecked)
    }

    binding.tvIp.text = NetworkUtils.getIPAddress(true)
  }

  override fun initData() {
    /*try {
      serverThread = thread {
        val serverSocket = ServerSocket(8888)
        serverSocket.reuseAddress = true
        val socket = serverSocket.accept()
        Log.i("jdy", "已连接设备: ${socket.inetAddress.hostAddress}")
        val br = BufferedReader(socket.inputStream.reader())
        val bw = socket.outputStream.writer()

        var line = br.readLine()
        while (line != null) {
          when (line) {
            "liveModel" -> {
              bw.write(json.encodeToString(mainViewModel.liveModel))
            }
          }
          line = br.readLine()
        }
        socket.shutdownInput()
        socket.close()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }*/
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    try {
      serverThread?.interrupt()
    } catch (_: InterruptedException) {
    }
  }
}