package com.octop.mobile

import android.app.Application
import com.octop.mobile.core.auth.TokenStore
import com.octop.mobile.core.system.BridgeManager
import com.octop.mobile.core.tools.ToolRegistry

class OctopApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
        ToolRegistry.init(this)
        // 手机能力桥：局域网内给 NAS 上的 agent 用
        BridgeManager.start(this)
    }
}
