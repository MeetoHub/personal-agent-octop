# 抢占式截图：设备上有真人同时在用抖音/小红书/微信，会随时抢前台。
# 单次 dumpsys 确认焦点后截图是不够的 —— 焦点可能在确认与 screencap 之间被抢走。
# 这里把「确认焦点 → 截图 → 截图后再次确认焦点」做成一个循环，
# 只有截图前后焦点都是本 app 才认可这张图，否则删掉重来。
param(
    [Parameter(Mandatory = $true)][string]$Name,
    [int]$MaxTries = 12
)

$pkg  = "com.octop.mobile.debug"
$act  = "$pkg/com.octop.mobile.MainActivity"
$out  = Join-Path $PSScriptRoot "shots\$Name.png"
$rival = @("com.xingin.xhs", "com.ss.android.ugc.aweme", "com.tencent.mm")

function Get-Focus {
    (adb shell dumpsys window 2>$null | Select-String "mCurrentFocus").ToString()
}

for ($i = 1; $i -le $MaxTries; $i++) {
    foreach ($r in $rival) { adb shell am force-stop $r 2>&1 | Out-Null }
    adb shell am start -n $act 2>&1 | Out-Null
    Start-Sleep -Milliseconds 1500

    $before = Get-Focus
    if ($before -notmatch [regex]::Escape($pkg)) { continue }

    # 关键：截图紧贴焦点确认，中间不留 sleep
    adb exec-out screencap -p > $out
    $after = Get-Focus

    $size = (Get-Item $out).Length
    if ($after -match [regex]::Escape($pkg) -and $size -gt 150000) {
        "OK  try=$i  size=$size  focus=$($after.Trim())"
        exit 0
    }
    "retry try=$i  size=$size  afterFocus=$($after.Trim())"
    Start-Sleep -Milliseconds 800
}

"FAILED after $MaxTries tries"
exit 1
