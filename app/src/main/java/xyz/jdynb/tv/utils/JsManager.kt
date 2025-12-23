package xyz.jdynb.tv.utils

import android.content.Context
import android.util.Log
import android.webkit.WebView
import xyz.jdynb.tv.enums.JsType
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.jvm.Throws

object JsManager {

  private val jsMap = mutableMapOf<String, String>()

  @Throws(IOException::class)
  fun init(context: Context) {
    JsType.entries.forEach { type ->
      context.assets.open("js/${type.liveSource.source}/${type.filename}.js").use {
        jsMap[type.typeName] =
          it.readBytes().toString(StandardCharsets.UTF_8)
      }
    }
  }

  fun getJs(type: JsType) = jsMap[type.typeName]

  fun WebView.execJs(type: JsType, vararg args: Pair<String, Any?>) {
    getJs(type)?.let {

      var result = it
      for ((key, value) in args) {
        result = result.replace("{{${key}}}", value.toString())
      }

      evaluateJavascript(result) { i ->
        Log.i("JsManager", i)
      }
    }
  }
}