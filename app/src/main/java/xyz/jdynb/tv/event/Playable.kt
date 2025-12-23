package xyz.jdynb.tv.event

import xyz.jdynb.tv.model.YspLiveChannelModel

interface Playable {

  fun play(channel: YspLiveChannelModel)

  fun playOrPause()
}