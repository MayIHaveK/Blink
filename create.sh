#!/usr/bin/env bash
set -e

echo ""
echo "  ⚡ Blink 项目初始化"
echo ""

read -p "  项目名称: " PROJECT_NAME
read -p "  作者: " AUTHOR
read -p "  包名 (如 com.example.myplugin): " PACKAGE_NAME

if [ -z "$PROJECT_NAME" ] || [ -z "$AUTHOR" ] || [ -z "$PACKAGE_NAME" ]; then
    echo "  ❌ 所有字段都必须填写"
    exit 1
fi

PROJECT_DIR="$PROJECT_NAME"
if [ -d "$PROJECT_DIR" ]; then
    echo "  ❌ 目录 $PROJECT_DIR 已存在"
    exit 1
fi

PACKAGE_DIR=$(echo "$PACKAGE_NAME" | tr '.' '/')
SRC_DIR="$PROJECT_DIR/src/main/kotlin/$PACKAGE_DIR"

mkdir -p "$SRC_DIR"
mkdir -p "$PROJECT_DIR/gradle/wrapper"

# settings.gradle.kts
cat > "$PROJECT_DIR/settings.gradle.kts" << 'SETTINGS'
pluginManagement {
    repositories {
        maven("https://repo.arcartx.com/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
    }
}
SETTINGS
echo "rootProject.name = \"$PROJECT_NAME\"" >> "$PROJECT_DIR/settings.gradle.kts"

# build.gradle.kts
cat > "$PROJECT_DIR/build.gradle.kts" << EOF
plugins {
    kotlin("jvm") version "1.8.22"
    id("priv.seventeen.artist.blink") version "1.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "$PACKAGE_NAME"
version = "1.0.0"

repositories {
    maven("https://repo.arcartx.com/repository/maven-public/")
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

blink {
    name.set("$PROJECT_NAME")
    version.set("1.0.0")
    authors.set(listOf("$AUTHOR"))
    apiVersion.set("1.21")
    packageName.set("$PACKAGE_NAME")
}

dependencies {
    implementation("priv.seventeen.artist.blink:blink-common:1.0.0")
    compileOnly("org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(17)
}

tasks.named("build") {
    dependsOn("shadowJar")
}
EOF

# Bootstrap.kt
cat > "$SRC_DIR/Bootstrap.kt" << EOF
package $PACKAGE_NAME

import priv.seventeen.artist.blink.bukkitPlugin
import priv.seventeen.artist.blink.lifecycle.Awake
import priv.seventeen.artist.blink.lifecycle.LifeCycle

object Bootstrap {

    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        bukkitPlugin.logger.info("$PROJECT_NAME enabled!")
    }

    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        bukkitPlugin.logger.info("$PROJECT_NAME disabled!")
    }
}
EOF

# gradle-wrapper.properties
cat > "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.properties" << 'WRAPPER'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
WRAPPER

# .gitignore
cat > "$PROJECT_DIR/.gitignore" << 'GITIGNORE'
build/
.gradle/
.idea/
.kotlin/
*.iml
out/
libs/
GITIGNORE

echo ""
echo "  ✅ 项目已创建: $PROJECT_DIR/"
echo ""
echo "  下一步:"
echo "    cd $PROJECT_DIR"
echo "    gradle wrapper    # 生成 gradlew"
echo "    ./gradlew build   # 构建插件"
echo ""
