param(
    [string]$ProjectId = 'finanzasapp-1f76a',
    [string]$Region,
    [string]$Service = 'tp3-finanzas-summary'
)
$ErrorActionPreference = 'Stop'
if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw 'Falta Google Cloud CLI. Ejecutar este script en Cloud Shell o instalar la CLI oficial. No se hicieron cambios.'
}
function Read-GcloudJson([string[]]$Arguments) {
    $result = & gcloud @Arguments --format=json
    if ($LASTEXITCODE -ne 0) { throw "No se pudo completar una consulta cloud de sólo lectura: $($Arguments[0])" }
    return $result | ConvertFrom-Json
}
$project = Read-GcloudJson -Arguments @('projects', 'describe', $ProjectId)
$billing = Read-GcloudJson -Arguments @('billing', 'projects', 'describe', $ProjectId)
$databases = Read-GcloudJson -Arguments @('firestore', 'databases', 'list', "--project=$ProjectId")
$services = Read-GcloudJson -Arguments @('services', 'list', '--enabled', "--project=$ProjectId")
$report = [ordered]@{
    capturedAtUtc = [DateTime]::UtcNow.ToString('o')
    projectId = $project.projectId
    projectState = $project.lifecycleState
    billingEnabled = $billing.billingEnabled
    databases = @($databases | ForEach-Object { @{ name = $_.name; locationId = $_.locationId; type = $_.type } })
    enabledApis = @($services | ForEach-Object { $_.config.name })
    deployedService = $null
}
if ($Region) {
    $serviceData = Read-GcloudJson -Arguments @('run', 'services', 'describe', $Service, "--region=$Region", "--project=$ProjectId")
    $policy = Read-GcloudJson -Arguments @('run', 'services', 'get-iam-policy', $Service, "--region=$Region", "--project=$ProjectId")
    $report.deployedService = @{
        name = $serviceData.metadata.name
        url = $serviceData.status.url
        readyRevision = $serviceData.status.latestReadyRevisionName
        serviceScaling = $serviceData.metadata.annotations
        revisionScaling = $serviceData.spec.template.metadata.annotations
        concurrency = $serviceData.spec.template.spec.containerConcurrency
        timeoutSeconds = $serviceData.spec.template.spec.timeoutSeconds
        serviceAccount = $serviceData.spec.template.spec.serviceAccountName
        resources = $serviceData.spec.template.spec.containers[0].resources
        image = $serviceData.spec.template.spec.containers[0].image
        traffic = $serviceData.status.traffic
        invokerBindings = @($policy.bindings | Where-Object { $_.role -eq 'roles/run.invoker' })
    }
}
$evidenceDir = Join-Path $PSScriptRoot '../docs/tp3/evidence'
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null
$path = Join-Path $evidenceDir 'cloud-inspection.local.json'
$report | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $path -Encoding utf8
Write-Output "Inspección de sólo lectura guardada en $path. Revisar y anonimizar antes de entregar."
