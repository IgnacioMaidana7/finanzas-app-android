param(
    [Parameter(Mandatory)][string]$ArchivePath,
    [Parameter(Mandatory)][ValidatePattern('^[a-f0-9]{64}$')][string]$ExpectedSha256,
    [Parameter(Mandatory)][ValidatePattern('^[a-z0-9]{5,50}$')][string]$RegistryName,
    [Parameter(Mandatory)][ValidatePattern('^[a-z0-9][a-z0-9_.-]{0,100}$')][string]$Tag,
    [switch]$ApproveCreditUsage
)
$ErrorActionPreference = 'Stop'
if (-not $ApproveCreditUsage) { throw 'Approve the cloud transfer and credit usage first.' }
. (Join-Path $PSScriptRoot 'Initialize-AzureTools.ps1')
$taskCrane = Join-Path $env:LOCALAPPDATA 'Programs\tp3-crane\crane.exe'
if (-not (Test-Path -LiteralPath $taskCrane)) { throw 'Install checksum-verified crane from its official Google release.' }
if ((Get-FileHash -LiteralPath $ArchivePath -Algorithm SHA256).Hash -ine $ExpectedSha256) {
    throw 'Image archive hash differs from the tested CI artifact.'
}
$env:DOCKER_CONFIG = Join-Path $env:LOCALAPPDATA 'FinanzasTP3\docker-tp3-transfer'
New-Item -ItemType Directory -Path $env:DOCKER_CONFIG -Force | Out-Null
$taskServer = $RegistryName + '.azurecr.io'
try {
    # Capture in memory: never print, pass as an argument, or commit this token.
    $taskToken = az acr login --name $RegistryName --expose-token --output json | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0 -or $taskToken.loginServer -ne $taskServer) { throw 'Registry authentication failed.' }
    $taskToken.accessToken | & $taskCrane auth login $taskServer --username '00000000-0000-0000-0000-000000000000' --password-stdin
    if ($LASTEXITCODE -ne 0) { throw 'Registry login failed.' }
    & $taskCrane push $ArchivePath ($taskServer + '/summary:' + $Tag)
    if ($LASTEXITCODE -ne 0) { throw 'Image upload failed.' }
    $taskDigest = & $taskCrane digest ($taskServer + '/summary:' + $Tag)
    if ($LASTEXITCODE -ne 0 -or $taskDigest -notmatch '^sha256:[a-f0-9]{64}$') { throw 'Registry digest verification failed.' }
    $taskImageReference = $taskServer + '/summary@' + $taskDigest
    $taskImageReference | Set-Content -Encoding utf8 (Join-Path $env:LOCALAPPDATA 'FinanzasTP3\tested-image-reference.txt')
    Write-Output $taskImageReference
} finally {
    $taskToken = $null
    & $taskCrane auth logout $taskServer
}
