package com.octop.mobile.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.octop.mobile.ui.components.PillButton
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.TextTertiary

/**
 * 用网页版登录页登录，成功后把 JWT 抓回来。
 *
 * 为什么不在原生界面里实现登录：Octop 的 /api/auth/login 前面还有验证码、
 * MFA 等一整套校验（滑块 provider 可插拔），在 App 里重写一遍等于把
 * 那套逻辑抄过来，而且以后改了还得跟着改。
 * 网页版已经有完整实现——直接借它，登录成功后从 localStorage 里取 token。
 *
 * 取 token 不依赖具体的存储键名：遍历 localStorage / sessionStorage，
 * 按 JWT 的形状（eyJ 开头的三段 base64url）匹配。
 */
private const val WATCH_JS = """
(function() {
  if (window.__octopWatch) return;
  function findToken() {
    // 网页版把 JWT 存在固定键 "auth_token"（见 getAuthToken/setAuthToken），
    // 精确取它。不要遍历乱猜：localStorage 里还有 octop:wizard-token 等
    // 其它 JWT，抓错会得到一个看似正常实则无权限的令牌。
    try {
      var v = localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token');
      if (v && v.indexOf('eyJ') === 0) return v;
    } catch (e) {}
    // 兜底：只在精确键缺失时才遍历，且必须形如完整 JWT
    var stores = [localStorage, sessionStorage];
    for (var s = 0; s < stores.length; s++) {
      var st = stores[s];
      try {
        for (var i = 0; i < st.length; i++) {
          var k = st.key(i);
          if (k && k.indexOf('token') < 0) continue;
          var val = st.getItem(k);
          if (!val) continue;
          var m = val.match(/^eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}$/);
          if (m) return m[0];
        }
      } catch (e) {}
    }
    return null;
  }
  window.__octopWatch = setInterval(function() {
    var t = findToken();
    if (!t) return;
    clearInterval(window.__octopWatch);
    window.__octopWatch = null;
    try { window.OctopBridge.onTokenFound(t); } catch (e) {}
  }, 700);
})();
"""

@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun WebLoginScreen(
    baseUrl: String,
    onToken: (String) -> Unit,
    onCancel: () -> Unit
) {
    val url = remember(baseUrl) { baseUrl.trimEnd('/') + "/login" }
    val tokenSink by rememberUpdatedState(onToken)

    Box(Modifier.fillMaxSize().background(Bg)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, u: String?) {
                            super.onPageFinished(view, u)
                            // 每次页面加载完就挂上监听；SPA 不刷新页面也能捕捉到
                            view?.evaluateJavascript(WATCH_JS, null)
                        }
                    }
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onTokenFound(token: String) {
                            if (token.isNotBlank()) post { tokenSink(token) }
                        }
                    }, "OctopBridge")
                    loadUrl(url)
                }
            }
        )

        // 顶部提示条：告诉用户这是网页版登录，以及怎么退出
        Column(Modifier.fillMaxWidth().background(Surface1).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "网页版登录",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                PillButton(text = "返回", filled = false) { onCancel() }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "在这里登录成功后会自动回到 App。验证码、MFA 都按网页版走。",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }
}
