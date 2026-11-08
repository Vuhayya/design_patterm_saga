# Stops whatever is listening on the 5 backend ports.
foreach ($port in 3000, 8080, 8081, 8082, 8083, 8084) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        ForEach-Object {
            Write-Host "Stopping PID $($_.OwningProcess) on port $port"
            Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
        }
}
