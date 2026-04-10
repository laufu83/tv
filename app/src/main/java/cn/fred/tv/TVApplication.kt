package cn.fred.tv

import android.app.Application
import com.tencent.mmkv.MMKV

class TVApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
    }
}