# Starts the Next.js frontend on http://localhost:3000 (backend must be running on :8080).
$root = Split-Path -Parent $PSScriptRoot
Set-Location (Join-Path $root 'frontend')
if (-not (Test-Path node_modules)) { npm install }
npm run dev
