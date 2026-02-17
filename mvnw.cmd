@REM Maven Wrapper for Windows
@REM Auto-downloads Maven if not present

@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6"
set "MVN_CMD=%MAVEN_HOME%\apache-maven-3.9.6\bin\mvn.cmd"

if exist "%MVN_CMD%" goto runMaven

echo Downloading Maven 3.9.6...
set "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip"
set "MAVEN_ZIP=%MAVEN_HOME%\maven.zip"

if not exist "%MAVEN_HOME%" mkdir "%MAVEN_HOME%"

powershell -Command "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%MAVEN_ZIP%'"
if errorlevel 1 (
    echo Failed to download Maven
    exit /b 1
)

powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_HOME%' -Force"
if errorlevel 1 (
    echo Failed to extract Maven
    exit /b 1
)

del "%MAVEN_ZIP%" 2>nul

:runMaven
"%MVN_CMD%" %*
