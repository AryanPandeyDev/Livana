#!/usr/bin/env pwsh
# dump-anvil-state.ps1
#
# Calls Anvil's anvil_dumpState JSON-RPC endpoint and writes the resulting
# state snapshot to a file.  The file can be loaded later with:
#
#   anvil --load-state <StateFile>
#
# Usage (called by Makefile):
#   powershell -ExecutionPolicy Bypass -File script/dump-anvil-state.ps1 [StateFile] [RpcUrl]
#
# Arguments:
#   $StateFile  - Output path for the state JSON   (default: anvil-state.json)
#   $RpcUrl     - Anvil RPC endpoint               (default: http://127.0.0.1:8545)

param(
    [string]$StateFile = "anvil-state.json",
    [string]$RpcUrl    = "http://127.0.0.1:8545"
)

$ErrorActionPreference = "Stop"

Write-Host "Dumping Anvil state from $RpcUrl ..."

$body = @{
    jsonrpc = "2.0"
    method  = "anvil_dumpState"
    params  = @()
    id      = 1
} | ConvertTo-Json -Depth 3

try {
    $response = Invoke-RestMethod `
        -Uri         $RpcUrl `
        -Method      Post `
        -ContentType "application/json" `
        -Body        $body

    if ($null -eq $response.result) {
        Write-Error "anvil_dumpState returned null. Is Anvil running at $RpcUrl?"
        exit 1
    }

    # The result is a hex-encoded gzipped blob — write the raw string directly
    # so that `anvil --load-state` can consume it.
    Set-Content -Path $StateFile -Value $response.result -Encoding UTF8 -NoNewline
    Write-Host "State saved to $StateFile  ($([math]::Round((Get-Item $StateFile).Length / 1KB, 1)) KB)"
}
catch {
    Write-Error "Failed to dump Anvil state: $_"
    exit 1
}
