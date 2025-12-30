package xyz.jdynb.tv.fragment

import android.webkit.WebResourceResponse

class SimpleLivePlayerFragment : LivePlayerFragment() {

  companion object {

    private const val TAG = "SimpleLivePlayerFragment"

  }

  override fun shouldInterceptRequest(url: String): WebResourceResponse? {
    val shouldIntercept = super.shouldInterceptRequest(url)
    if (url.endsWith("dy-crypto-js.min.js")) {
      // 注入 CRYPTO.JS
      return createCryptoJsResponse()
    }
    return shouldIntercept
  }

  override fun onLoadUrl(url: String?) {
    webView.loadUrl("file:///android_asset/html/simple_player.html")
  }

  override fun resumeOrPause() {
    webView.evaluateJavascript("resumeOrPause()", null)
  }
}