# Starts the 5 backend services, each in its own window. Uses `mvn spring-boot:run`
# on purpose: `java` on PATH may be JDK 8, while Maven uses JAVA_HOME (JDK 21).
# Order: business services first, then the orchestrator, then the gateway.
$root = Split-Path -Parent $PSScriptRoot
$services = @('order-service', 'inventory-service', 'payment-service', 'saga-orchestrator', 'api-gateway')

foreach ($svc in $services) {
    Write-Host "Starting $svc ..."
    Start-Process powershell -ArgumentList '-NoExit', '-Command',
        "`$Host.UI.RawUI.WindowTitle='$svc'; Set-Location '$root'; mvn -q -pl $svc spring-boot:run"
}
Write-Host "Gateway will be on http://localhost:8080 once all windows show 'Started'."
