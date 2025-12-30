(function() {
    window.addLiveUrlQuery = function (obj) {
       let ts = parseInt(new Date().getTime() / (1000)) + (7200);
       return obj.liveVideo[0].formats[0].url + "?t=" + ts + '&k=' + CryptoJS.MD5(obj.appCustomParams.movie.liveUri + obj.appCustomParams.movie.liveKey + ts);;
    }

    fetch("https://api.cmc.hebtv.com/scms/api/com/article/getArticleList?catalogId=32557&siteId=1", {
      "headers": {
        "accept": "application/json, text/javascript, */*; q=0.01",
        "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        "sec-ch-ua": "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"Windows\"",
        "sec-fetch-dest": "empty",
        "sec-fetch-mode": "cors",
        "sec-fetch-site": "same-site",
        "Referer": "https://www.hebtv.com/"
      },
      "body": null,
      "method": "GET"
    }).then(res => {
      return res.json()
    }).then(res => {
        const news = res.returnData.news
        window.channelList = news.map(item => {
            return {
                title: item.title,
                liveVideo: item.liveVideo,
                appCustomParams: item.appCustomParams
            }
        })

        const channelItem = window.channelList.find(item => item.title === '{{channelName}}')
        const playUrl = window.addLiveUrlQuery(channelItem)
        playLive(playUrl)
    })
})();