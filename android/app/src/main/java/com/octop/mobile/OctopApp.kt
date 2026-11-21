package com.octop.mobile

import android.app.Application
import com.octop.mobile.core.auth.TokenStore
import com.octop.mobile.core.system.PhoneLink
import com.octop.mobile.core.tools.ToolRegistry

class OctopApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
        ToolRegistry.init(this)
        // 反向通道（A 方案）：手机主动连 NAS，等它下发工具调用。
        // 局域网 HTTP 手机桥（B 方案）保留代码但不再自动启动，入口在「我的 → 手机桥」。
        PhoneLink.start(this)
    }
}
