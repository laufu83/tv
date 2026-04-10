package cn.fred.tv

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.fred.tv.databinding.ActivityMainBinding
import cn.fred.tv.dialog.SettingDialog
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
   private  val TAG : String= "MainActivity"
    lateinit var binding: ActivityMainBinding

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // 隐藏状态栏
        binding = ActivityMainBinding.inflate(layoutInflater) // 创建绑定对象。注意Kotlin中不需要getRoot()，因为binding对象本身就是根视图。
        setContentView(binding.root) // 使用binding的root属性作为setContentView()的参数。或者你也可以直接使用binding对象，因为Kotlin中的合成绑定允许你直接访问布局中的视图。例如：binding.someView。
        val isInit = MMKV.defaultMMKV().decodeBool(Constant.KEY_INIT, true)

        val handler = CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "onCreate: exception:${throwable.message}")
            if (isInit) {
                binding.tvMessage.text = getString(R.string.init_channel_list_fail_network_error)
            } else {
                Toast.makeText(this, R.string.update_channel_list_fail_network_error, Toast.LENGTH_LONG).show()
                startActivity(Intent(this@MainActivity, PlayerActivity::class.java))
                finish()
            }
            binding.progress.hide()
        }
        lifecycleScope.launch(handler){

            if (isInit) {
                binding.tvMessage.setText(R.string.init_channel_list)
            }
            withContext(Dispatchers.IO) {
                AppUtils.getVersion()
            }
            ChannelUtils.parseChannel(this@MainActivity)
            binding.progress.hide()
            if (ChannelUtils.channelSet.isEmpty()) {
                Log.e(TAG, "onCreate: 没有频道列表,退出播放")
                binding.tvMessage.text = getString(R.string.load_channel_list_fail)
            } else {
                if (isInit) {
                    MMKV.defaultMMKV().encode(Constant.KEY_INIT, false)
                }
                Log.e(TAG, "onCreate: channel size:${ChannelUtils.channelSet.size}")
                binding.tvMessage.visibility = View.GONE
                startActivity(Intent(this@MainActivity, PlayerActivity::class.java))
                finish()
            }
        }
        binding.main.setOnClickListener {
            SettingDialog().show(supportFragmentManager, Constant.DIALOG_TAG_SETTING)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            SettingDialog().show(supportFragmentManager, Constant.DIALOG_TAG_SETTING)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }
}

/*
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TvTheme {
        Greeting("Android")
    }
}*/
