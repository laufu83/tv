package cn.fred.tv.dialog

import android.view.View
import androidx.lifecycle.lifecycleScope
import cn.fred.tv.R
import cn.fred.tv.data.DownloadStatus
import cn.fred.tv.data.VersionInfo
import cn.fred.tv.databinding.DialogUpdateBinding
import cn.fred.tv.AppUtils
import kotlinx.coroutines.launch

class UpdateDialog(private val versionInfo: VersionInfo?) : BaseDialogFragment<DialogUpdateBinding>() {
    override fun initLayoutResource(): Int = R.layout.dialog_update

    override fun onResume() {
        super.onResume()
        isCancelable = false
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            binding.btnConfirm.visibility = View.GONE
            binding.btnCancel.visibility = View.GONE
            binding.btnCancel.isEnabled = false
            binding.btnCancel.text = getString(R.string.confirm)
            binding.progress.visibility = View.VISIBLE
            binding.tvHint.visibility = View.GONE
            context?.let { context ->
                lifecycleScope.launch {
                    context.externalCacheDir?.let { dir ->
                        AppUtils.download(dir, versionInfo).collect { status ->
                            when (status) {
                                is DownloadStatus.DownLoading -> {
                                    binding.progress.progress = status.progress
                                }

                                is DownloadStatus.Error -> {
                                    binding.progress.visibility = View.GONE
                                    binding.tvHint.visibility = View.VISIBLE
                                    binding.btnCancel.visibility = View.VISIBLE
                                    binding.tvHint.text =
                                        getString(R.string.update_fail_hint, "error:${status.error.message}")
                                }

                                is DownloadStatus.Fail -> {
                                    binding.progress.visibility = View.GONE
                                    binding.tvHint.visibility = View.VISIBLE
                                    binding.btnCancel.visibility = View.VISIBLE
                                    binding.tvHint.text =
                                        getString(R.string.update_fail_hint, "fail:${status.code}")
                                }

                                DownloadStatus.Success -> {
                                    dismiss()
                                    AppUtils.installApk(context)
                                }
                            }
                        }
                    }
                    binding.btnCancel.isEnabled = true
                }
            }
        }
    }
}