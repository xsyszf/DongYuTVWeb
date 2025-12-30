(function() {
    const channelItem = window.channelList.find(item => item.title === '{{channelName}}')
    const playUrl = window.addLiveUrlQuery(channelItem)
    playLive(playUrl)
})();