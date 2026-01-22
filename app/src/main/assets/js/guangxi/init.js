/**
* 广西卫视
*/
(function() {
    function fullscreen() {
        const videoDiv = document.querySelector('#dhc-video')
        if (videoDiv) {
            videoDiv.style.position = "fixed"
            videoDiv.style.top = "0"
            videoDiv.style.left = "0"
            video.style['z-index'] = 99999
            const scaleW = screen.width / 940
            const scaleH = screen.height / 570
            const scale = Math.min(scaleW, scaleH)
            videoDiv.style.width = `${scale * 940}px`
            videoDiv.style.height = `${scale * 570}px`
            return
        }

        setTimeout(() => {
            fullscreen()
        }, 12)
    }

    fullscreen()

    document.querySelector('.Gxntv_nav').style.display = 'none'

})();