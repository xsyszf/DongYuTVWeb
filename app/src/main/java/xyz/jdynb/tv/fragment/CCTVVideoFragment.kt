package xyz.jdynb.tv.fragment

import android.os.Bundle
import android.view.View
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.utils.JsManager.execJs

class CCTVVideoFragment: VideoFragment() {

  /**
   * 禁止加载的文件
   */
  private val _blockFiles = listOf(
    "jweixin-1.4.0.js",
    "dingtalk.js",
    "configtoolV1.1.js",
    "2019whitetop/index.js",
    "pc_nav/index.js",
    "cctvnew-jquery.tinyscrollbar.js",
    "jquery.qrcode.min.js",
    "cntv_Advertise.js",
    "zhibo_shoucang.js",
    "mapjs/index.js",
    "bottomjs/index.js",
    "indexPC.js",
    "shareindex.js",
    "md5login.js",
    "login_new.js",
    "login190120home.js",
    "top2023newindex.js",
    "2019dlbhyjs/index.js",
    "mylogin.js",
    "aplus_plugin_aplus_u.js",
    "daohang211126.js",
    "gray20221130.js",
  )

  override val blockFiles: List<String>
    get() = _blockFiles

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onPageFinished(url: String) {
    super.onPageFinished(url)
    // webView.execJs(JsType.CLEAR_CCTV)
    // webView.execJs(JsType.FULLSCREEN_CCTV)
  }

}