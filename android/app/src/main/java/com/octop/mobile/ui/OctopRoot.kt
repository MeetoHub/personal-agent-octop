package com.octop.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.octop.mobile.core.model.TaskStatus
import com.octop.mobile.ui.components.Glyph
import com.octop.mobile.ui.components.TabIcon
import com.octop.mobile.ui.screens.ChatScreen
import com.octop.mobile.ui.screens.ExpertDetailScreen
import com.octop.mobile.ui.screens.ExpertsScreen
import com.octop.mobile.ui.screens.MemoryScreen
import com.octop.mobile.ui.screens.LoginScreen
import com.octop.mobile.ui.screens.ScheduleScreen
import com.octop.mobile.ui.screens.SettingsScreen
import com.octop.mobile.ui.screens.TaskDetailScreen
import com.octop.mobile.ui.screens.TasksScreen
import com.octop.mobile.ui.screens.WebLoginScreen
import com.octop.mobile.ui.screens.settings.AppSettingsScreen
import com.octop.mobile.ui.screens.settings.ChannelsScreen
import com.octop.mobile.ui.screens.settings.CloudScreen
import com.octop.mobile.ui.screens.settings.ConnectorsScreen
import com.octop.mobile.ui.screens.settings.KnowledgeBasesScreen
import com.octop.mobile.ui.screens.settings.ModelsScreen
import com.octop.mobile.ui.screens.settings.PluginsScreen
import com.octop.mobile.ui.screens.settings.SecurityScreen
import com.octop.mobile.ui.screens.settings.SkillsScreen
import com.octop.mobile.ui.screens.settings.StorageScreen
import com.octop.mobile.ui.screens.settings.UsageScreen
import com.octop.mobile.ui.screens.settings.UsersScreen
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.BgPrimary
import com.octop.mobile.ui.theme.Danger
import com.octop.mobile.ui.theme.SidebarItemActiveBg
import com.octop.mobile.ui.theme.SidebarItemActiveText
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import com.octop.mobile.ui.theme.ThemeState

/** 本体 viewport.ts 的 KEYBOARD_GAP_THRESHOLD_PX：比这个小的一律当手势条残留。 */
private const val KEYBOARD_GAP_THRESHOLD_PX = 80

private enum class Tab(val route: String, val label: String, val glyph: Glyph) {
    CHAT("threads", "对话", Glyph.CHAT),
    EXPERTS("experts", "专家", Glyph.PERSON),
    MEMORY("memory", "记忆", Glyph.STAR),
    SCHEDULE("schedule", "定时", Glyph.CLOCK),
    SETTINGS("settings", "我的", Glyph.MENU)
}

/** 起始 tab：返回键在其它 tab 上要回到这里。 */
private val START_ROUTE = Tab.CHAT.route

@Composable
fun OctopRoot(vm: AppViewModel = viewModel(), themeState: ThemeState) {
    val auth by vm.auth.collectAsState()
    LaunchedEffect(Unit) { vm.restoreSession() }

    when (auth) {
        AppViewModel.AuthState.CHECKING -> Splash()

        AppViewModel.AuthState.LOGGED_OUT -> {
            val busy by vm.loginBusy.collectAsState()
            val err by vm.loginError.collectAsState()
            val hint by vm.loginHint.collectAsState()
            var webLogin by remember { mutableStateOf(false) }
            var webEndpoint by remember { mutableStateOf(vm.savedEndpoint) }

            // 登录分支不在 Scaffold 里，安全区得自己垫（否则输入框会压到状态栏/手势条）
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                if (webLogin) {
                    WebLoginScreen(
                        baseUrl = webEndpoint,
                        onToken = { token ->
                            webLogin = false
                            vm.loginWithToken(webEndpoint, token)
                        },
                        onCancel = { webLogin = false }
                    )
                } else {
                    LoginScreen(
                        initialEndpoint = vm.savedEndpoint,
                        initialUsername = vm.savedUsername,
                        busy = busy,
                        error = err,
                        hint = hint,
                        onLogin = { ep, u, p -> vm.login(ep, u, p) },
                        onWebLogin = { ep ->
                            webEndpoint = ep
                            webLogin = true
                        }
                    )
                }
            }
        }

        AppViewModel.AuthState.LOGGED_IN -> MainScaffold(vm, themeState)
    }
}

