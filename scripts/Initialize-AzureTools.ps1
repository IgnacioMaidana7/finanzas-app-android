# Source in a local PowerShell process after installing the official Azure CLI ZIP.
# Credentials remain in the CLI's local cache, outside this repository and OneDrive.
$taskAzureCliDirectory = Join-Path $env:LOCALAPPDATA 'Programs\tp3-azure-cli\bin'
if (-not (Test-Path -LiteralPath (Join-Path $taskAzureCliDirectory 'az.cmd'))) {
    throw 'Install the official Azure CLI ZIP before using this helper.'
}
$env:PATH = $taskAzureCliDirectory + [IO.Path]::PathSeparator + $env:PATH
$env:AZURE_CONFIG_DIR = Join-Path $env:LOCALAPPDATA 'FinanzasTP3\azure'
$env:AZURE_CORE_COLLECT_TELEMETRY = 'no'
$env:AZURE_CORE_LOGIN_EXPERIENCE_V2 = 'off'
