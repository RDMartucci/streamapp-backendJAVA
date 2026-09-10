$ErrorActionPreference = 'Stop'

Write-Host 'Checking Docker Desktop...' -ForegroundColor Cyan
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker was not found in PATH. Install Docker Desktop and restart PowerShell.'
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not running. Start Docker Desktop and run this script again.'
}

Write-Host 'Starting MySQL container...' -ForegroundColor Cyan
$existingContainer = docker container inspect streamapp-mysql 2>$null

if ($LASTEXITCODE -eq 0) {
    $containerStatus = docker inspect --format '{{.State.Status}}' streamapp-mysql
    if ($containerStatus -ne 'running') {
        docker start streamapp-mysql | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw 'The existing streamapp-mysql container could not be started.'
        }
    }
}
else {
    docker compose up -d --wait mysql
    if ($LASTEXITCODE -ne 0) {
        throw 'MySQL could not be started. Check Docker Desktop and run: docker compose logs mysql'
    }
}

$containerStatus = docker inspect --format '{{.State.Status}}' streamapp-mysql 2>$null
$healthStatus = docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' streamapp-mysql 2>$null

if ($containerStatus -ne 'running') {
    throw "MySQL is not ready. Container status: $containerStatus; health: $healthStatus"
}

if ($healthStatus -ne 'healthy' -and $healthStatus -ne 'no-healthcheck') {
    throw "MySQL healthcheck failed. Container status: $containerStatus; health: $healthStatus"
}

if ($healthStatus -eq 'no-healthcheck') {
    $mysqlReady = $false
    1..30 | ForEach-Object {
        if (-not $mysqlReady) {
            docker exec streamapp-mysql mysqladmin ping -h localhost --silent *> $null
            if ($LASTEXITCODE -eq 0) { $mysqlReady = $true }
        }
    }
    if (-not $mysqlReady) {
        throw 'The streamapp-mysql container is running but MySQL is not responding.'
    }
}

Write-Host 'MySQL is ready on localhost:3306.' -ForegroundColor Green
Write-Host 'Now run: .\mvnw.cmd spring-boot:run' -ForegroundColor Gray