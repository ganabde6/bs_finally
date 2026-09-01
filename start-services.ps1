# 隐藏窗口启动前后端服务(通过计划任务调用,避免可见控制台窗口被关闭导致进程终止)
$ErrorActionPreference = 'SilentlyContinue'

# 后端: 9000 端口未监听才启动
$backend = Get-NetTCPConnection -LocalPort 9000 -State Listen -ErrorAction SilentlyContinue
if (-not $backend) {
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c