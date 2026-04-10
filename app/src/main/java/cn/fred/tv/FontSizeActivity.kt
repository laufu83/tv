package cn.fred.tv

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import cn.fred.tv.adapter.FontSizeAdapter
import cn.fred.tv.databinding.ActivityFontSizeBinding
import com.tencent.mmkv.MMKV

class FontSizeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lp = window.attributes
        lp.width = (resources.displayMetrics.widthPixels * 0.5f).toInt()
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.alpha = 0.9f
        window.attributes = lp
        // 设置横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DataBindingUtil.setContentView<ActivityFontSizeBinding>(
            this, R.layout.activity_font_size
        )
        val font = MMKV.defaultMMKV().decodeFloat(Constant.KEY_FONT_SIZE, Constant.FONT_SIZE_NORMAL)
        val index = when (font) {
            Constant.FONT_SIZE_NORMAL -> 0
            Constant.FONT_SIZE_LARGE -> 1
            else -> 2
        }
        val data = resources.getStringArray(R.array.list_font_size).asList()
        val adapter = FontSizeAdapter(this, data)
        binding.lvMenu.adapter = adapter
        binding.lvMenu.setItemChecked(index, true)
        binding.lvMenu.setOnItemClickListener { _, _, position, _ ->
            val fontScale = when (position) {
                0 -> Constant.FONT_SIZE_NORMAL
                1 -> Constant.FONT_SIZE_LARGE
                else -> Constant.FONT_SIZE_HUGE
            }
            if (font != fontScale) {
                MMKV.defaultMMKV().encode(Constant.KEY_FONT_SIZE, fontScale)
                AppUtils.setFontScale(this, fontScale)
                AppUtils.recreateActivity(this)
            }
            finish()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        AppUtils.setFontScale(newBase, 1.0f)
        super.attachBaseContext(newBase)
    }
}