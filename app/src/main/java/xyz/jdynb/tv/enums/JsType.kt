package xyz.jdynb.tv.enums

enum class JsType(val filename: String, val liveSource: LiveSource) {
  /*VIDEO("video", LiveSource.COMMON),
  PLAY("play", LiveSource.COMMON),
  PAUSE("pause", LiveSource.COMMON),
  FULLSCREEN_CCTV("auto_fullscreen", LiveSource.CCTV),
  CLEAR_CCTV("clear", LiveSource.CCTV),*/

  FULLSCREEN_YSP("auto_fullscreen", LiveSource.YSP),

  CLEAR_YSP("clear", LiveSource.YSP),

  PLAY_YSP("play", LiveSource.YSP),

  PLAY_PAUSE("play_pause", LiveSource.YSP)

  ;

  val typeName = filename + liveSource.source
}