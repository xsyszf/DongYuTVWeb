(async function () {
    let live = window.livePlayerInstance
    let flag = true

    function findVideoInstance(vueInstance) {
        if (!vueInstance || typeof vueInstance !== 'object') {
            console.warn('无效的 Vue 实例');
            return null;
        }

        // 深度优先遍历函数
        function dfs(instance) {
            // 检查当前实例是否包含 livePlayer 属性
            if (instance && instance.getLiveUrlsByVid !== undefined) {
                console.log('找到包含 livePlayer 属性的实例:', instance);
                return instance;
            }

            // 如果当前实例有 $children，递归遍历
            if (instance.$children && Array.isArray(instance.$children)) {
                for (let i = 0; i < instance.$children.length; i++) {
                    const child = instance.$children[i];
                    const found = dfs(child);
                    if (found) {
                        return found;
                    }
                }
            }

            return null;
        }

        return dfs(vueInstance);
    }

    // 从 window.app.__vue__ 开始查找
    function findVideoInstanceFromApp() {
        // 检查 window.app.__vue__ 是否存在
        if (!window.app || !window.app.__vue__) {
            console.warn('window.app.__vue__ 不存在');
            return null;
        }

        const rootInstance = window.app.__vue__;
        return findVideoInstance(rootInstance);
    }

    console.log('Vue: ' + window.Vue + ", " + window.app.__vue__)

    if (!live) {
        setTimeout(function() {
            flag = false
        }, 5000)
        console.log('==========获取 LivePlayer 组件实例=============')

        while (!live && flag) {
            live = findVideoInstanceFromApp()
            await new Promise(resolve => setTimeout(resolve, 1));
        }
        window.livePlayerInstance = live
    }

    console.log('live: ' + live)

    if (live) {
        live.videoConfig.pid = '{{pid}}'
        live.videoConfig.vid = '{{vid}}'
        console.log('videoConfig: pid=' + live.videoConfig.pid + "vid=" + live.videoConfig.vid)
    }
})();
