(function() {
   if (window.livePlayerInstance) {
        // window.livePlayerInstance.keydownFunc({{keyCode}});
        // console.log('keydownFunc: ' + {{keyCode}})
        // console.log('keydownFunc: ' + livePlayerInstance.keydownFunc)
        window.livePlayerInstance.myVideo.playPause()
   } else {
        console.log('window.livePlayerInstance is null');
   }
})();