@Composable
private fun Splash() {
    Box(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Octop", style = MaterialTheme.typography.titleLarge, color = TextTertiary)
    }
}

@Composable
private fun MainScaffold(vm: AppViewModel, themeState: ThemeState) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    // 二级页（「我的」里的功能入口）也占满屏：它们是从 tab 里点进去的下一层，
    // 底下还挂着 tab 会让人觉得「这是平级页面」。返回靠页内左上角的箭头。
    val fullscreen = route?.startsWith("task/") == true ||
        route?.startsWith("expert/") == true ||
        route?.startsWith("settings/") == true ||
        route == "chat"

    val tasks by vm.tasks.collectAsState()
    val pending = tasks.count { it.status == TaskStatus.WAITING_APPROVAL }

    // ── 返回键对齐本体移动端语义（MainLayout/index.tsx）──────────────
    // 本体没有底部 tab，我们的五个 tab 是等价替换物：tab 之间平级，
    // 所以在非起始 tab 上按返回 = 回起始 tab；settings 子页 = 回「我的」。
    // 其余情况不接管（全屏详情页走默认 popBackStack，起始 tab 走系统退出）。
    val onTab = Tab.entries.any { it.route == route }
    val inSettingsChild = route?.startsWith("settings/") == true
    BackHandler(enabled = (onTab && route != START_ROUTE) || inSettingsChild) {
        if (inSettingsChild) {
            // 正常情况下弹一层就是 settings；异常栈（直接深链）则直跳「我的」
            if (!nav.popBackStack()) {
                nav.navigate(Tab.SETTINGS.route) {
                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        } else {
            nav.navigate(START_ROUTE) {
                popUpTo(nav.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // 键盘：本体 measureKeyboardOffset() 只在 inset > 80px 时才认作真键盘，
    // 否则那是手势条残留，再垫一次就成了双倍留白。
    val density = LocalDensity.current
    val imeShown = WindowInsets.ime.getBottom(density) > KEYBOARD_GAP_THRESHOLD_PX

    // 键盘 inset 只能有一个 owner，否则双份留白：键盘弹出时内容独占 ime（并被底栏让位），
    // 键盘收起时把 ime 剔掉只留系统栏/挖孔。80px 以下一律当手势条残留剔掉，
    // 免得键盘动画期间几十像素 inset 让整页抖一下（方案 §4.8 / viewport.ts）。
    val contentInsets = if (imeShown) {
        WindowInsets.safeDrawing
    } else {
        WindowInsets.safeDrawing.exclude(WindowInsets.ime)
    }

    Scaffold(
        containerColor = Bg,
        // 系统栏/挖孔安全区由 Scaffold 消费（bottomBar 自己 navigationBarsPadding）
        contentWindowInsets = contentInsets,
        bottomBar = {
            // 键盘弹出时不画底部 tab：它本来就会被键盘盖住，留着只会露出半截
            if (!fullscreen && !imeShown) {
                BottomBar(nav = nav, current = route, pending = pending)
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = START_ROUTE,
            // fillMaxSize 必不可少：Scaffold 的 content 只保证 inset，不给尺寸约束。
            // 少了它，下面带 LazyColumn 的页面（首页=聊天页）会拿到无限高约束而崩溃。
            modifier = Modifier.fillMaxSize().padding(pad)
        ) {
            composable(Tab.CHAT.route) {
                ChatScreen(
                    vm = vm,
                    onBack = { },
                    onTapAgent = { nav.navigate(Tab.EXPERTS.route) { launchSingleTop = true } },
                    showBack = false
                )
            }
            composable("tasks") {
                TasksScreen(
                    vm = vm,
                    onOpen = { nav.navigate("task/$it") },
                    onOpenChat = { nav.navigate(Tab.CHAT.route) }
                )
            }
            composable("chat") {
                ChatScreen(vm = vm, onBack = { nav.popBackStack() })
            }
            composable(Tab.EXPERTS.route) {
                ExpertsScreen(vm = vm, onOpen = { nav.navigate("expert/$it") })
            }
            composable("expert/{agentId}") { backEntry ->
                val id = backEntry.arguments?.getString("agentId").orEmpty()
                ExpertDetailScreen(vm = vm, agentId = id, onBack = { nav.popBackStack() })
            }
            composable(Tab.MEMORY.route) { MemoryScreen(vm) }
            composable(Tab.SCHEDULE.route) { ScheduleScreen(vm) }
            composable(Tab.SETTINGS.route) {
                SettingsScreen(vm = vm, onOpen = { nav.navigate(it) })
            }

            // ── 「我的」功能中心的二级页 ──────────────────────
            // 只有一个入口页（settings），下面是各功能页。底部 tab 仍是原来 5 个。
            composable("settings/knowledge") {
                KnowledgeBasesScreen(onBack = { nav.popBackStack() })
            }
            composable("settings/skills") {
                SkillsScreen(agentId = vm.activeAgentId, onBack = { nav.popBackStack() })
            }
            composable("settings/channels") {
                ChannelsScreen(agentId = vm.activeAgentId, onBack = { nav.popBackStack() })
            }
            composable("settings/connectors") {
                ConnectorsScreen(onBack = { nav.popBackStack() })
            }
            composable("settings/usage") {
                UsageScreen(onBack = { nav.popBackStack() })
            }
            composable("settings/models") {
                ModelsScreen(onBack = { nav.popBackStack() })
            }
            composable("settings/plugins") {
                PluginsScreen(onBack = { nav.popBackStack() })
            }
            composable("settings/security") {
                SecurityScreen(onBack = { nav.popBackStack() })
            }
            composable("settings/app") {
                AppSettingsScreen(onBack = { nav.popBackStack() }, themeState = themeState)
            }
            composable("settings/users") {
                UsersScreen(onBack = { nav.popBackStack() })
            }
            composable("settings/storage") {
                StorageScreen(onBack = { nav.popBackStack() })
            }
            composable("settings/cloud") {
                CloudScreen(onBack = { nav.popBackStack() })
            }
            composable("task/{taskId}") { backEntry ->
                val id = backEntry.arguments?.getString("taskId").orEmpty()
                TaskDetailScreen(vm = vm, taskId = id, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, current: String?, pending: Int) {
    // 暗色判定用背景亮度：本文件不依赖主题内核额外暴露开关
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // 阴影：暗色底要更重的黑才看得出浮起（theme-vars.css 暗色 shadow 25%~40%）
    val ambient = Color.Black.copy(alpha = if (dark) 0.35f else 0.05f)
    val spot = Color.Black.copy(alpha = if (dark) 0.45f else 0.10f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 底部 tab 让开手势条（env(safe-area-inset-bottom) 等价物）
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 7.dp,
                    shape = RoundedCornerShape(40.dp),
                    ambientColor = ambient,
                    spotColor = spot
                )
                .background(BgPrimary, RoundedCornerShape(40.dp))
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { tab ->
                val selected = current == tab.route
                // 选中态对齐本体侧边栏：SidebarItemActiveBg / SidebarItemActiveText
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(47.dp)
                        .background(
                            if (selected) SidebarItemActiveBg else Color.Transparent,
                            RoundedCornerShape(28.dp)
                        )
                        .clickable {
                            if (!selected) {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        TabIcon(
                            glyph = tab.glyph,
                            tint = if (selected) SidebarItemActiveText else TextSecondary,
                            size = 20.dp
                        )
                        if (tab == Tab.CHAT && pending > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-3).dp)
                                    .clip(CircleShape)
                                    .background(Danger)
                                    .size(7.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) SidebarItemActiveText else TextSecondary
                    )
                }
            }
        }
    }
}
