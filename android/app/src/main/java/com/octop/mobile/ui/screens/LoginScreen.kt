package com.octop.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.ui.theme.BgElevated
import com.octop.mobile.ui.theme.BgLayout
import com.octop.mobile.ui.theme.BorderInput
import com.octop.mobile.ui.theme.BorderPrimary
import com.octop.mobile.ui.theme.Brand
import com.octop.mobile.ui.theme.Danger
import com.octop.mobile.ui.theme.Info
import com.octop.mobile.ui.theme.SurfaceSunken
import com.octop.mobile.ui.theme.TextInverse
import com.octop.mobile.ui.theme.TextPlaceholder
import com.octop.mobile.ui.theme.TextPrimaryColor
import com.octop.mobile.ui.theme.TextSecondaryColor
import com.octop.mobile.ui.theme.TextTertiaryColor
import com.octop.mobile.ui.theme.Warning

/**
 * 登录页。业务逻辑（表单状态 / VM 回调 / 错误提示）保持原样，只按本体视觉重排。
 *
 * 本体对照：dashboard/src/pages/Login/index.tsx
 *  - 外层居中容器（minHeight 100dvh, background var(--fn-bg-layout)）：index.tsx:729-741
 *  - 卡片 cardStyle（maxWidth 360 / radius 16 / bg-elevated / border-primary / padding 48 32 40 / gap 20）：index.tsx:533-546
 *  - logo 横版（logo_horizontal_* .png, height 48, maxWidth 260）：index.tsx:548-564
 *  - 标题 h2（20px / 600 / 居中）：index.tsx:745-755
 *  - 用户名/密码输入框（User/Lock 16px 前缀，placeholder 来自 i18n，radius 10）：index.tsx:757-783
 *  - 记住/忘记行 + 主按钮：index.tsx:813-867
 * 手机端额外保留：本体地址、网页登录、hint/error（本体无对应控件，避免删功能）。
 */
@Composable
fun LoginScreen(
    initialEndpoint: String,
    initialUsername: String,
    busy: Boolean,
    error: String?,
    hint: String?,
    onLogin: (endpoint: String, username: String, password: String) -> Unit,
    onWebLogin: (endpoint: String) -> Unit
) {
    var endpoint by remember { mutableStateOf(initialEndpoint) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var rememberLogin by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLayout)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // cardStyle：maxWidth 360 / padding 48 32 40 / radius 16 / bg-elevated / border-primary / gap 20
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgElevated, RoundedCornerShape(16.dp))
                    .border(1.dp, BorderPrimary, RoundedCornerShape(16.dp))
                    .padding(start = 24.dp, top = 40.dp, end = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // logo：本体横版 logo 图片 height 48 / maxWidth 260。手机端无该资源，
                // 用包内已有 logo 前景图（若工程未提供则退化为文字标题，见下）。
                LoginLogo()

                Text(
                    text = "登录",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextPrimaryColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // 用户名：User 16px 前缀（本体 index.tsx:757-769）
                InputBox(prefix = LoginIcon.USER) {
                    BasicTextField(
                        value = username,
                        onValueChange = { username = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = TextPrimaryColor),
                        cursorBrush = SolidColor(Brand),
                        decorationBox = { inner ->
                            if (username.isEmpty()) {
                                Text(
                                    text = "用户名或邮箱",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPlaceholder
                                )
                            }
                            inner()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    )
                }

                // 密码：Lock 16px 前缀（本体 index.tsx:771-783）
                InputBox(prefix = LoginIcon.LOCK) {
                    BasicTextField(
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(fontSize = 15.sp, color = TextPrimaryColor),
                        cursorBrush = SolidColor(Brand),
                        decorationBox = { inner ->
                            if (password.isEmpty()) {
                                Text(
                                    text = "密码",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPlaceholder
                                )
                            }
                            inner()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    )
                }

                // 本体地址（手机端专有，本体无此控件）：紧凑地放在密码框下
                InputBox(prefix = null) {
                    BasicTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = TextPrimaryColor),
                        cursorBrush = SolidColor(Brand),
                        decorationBox = { inner ->
                            if (endpoint.isEmpty()) {
                                Text(
                                    text = "本体地址（如 192.168.1.10:8089）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPlaceholder
                                )
                            }
                            inner()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    )
                }

                if (error != null) {
                    LoginAlertBox(kind = LoginAlert.ERROR, text = error)
                }
                if (hint != null) {
                    LoginAlertBox(kind = LoginAlert.INFO, text = hint)
                }

                // 记住 / 忘记行（本体 index.tsx:813-851）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.clickable { rememberLogin = !rememberLogin },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (rememberLogin) Brand else SurfaceSunken,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    if (rememberLogin) Brand else BorderInput,
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rememberLogin) {
                                Text(
                                    text = "✓",
                                    color = TextInverse,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "记住登录状态",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiaryColor
                        )
                    }
                    Text(
                        text = "忘记密码？",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiaryColor
                    )
                }

                // 主按钮（本体 index.tsx:857-867：type primary / large / block / radius 10 / height 44）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            if (busy) Brand.copy(alpha = 0.5f) else Brand,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = !busy) { onLogin(endpoint, username, password) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (busy) "登录中…" else "登录",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextInverse,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 网页登录（手机端专有，保留功能）：本体无对应控件
                Text(
                    text = "在网页上登录 →",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryColor,
                    modifier = Modifier.clickable { onWebLogin(endpoint) }
                )
            }
        }
    }
}

