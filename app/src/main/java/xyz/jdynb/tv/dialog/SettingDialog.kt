package xyz.jdynb.tv.dialog

import android.content.Context
import android.os.Bundle
import com.drake.engine.base.EngineDialog
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.music.utils.SpUtils.put
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogSettingBinding

class SettingDialog(context: Context) : EngineDialog<DialogSettingBinding>(context, R.style.Theme_BaseDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_setting)
    }

    override fun initView() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.btnBack.requestFocus()

        binding.swReverseDirection.isChecked = SPKeyConstants.REVERSE_DIRECTION.getRequired(false)
        binding.swBoot.isChecked = SPKeyConstants.BOOT_AUTO_START.getRequired(true)

        binding.swBoot.setOnCheckedChangeListener { buttonView, isChecked ->
            SPKeyConstants.BOOT_AUTO_START.put(isChecked)
        }

        binding.swReverseDirection.setOnCheckedChangeListener { buttonView, isChecked ->
            SPKeyConstants.REVERSE_DIRECTION.put(isChecked)
        }
    }

    override fun initData() {

    }
}