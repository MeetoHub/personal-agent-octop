package com.octop.mobile.core.sync

/**
 * 本体地址归一化。
 *
 * 用户最容易输错的两处：忘记 `http://`、忘记端口。两者都会让请求发到一个
 * 不存在的地址，而界面上只显示"获取数据失败"，排查成本很高。
 * 这里在入口统一补全，登录框里仍按用户习惯显示。
 */
object Endpoints {

    const val DEFAULT_PORT = 8089

    fun normalize(raw: String): String {
        var value = raw.trim().trimEnd('/')
        if (value.isEmpty()) return value
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://$value"
        }
        val rest = value.substringAfter("://")
        // 已经有端口（host:port）或路径带冒号都不动
        if (!rest.contains(':')) {
            value = "$value:$DEFAULT_PORT"
        }
        return value
    }
}