/* ───────── 本体视觉元素复刻（仅本文件内私有） ───────── */

/**
 * 本体 logo 是横版 PNG（logo_horizontal_dark/white.png），手机工程未提供同名资源。
 * 这里用包内已有 ic_launcher_foreground 作为图形标记，尺寸贴近本体 height 48。
 */
@Composable
private fun LoginLogo() {
    Image(
        painter = painterResource(com.octop.mobile.R.drawable.ic_launcher_foreground),
        contentDescription = "Octop",
        modifier = Modifier.size(48.dp)
    )
}

private enum class LoginIcon { USER, LOCK }

/** 本体输入框前缀 lucide User/Lock（16px，stroke 1.8），线宽比例 1.8/16 = 0.1125。 */
@Composable
private fun InputBox(
    prefix: LoginIcon?,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(SurfaceSunken, RoundedCornerShape(10.dp))
            .border(1.dp, BorderInput, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix != null) {
            // composable getter 必须在 Canvas 的 draw lambda 之外取值。
            val iconColor = TextTertiaryColor
            val d = when (prefix) {
                // lucide user: M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2；circle cx=12 cy=7 r=4
                LoginIcon.USER -> "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2 M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8"
                // lucide lock: rectangle x=5 y=11 w=14 h=11 rx=2；M16 11V7a4 4 0 0 0-8 0v4
                LoginIcon.LOCK -> "M7 11h10a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z M16 11V7a4 4 0 0 0-8 0v4"
            }
            val path = androidx.compose.ui.graphics.vector.PathParser()
                .parsePathString(d)
                .toPath()
            androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                val s = size.minDimension
                val st = Stroke(width = s * 0.1125f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                // path 是 24×24 viewBox 坐标，用 DrawScope.scale 缩到实际尺寸。
                scale(s / 24f, s / 24f) {
                    drawPath(path, iconColor, style = st)
                }
            }
            Spacer(Modifier.width(12.dp))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            content()
        }
    }
}

private enum class LoginAlert { ERROR, INFO, WARN }

@Composable
private fun LoginAlertBox(kind: LoginAlert, text: String) {
    val color = when (kind) {
        LoginAlert.ERROR -> Danger
        LoginAlert.INFO -> Info
        LoginAlert.WARN -> Warning
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}
