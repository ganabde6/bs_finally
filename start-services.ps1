# 隐藏窗口启动前后端服务(通过计划任务调用,避免可见控制台窗口被关闭导致进程终止)
$ErrorActionPreference = 'SilentlyContinue'

# 后端: 9000 端口未监听才启动
$backend = Get-NetTCPConnection -LocalPort 9000 -State Listen -ErrorAction SilentlyContinue
if (-not $backend) {
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "D:\bs_finally\launch-backend.cmd" -WindowStyle Hidden
}

# 前端: 5173 端口未监听才启动
$frontend = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
if (-not $frontend) {
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "D:\bs_finally\launch-frontend.cmd" -WindowStyle Hidden
}

# 写一个标记,便于确认脚本执行过
"started at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" | Out-File -FilePath "D:\bs_finally\start-services.log" -Encoding utf8
