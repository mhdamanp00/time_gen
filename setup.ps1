#!/usr/bin/env powershell
# =================================================================
# TimetableScheduler — One-time setup script for Windows
# Installs Java 17 JDK + Maven if not already present
# =================================================================
# Run as: .\setup.ps1   (from the project root)
# =================================================================

Write-Host "`n== Automatic Class Timetable Scheduler — Setup ==" -ForegroundColor Cyan

# ── 1. Check / Install Java 17 ────────────────────────────────
Write-Host "`n[1/2] Checking Java 17..." -ForegroundColor Yellow

$java17 = Get-Command java -ErrorAction SilentlyContinue
$needJava = $true

if ($java17) {
    $ver = & java -version 2>&1 | Select-String "version"
    if ($ver -match '"17\.' -or $ver -match '"21\.' -or $ver -match '"22\.') {
        Write-Host "    Java 17+ already installed: $ver" -ForegroundColor Green
        $needJava = $false
    }
}

if ($needJava) {
    Write-Host "    Java 17 not found. Attempting install via winget..." -ForegroundColor Yellow
    try {
        winget install EclipseAdoptium.Temurin.17.JDK --silent --accept-source-agreements --accept-package-agreements
        Write-Host "    Java 17 installed. Please RESTART this terminal and re-run setup.ps1" -ForegroundColor Green
        Write-Host "    Or set JAVA_HOME manually to your JDK 17 path." -ForegroundColor Yellow
        exit 0
    } catch {
        Write-Host "    winget not available. Please install manually:" -ForegroundColor Red
        Write-Host "    https://adoptium.net/temurin/releases/?version=17" -ForegroundColor Cyan
        Write-Host "    Then set JAVA_HOME and re-run this script." -ForegroundColor Yellow
        exit 1
    }
}

# ── 2. Check / Install Maven ──────────────────────────────────
Write-Host "`n[2/2] Checking Maven..." -ForegroundColor Yellow

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvn) {
    $mvnVer = & mvn --version 2>&1 | Select-String "Apache Maven"
    Write-Host "    Maven already installed: $mvnVer" -ForegroundColor Green
} else {
    Write-Host "    Maven not found. Downloading Maven 3.9.6..." -ForegroundColor Yellow
    $mavenUrl = "https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
    $mavenZip = "$env:TEMP\maven.zip"
    $mavenDir = "C:\tools\maven"

    Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip
    Expand-Archive -Path $mavenZip -DestinationPath "C:\tools" -Force
    Rename-Item "C:\tools\apache-maven-3.9.6" $mavenDir -ErrorAction SilentlyContinue

    # Add to PATH for current session
    $env:PATH = "$mavenDir\bin;$env:PATH"
    $env:M2_HOME = $mavenDir

    # Persist to user PATH
    [Environment]::SetEnvironmentVariable("M2_HOME", $mavenDir, "User")
    $userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
    [Environment]::SetEnvironmentVariable("PATH", "$mavenDir\bin;$userPath", "User")

    Write-Host "    Maven installed at: $mavenDir" -ForegroundColor Green
    Write-Host "    Please restart your terminal for PATH changes to take effect." -ForegroundColor Yellow
}

# ── 3. Build & Run ───────────────────────────────────────────
Write-Host "`n== Setup complete! ==" -ForegroundColor Cyan
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor White
Write-Host "  1. Create MySQL database:  CREATE DATABASE timetable_db;" -ForegroundColor Gray
Write-Host "  2. Edit hibernate.cfg.xml — set your MySQL username/password" -ForegroundColor Gray
Write-Host "  3. Build:   mvn clean compile" -ForegroundColor Gray
Write-Host "  4. Run:     mvn javafx:run" -ForegroundColor Gray
Write-Host ""
