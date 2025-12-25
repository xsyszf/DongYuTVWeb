package xyz.jdynb.tv.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.FragmentLivePlayerBinding
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.event.Playable
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.model.LivePlayerModel
import xyz.jdynb.tv.utils.JsManager.execJs
import xyz.jdynb.tv.utils.getSerializableForKey
import xyz.jdynb.tv.utils.setSerializableArguments
import java.io.ByteArrayInputStream

open class LivePlayerFragment: Fragment(), Playable {

  companion object {
    fun newInstance(currentLiveItem: LiveChannelModel): LivePlayerFragment {
      return LivePlayerFragment().also {
        it.setSerializableArguments("channelModel", currentLiveItem)
      }
    }

    private const val TAG = "LivePlayerFragment"

    private const val YSP_HOME = "https://www.yangshipin.cn/tv/home"

    private const val USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    private const val JS_INTERFACE_NAME = "AndroidVideo"

  }

  private var _binding: FragmentLivePlayerBinding? = null

  private val binding get() = _binding!!

  private val webView get() = binding.webview

  private var videoJsInterface = VideoJavaScriptInterface()

  private val livePlayerModel = LivePlayerModel()

  private lateinit var channelModel: LiveChannelModel

  inner class VideoJavaScriptInterface {
    /**
     * 视频播放事件
     */
    @JavascriptInterface
    fun onPlay() {

    }

    @JavascriptInterface
    fun onPause() {
    }

    @JavascriptInterface
    fun onKeyDown(key: String, keyCode: Int) {
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    channelModel = arguments?.getSerializableForKey("channelModel") ?: LiveChannelModel()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_live_player, container, false)
    return _binding?.root
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.m = livePlayerModel

    binding.webview.setOnTouchListener { v, event ->
      true
    }

    initWebView(webView)
    binding.webview.loadUrl("$YSP_HOME?pid=${channelModel.pid}")
  }

  override fun play(channel: LiveChannelModel) {
    channelModel = channel
    Log.i(TAG, "play: $channelModel")
    setSerializableArguments("channelModel", channel)

    if (_binding == null) {
      return
    }
    webView.execJs(JsType.PLAY_YSP, "pid" to channel.pid, "vid" to channel.streamId)
  }

  override fun playOrPause() {
    if (_binding == null) {
      return
    }
    webView.execJs(JsType.PLAY_PAUSE)
  }

  /**
   * 创建并配置 WebView
   */
  @SuppressLint("SetJavaScriptEnabled")
  fun initWebView(webView: WebView) {
    webView.apply {
      // 基本配置
      setupWebSettings()
      setupWebChromeClient()
      setupWebViewClient()
      // 添加自定义的接口
      addJavascriptInterface(videoJsInterface, JS_INTERFACE_NAME)
    }
  }

  /**
   * WebSettings 配置
   */
  @SuppressLint("SetJavaScriptEnabled")
  private fun WebView.setupWebSettings() {
    settings.apply {

      isFocusable = false

      userAgentString = USER_AGENT

      // 基本设置
      javaScriptEnabled = true
      domStorageEnabled = true
      databaseEnabled = true
      allowFileAccess = true
      allowContentAccess = true

      // 缓存设置
      cacheMode = WebSettings.LOAD_DEFAULT

      // 布局渲染
      layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
      useWideViewPort = false
      loadWithOverviewMode = false
      builtInZoomControls = false
      displayZoomControls = false
      setSupportZoom(false)

      // 文本渲染
      textZoom = 100
      defaultFontSize = 16
      defaultFixedFontSize = 13
      minimumFontSize = 8
      minimumLogicalFontSize = 8
      // setInitialScale(getMinimumScale())

      // 其他设置
      setSupportMultipleWindows(false)
      javaScriptCanOpenWindowsAutomatically = false
      loadsImagesAutomatically = true // 禁止加载图片
      // blockNetworkImage = true
      mediaPlaybackRequiresUserGesture = false
    }
  }

  /**
   * WebChromeClient 配置
   */
  private fun WebView.setupWebChromeClient() {
    webChromeClient = object : WebChromeClient() {

      override fun onPermissionRequest(request: PermissionRequest?) {
        // 处理权限请求（麦克风、摄像头等）
        request?.grant(request.resources)
      }

      override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
          Log.d("Console", "${it.message()} - ${it.lineNumber()}")
        }
        return true
      }

      override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        livePlayerModel.progress = newProgress
      }
    }
  }


  /**
   * 是否应该加载资源
   *
   * @param url 加载地址
   *
   * @return null 则默认加载，否则指定加载资源
   */
  protected fun shouldInterceptRequest(url: String): WebResourceResponse? {
    if (url.endsWith(".webp")) {
      return createEmptyResponse("image/*")
    }
    return null
  }

  /**
   * 是否拦截跳转
   *
   * @return true 拦截 false 不拦截
   */
  protected fun shouldOverride(url: String): Boolean {
    // 自定义跳转逻辑
    // 例如：拦截特定协议，打开外部应用等

    Log.i(TAG, "shouldOverride: $url")
    return false
  }

  protected fun onPageFinished(url: String) {

  }


  /**
   * WebViewClient 配置
   */
  private fun WebView.setupWebViewClient() {
    webViewClient = object : WebViewClient() {

      @SuppressLint("WebViewClientOnReceivedSslError")
      override fun onReceivedSslError(p0: WebView?, p1: SslErrorHandler?, p2: SslError?) {
        p1?.proceed()
      }

      override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
      ): WebResourceResponse? {

        val url = request?.url?.toString() ?: return createEmptyResponse()

        return shouldInterceptRequest(url) ?: super.shouldInterceptRequest(view, request)
      }

      override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
      ): Boolean {
        // 处理链接跳转
        request?.url?.let { url ->
          val urlString = url.toString()
          // 自定义跳转逻辑
          if (shouldOverride(urlString)) {
            return true
          }
        }
        return super.shouldOverrideUrlLoading(view, request)
      }

      override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        // 页面开始加载
        super.onPageStarted(view, url, favicon)
      }

      override fun onPageFinished(view: WebView, url: String) {
        // 页面加载完成
        super.onPageFinished(view, url)

        onPageFinished(url)
        webView.execJs(JsType.CLEAR_YSP)
        // webView.execJs(JsType.PLAY_YSP, "pid" to channelModel.pid, "vid" to channelModel.streamId)
        webView.execJs(JsType.FULLSCREEN_YSP)
      }
    }
  }


  private val emptyByteArrayStream = ByteArrayInputStream("".toByteArray())

  private fun createEmptyResponse(mimeType: String = "text/plain"): WebResourceResponse {
    // 创建一个空的响应
    return WebResourceResponse(
      mimeType,
      "UTF-8",
      emptyByteArrayStream
    )
  }

  override fun onDestroyView() {
    super.onDestroyView()
    webView.destroy()
    _binding = null
  }

  override fun onDestroy() {
    super.onDestroy()
  }
}