package xyz.jdynb.tv.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Runnable
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.MainActivity
import xyz.jdynb.tv.databinding.FragmentPlaybackBinding
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.model.LiveItem
import xyz.jdynb.tv.utils.JsManager.execJs
import xyz.jdynb.tv.utils.toBundle
import xyz.jdynb.tv.utils.toObj
import java.io.ByteArrayInputStream
import kotlin.math.roundToInt

abstract class VideoFragment: Fragment() {

  companion object {

    private const val TAG = "VideoFragment"

    /**
     * windows UA
     */
    private const val USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    private const val JS_INTERFACE_NAME = "AndroidVideo"

    private const val MAX_PAUSE_TIME = 20000L

    inline fun <reified T: VideoFragment> newInstance(liveItem: LiveItem): VideoFragment {
      val fragment = T::class.java.getDeclaredConstructor().newInstance()
      fragment.arguments = liveItem.toBundle()
      return fragment
    }
  }

  /**
   * 视频是否需要销毁
   *
   * 标记状态
   */
  private var isDestroy = true

  /**
   * 页面是否加载完成了
   */
  private var isPageLoadFinished = false

  /**
   * 视频是否正在播放
   */
  private var isVideoPlaying = false

  private var _binding: FragmentPlaybackBinding? = null
  protected val binding get() = _binding!!

  protected val webView get() = binding.webview

  protected lateinit var liveUrl: String

  private lateinit var liveItem: LiveItem

  protected val mainActivity get() = (requireActivity() as MainActivity)

  private val handler = Handler(Looper.getMainLooper())

  private val pauseRunnable = Runnable {
    // 标记为销毁状态
    isDestroy = true
  }

  /**
   * 禁止加载的文件
   */
  abstract val blockFiles: List<String>

  private var videoJsInterface = VideoJavaScriptInterface()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentPlaybackBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    initWebView(binding.webview)
    Log.i(TAG, "onViewCreated: ${binding.webview.url}")
  }

  inner class VideoJavaScriptInterface {
    /**
     * 视频播放事件
     */
    @JavascriptInterface
    fun onPlay() {
      isVideoPlaying = true
      Log.i(TAG, "isVideoPlaying = true")
    }

    @JavascriptInterface
    fun onPause() {
      isVideoPlaying = false
      Log.i(TAG, "isVideoPlaying = false")
    }

    @JavascriptInterface
    fun onKeyDown(key: String, keyCode: Int) {
      Log.i(TAG, "onKeyDown: $keyCode, $key")
    }
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
      // 性能优化
      setupPerformance()
      // 调试支持
      enableDebugging()
      // 添加自定义的接口
      addJavascriptInterface(videoJsInterface, JS_INTERFACE_NAME)

      // CookieManager.getInstance().setAcceptCookie(false)
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
      setInitialScale(getMinimumScale())

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
    }
  }

  private val emptyByteArrayStream = ByteArrayInputStream("".toByteArray())

  protected fun createEmptyResponse(mimeType: String = "text/plain"): WebResourceResponse {
    // 创建一个空的响应
    return WebResourceResponse(
      mimeType,
      "UTF-8",
      emptyByteArrayStream
    )
  }

  /**
   * 是否应该加载资源
   *
   * @param url 加载地址
   *
   * @return null 则默认加载，否则指定加载资源
   */
  protected open fun shouldInterceptRequest(url: String): WebResourceResponse? {
    if (url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".webp")) {
      return createEmptyResponse("image/*")
    }

    if (blockFiles.any { url.endsWith(it) }) {
      Log.i(TAG, "拦截: $url")
      return createEmptyResponse()
    }

    return null
  }

  /**
   * 当页面加载时
   */
  open fun onPageStarted(url: String) {
    isPageLoadFinished = false
    binding.loading.isVisible = true
  }

  /**
   * 当页面加载完成时
   */
  open fun onPageFinished(url: String) {
    isPageLoadFinished = true
    // webView.execJs(JsType.VIDEO)
    binding.loading.isVisible = false
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
        onPageStarted(url)
      }

      override fun onPageFinished(view: WebView, url: String) {
        // 页面加载完成
        super.onPageFinished(view, url)
        onPageFinished(url)
      }
    }
  }

  /**
   * 是否拦截跳转
   *
   * @return true 拦截 false 不拦截
   */
  protected open fun shouldOverride(url: String): Boolean {
    // 自定义跳转逻辑
    // 例如：拦截特定协议，打开外部应用等

    Log.i(TAG, "shouldOverride: $url")

    if (!url.startsWith(liveUrl)) {
      Log.i(TAG, "拦截: $url")
      return true
    }

    return false
  }

  /**
   * 性能优化
   */
  private fun WebView.setupPerformance() {
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    settings.setRenderPriority(WebSettings.RenderPriority.NORMAL)
  }

  /**
   * 启用调试
   */
  private fun enableDebugging() {
    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
  }

  private fun getMinimumScale(): Int {
    val displayMetrics = DisplayMetrics()
    requireActivity().windowManager.getDefaultDisplay().getMetrics(displayMetrics)
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    // 计算缩放比例，使用 double 类型进行计算
    val scale =
      (screenWidth.toDouble() / 1920.0).coerceAtMost(screenHeight.toDouble() / 1080.0) * 100
    // 四舍五入并转为整数
    return scale.roundToInt()
  }

  fun play() {
    if (isDestroy) {
      isDestroy = false
      Log.i(TAG, "onResume: startLoading: $liveUrl ")
      if (webView.url == liveUrl) {
        webView.reload()
      } else {
        webView.clearHistory()
        webView.loadUrl(liveUrl)
      }
    } else {
      Log.i(TAG, "onResume: play: ${binding.webview.url}")
      // 视频已经准备好了
      // webView.execJs(JsType.PLAY)
    }
    handler.removeCallbacks(pauseRunnable)
  }

  override fun onResume() {
    super.onResume()
    play()
  }

  fun pauseOrPlay() {
    if (isVideoPlaying) {
      Log.i(TAG, "pause")
      pause()
    } else {
      Log.i(TAG, "play")
      play()
    }
  }

  fun pause(destroy: Boolean = false) {
    // 如果离开时视频还没进行播放，则加载空白页面
    if (!isVideoPlaying) {
      // 如果视频未播放，则执行销毁事件
      isDestroy = true
      webView.loadUrl("about:blank")
      Log.i(TAG, "onPause: loadUrl about:blank, url: ${binding.webview.url}")
    } else {
      // 视频已经准备好了，则进行暂停
      // webView.execJs(JsType.PAUSE)
      // webView.loadUrl("about:blank")
      Log.i(TAG, "onPause: pause: ${binding.webview.url}")
    }

    if (destroy) {
      handler.removeCallbacks(pauseRunnable)
      handler.postDelayed(pauseRunnable, MAX_PAUSE_TIME)
    }
  }

  override fun onPause() {
    super.onPause()
    pause(true)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "onCreate")
    liveItem = requireArguments().toObj<LiveItem>() ?: return
    liveUrl = liveItem.url
  }

  override fun onDestroy() {
    super.onDestroy()
    isDestroy = true
    Log.d(TAG, "onDestroy")
  }

  override fun onDestroyView() {
    super.onDestroyView()
    Log.i(TAG, "onDestroyView")
    isDestroy = true
    binding.webview.destroy()
    binding.webview.removeAllViews()
    _binding = null
  }
}