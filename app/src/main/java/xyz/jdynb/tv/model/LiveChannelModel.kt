package xyz.jdynb.tv.model


import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.jdynb.tv.BR

@Serializable
data class LiveChannelModel(
    @SerialName("channelName")
    var channelName: String = "CCTV1",
    @SerialName("pid")
    var pid: String = "600001859",
    @SerialName("tvLogo")
    var tvLogo: String = "https://resources.yangshipin.cn/assets/oms/image/202306/d57905b93540bd15f0c48230dbbbff7ee0d645ff539e38866e2d15c8b9f7dfcd.png?imageMogr2/format/webp",
    @SerialName("streamId")
    var streamId: String = "2024078201",
    @SerialName("channelType")
    var channelType: String = "",
    @SerialName("selectTvLogo")
    var selectTvLogo: String = "",
    @SerialName("coverUrl")
    var coverUrl: String = "",
): BaseObservable() {

    /**
     * 直播序号（对应键盘输入）
     */
    var num = 0

    @Bindable
    var isSelected: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.isSelected)
        }

}