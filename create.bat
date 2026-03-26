@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

echo.
echo   ⚡ Blink 项目初始化
echo.

set /p PROJECT_NAME="  项目名称: "
set /p AUTHOR="  作者: "
set /p PACKAGE_NAME="  包名 (如 com.example.myplugin): "

if "%PROJECT_NAME%"=="" goto :missing
if "%AUTHOR%"=="" goto :missing
if "%PACKAGE_NAME%"=="" goto :missing

if exist "%PROJECT_NAME%" (
    echo   ❌ 目录 %PROJECT_NAME% 已存在
    exit /b 1
)

set "PACKAGE_DIR=%PACKAGE_NAME:.=\%"
set "SRC_DIR=%PROJECT_NAME%\src\main\kotlin\%PACKAGE_DIR%"

mkdir "%SRC_DIR%"
mkdir "%PROJECT_NAME%\gradle\wrapper"

(
echo pluginManagement {
echo     repositories {
echo         maven("https://repo.arcartx.com/repository/maven-public/"^)
echo         gradlePluginPortal(^)
echo         mavenCentral(^)
echo     }
echo }
echo rootProject.name = "%PROJECT_NAME%"
) > "%PROJECT_NAME%\settings.gradle.kts"

(
echo plugins {
echo     kotlin("jvm"^) version "1.8.22"
echo     id("priv.seventeen.artist.blink"^) version "1.0.0"
echo     id("com.github.johnrengelman.shadow"^) version "8.1.1"
echo }
echo.
echo group = "%PACKAGE_NAME%"
echo version = "1.0.0"
echo.
echo repositories {
echo     maven("https://repo.arcartx.com/repository/maven-public/"^)
echo     mavenCentral(^)
echo     maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/"^)
echo }
echo.
echo blink {
echo     name.set("%PROJECT_NAME%"^)
echo     version.set("1.0.0"^)
echo     authors.set(listOf("%AUTHOR%"^)^)
echo     apiVersion.set("1.21"^)
echo     packageName.set("%PACKAGE_NAME%"^)
echo }
echo.
echo dependencies {
echo     implementation("priv.seventeen.artist.blink:blink-common:1.0.0"^)
echo     compileOnly("org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT"^)
echo }
echo.
echo kotlin {
echo     jvmToolchain(17^)
echo }
echo.
echo tasks.named("build"^) {
echo     dependsOn("shadowJar"^)
echo }
) > "%PROJECT_NAME%\build.gradle.kts"

(
echo package %PACKAGE_NAME%
echo.
echo import priv.seventeen.artist.blink.bukkitPlugin
echo import priv.seventeen.artist.blink.lifecycle.Awake
echo import priv.seventeen.artist.blink.lifecycle.LifeCycle
echo.
echo object Bootstrap {
echo.
echo     @Awake(LifeCycle.ENABLE^)
echo     fun onEnable(^) {
echo         bukkitPlugin.logger.info("%PROJECT_NAME% enabled!"^)
echo     }
echo.
echo     @Awake(LifeCycle.DISABLE^)
echo     fun onDisable(^) {
echo         bukkitPlugin.logger.info("%PROJECT_NAME% disabled!"^)
echo     }
echo }
) > "%SRC_DIR%\Bootstrap.kt"

(
echo distributionBase=GRADLE_USER_HOME
echo distributionPath=wrapper/dists
echo distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
echo networkTimeout=10000
echo validateDistributionUrl=true
echo zipStoreBase=GRADLE_USER_HOME
echo zipStorePath=wrapper/dists
) > "%PROJECT_NAME%\gradle\wrapper\gradle-wrapper.properties"

(
echo build/
echo .gradle/
echo .idea/
echo .kotlin/
echo *.iml
echo out/
echo libs/
) > "%PROJECT_NAME%\.gitignore"

echo.
echo   ✅ 项目已创建: %PROJECT_NAME%\
echo.
echo   下一步:
echo     cd %PROJECT_NAME%
echo     gradle wrapper    &REM 生成 gradlew
echo     gradlew build     &REM 构建插件
echo.

exit /b 0

:missing
echo   ❌ 所有字段都必须填写
exit /b 1
