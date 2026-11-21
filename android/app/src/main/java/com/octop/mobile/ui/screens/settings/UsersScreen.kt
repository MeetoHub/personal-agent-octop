package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.sync.SyncRepository
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import org.json.JSONObject

/**
 * 用户管理（GET /api/users，需 users 权限）。
 *
 * 字段实测：id / username / role / display_name / email / mfa_active / disabled /
 * login_locked / login_failed_count / last_login_at / created_at / permissions[]。
 *
 * 只读。建号、改角色、重置密码、解锁——全是不可逆或高危操作，
 * 手机上点错一个就是别人登不上，一律回网页端。
 */
@Composable
fun UsersScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        state = LoadState.Loading
        val res = SyncRepository.hub.users()
        state = if (!res.ok) LoadState.Failed(reasonOf(res))
        else jsonList(res.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${res.body.take(80)}")
    }

    SubPage(
        title = "用户管理",
        subtitle = "谁能用这个本体",
        onBack = onBack,
        onReload = { tick++ },
        note = "只读。新建用户、改权限、重置密码、解锁都要在网页端做。"
    ) {
        when (val s = state) {
            is LoadState.Loading -> item { LoadingBlock() }
            is LoadState.Failed -> item { ErrorBlock(s.reason) { tick++ } }
            is LoadState.Ready -> {
                item {
                    val admins = s.value.count { it.str("role") == "admin" }
                    SectionTitle("共 ${s.value.size} 个账号", "管理员 $admins")
                }
                if (s.value.isEmpty()) {
                    item { com.octop.mobile.ui.components.EmptyHint("没有账号数据") }
                }
                s.value.forEach { u -> item { UserCard(u) } }
            }
        }
    }
}

@Composable
private fun UserCard(u: JSONObject) {
    val disabled = u.optBoolean("disabled", false)
    val locked = u.optBoolean("login_locked", false)
    val mfa = u.optBoolean("mfa_active", false)
    val role = u.str("role")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = u.str("display_name").ifBlank { u.str("username") },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (disabled) TextTertiary else TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "@" + u.str("username") + (u.str("email").let { if (it.isBlank()) "" else " · $it" }),
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.width(8.dp))
            Tag(
                text = when (role) {
                    "admin" -> "管理员"
                    "user" -> "普通用户"
                    "" -> "未知角色"
                    else -> role
                },
                color = if (role == "admin") Accent else TextTertiary
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(if (disabled) "已禁用" else "正常", if (disabled) Rose else Green)
            Spacer(Modifier.width(6.dp))
            if (locked) { StatusChip("登录锁定", Rose); Spacer(Modifier.width(6.dp)) }
            if (mfa) { StatusChip("MFA", Green); Spacer(Modifier.width(6.dp)) }
            if (u.optBoolean("directory", false)) { StatusChip("目录账号", TextTertiary); Spacer(Modifier.width(6.dp)) }
            Spacer(Modifier.weight(1f))
            Text(
                text = "上次登录 " + fmtTime(u.optLong("last_login_at", 0L)),
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
        val fails = u.optInt("login_failed_count", 0)
        val perms = u.optJSONArray("permissions")
        if (fails > 0 || (perms != null && perms.length() > 0)) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = buildString {
                    if (fails > 0) append("连续失败 $fails 次")
                    if (perms != null && perms.length() > 0) {
                        if (isNotEmpty()) append(" · ")
                        append("额外权限 ")
                        append((0 until perms.length()).joinToString("/") { perms.optString(it) })
                    }
                },
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = TextTertiary
            )
        }
        val quota = u.opt("token_quota")
        val maxAgents = u.opt("max_agents")
        if (quota != null || maxAgents != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOf(
                    if (quota != null && quota.toString() != "null") "Token 配额 $quota" else "",
                    if (maxAgents != null && maxAgents.toString() != "null") "最多 $maxAgents 个专家" else ""
                ).filter { it.isNotBlank() }.joinToString(" · "),
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 9.sp, color = color)
    }
}
