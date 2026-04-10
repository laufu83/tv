package cn.fred.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tencent.mmkv.MMKV

class TVBootReceive : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e("TVBootReceive", "onReceive: 开机")
        intent?.run {
            if (action == Intent.ACTION_BOOT_COMPLETED) {
                if (MMKV.defaultMMKV().decodeBool(Constant.KEY_BOOT_STARTUP, false)) {
                    context?.run {
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                }
            }
        }
    }
}