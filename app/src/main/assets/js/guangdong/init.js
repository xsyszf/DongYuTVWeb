/**
* 广东卫视
*/
(function() {
    function fullscreen() {
        const video = document.querySelector('#video_html5_api')
        if (video) {
            video.style.left = 0
            video.style.top = 0
            video.style.position = 'fixed'
            video.style['z-index'] = 99999
            const scaleW = screen.width / 580
            const scaleH = screen.height / 326
            const scale = Math.min(scaleW, scaleH)
            video.style.width = `${scale * 580}px`
            video.style.height = `${scale * 326}px`
            return
        }
        setTimeout(() => {
            fullscreen()
        }, 12)
    }

    fullscreen()
})();