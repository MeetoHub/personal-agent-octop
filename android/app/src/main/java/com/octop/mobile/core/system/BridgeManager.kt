package com.octop.mobile.core.system

import android.content.Context
import android.util.Log
import com.octop.mobile.core.auth.TokenStore
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 手机能力桥的开关与状态。App 里只有这里能启停 [PhoneBridgeServer]。
 */
object BridgeManager {

    private const val TAG = "BridgeManager"

    private var server: PhoneBridgeServer? = null
    private var lastError: String? = null

    val isRunning: Boolean get() = server?.isAlive == true
    val port: Int get() = PhoneBridgeServer.DEFAULT_PORT
    val error: String? get() = lastError

    fun start(ctx: Context) {
        if (isRunning) return
        if (!TokenStore.bridgeEnabled()) {
            Log.i(TAG, "能力桥已在设置里关闭，跳过启动")
            return
        }
        val app = ctx.applicationContext
        val s = PhoneBridgeServer(app, { TokenStore.bridgeToken() })
        runCatching { s.startServer() }
            .onSuccess {
                server = s
                lastError = null
                Log.i(TAG, "能力桥已启动 ${localUrl()}")
            }
            .onFailure {
                lastError = it.message
                Log.w(TAG, "能力桥启动失败: ${it.message}")
            }
    }

    fun stop() {
        server?.stopServer()
        server = null
    }

    fun restart(ctx: Context) {
        stop()
        start(ctx)
    }

    /** 手机在局域网里的地址——要填到 NAS 那边 MCP 配置里 */
    fun localUrl(): String {
        val ip = localIpv4() ?: return "http://<手机IP>:$port"
        return "http://$ip:$port"
    }

    fun localIpv4(): String? = runCatching {
        val candidates = NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            // 排除 docker/虚拟网桥，不然会挑到 NAS 内部地址，手机根本连不上
            .filter { nif ->
                val n = nif.name.lowercase()
                listOf("docker", "veth", "br-", "virbr", "lo").none { n.startsWith(it) }
            }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .filter { !it.isLinkLocalAddress && !it.isLoopbackAddress }
            .mapNotNull { it.hostAddress }

        // 100.64.0.0/10 是节点小宝 / Tailscale 这类组网工具的段，
        // 注意它不属于 isSiteLocalAddress，所以不能只靠系统判断
        candidates.firstOrNull { it.startsWith("100.") }
            ?: candidates.firstOrNull { it.startsWith("192.168.") }
            ?: candidates.firstOrNull()
    }.getOrNull()

    /** 所有可用地址，设置页列出来方便挑 */
    fun allLocalIpv4(): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .filter { nif ->
                val n = nif.name.lowercase()
                listOf("docker", "veth", "br-", "virbr", "lo").none { n.startsWith(it) }
            }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .filter { !it.isLinkLocalAddress && !it.isLoopbackAddress }
            .mapNotNull { it.hostAddress }
            .distinct()
    }.getOrDefault(emptyList())
}
