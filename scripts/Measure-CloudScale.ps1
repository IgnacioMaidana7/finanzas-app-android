param(
    [int]$DurationSeconds = 480,
    [int]$IntervalSeconds = 5
)
if ($DurationSeconds -lt 10 -or $DurationSeconds -gt 600 -or $IntervalSeconds -lt 5) {
    throw 'Use a bounded observation of 10-600 seconds and intervals of at least 5 seconds.'
}
. (Join-Path $PSScriptRoot 'Initialize-AzureTools.ps1')
$taskSamples = [Collections.Generic.List[object]]::new()
$taskEnd = [DateTime]::UtcNow.AddSeconds($DurationSeconds)
$taskPrevious = -1
$taskEvidencePath = Join-Path $PSScriptRoot '../docs/tp3/evidence/azure-scale-observed.json'
do {
    $taskReplicas = @(az containerapp replica list --name ca-tp3-finanzas-summary --resource-group rg-tp3-finanzas --output json | ConvertFrom-Json)
    if ($LASTEXITCODE -ne 0) { throw 'Azure replica observation failed.' }
    $taskReady = @($taskReplicas | Where-Object { @($_.properties.containers | Where-Object { $_.ready -eq $true }).Count -gt 0 })
    $taskSample = [ordered]@{atUtc=[DateTime]::UtcNow.ToString('o');replicas=$taskReplicas.Count;ready=$taskReady.Count}
    $taskSamples.Add($taskSample)
    [ordered]@{source='Azure Container Apps replica API';samples=$taskSamples;includesPersonalData=$false} |
        ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 $taskEvidencePath
    if ($taskPrevious -ne $taskReplicas.Count) {
        Write-Output ($taskSample.atUtc + ': replicas=' + $taskReplicas.Count + ', ready=' + $taskReady.Count)
        $taskPrevious = $taskReplicas.Count
    }
    if ([DateTime]::UtcNow -lt $taskEnd) { Start-Sleep -Seconds $IntervalSeconds }
} while ([DateTime]::UtcNow -lt $taskEnd)
Write-Output ('Saved ' + $taskSamples.Count + ' real replica observations.')
