/*
 * Copyright 2026 17Artist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package priv.seventeen.artist.blink.bootstrap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import sun.misc.Unsafe;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;

public final class KotlinBootstrap {

    private static final String PREFIX = "\u00A79\u25C6 \u00A7bBlink \u00A78| ";
    private static final String DEFAULT_KOTLIN_VERSION = "1.8.22";
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 30000;

    private static final String[] DEFAULT_REPOSITORIES = {
            "https://maven.aliyun.com/repository/central",
            "https://repo1.maven.org/maven2",
            "https://repo.huaweicloud.com/repository/maven"
    };

    private KotlinBootstrap() {}

    public static void bootstrap(JavaPlugin plugin) {
        ClassLoader classLoader = plugin.getClass().getClassLoader();

        int[] envVersion = detectKotlinVersion(classLoader);
        if (envVersion != null) {
            logInfo("Kotlin " + envVersion[0] + "." + envVersion[1] + "." + envVersion[2]
                    + " detected in environment, skipping download");
            ensureReflectAndAnnotations(plugin, classLoader, envVersion);
            return;
        }

        File libsDir = new File(plugin.getDataFolder(), "libs");
        libsDir.mkdirs();

        String kotlinVersion = DEFAULT_KOTLIN_VERSION;
        String[] repositories = DEFAULT_REPOSITORIES;

        File configFile = new File(plugin.getDataFolder(), "blink.yml");
        if (configFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
            String ver = yaml.getString("kotlin-version");
            if (ver != null && !ver.isBlank()) {
                kotlinVersion = ver;
            }
            List<String> repos = yaml.getStringList("repositories");
            if (!repos.isEmpty()) {
                repositories = repos.toArray(new String[0]);
            }
        }

        String[][] kotlinDeps = {
                {"org.jetbrains.kotlin", "kotlin-stdlib", kotlinVersion},
                {"org.jetbrains.kotlin", "kotlin-reflect", kotlinVersion},
                {"org.jetbrains", "annotations", "24.1.0"}
        };

        for (String[] dep : kotlinDeps) {
            String group = dep[0], artifact = dep[1], version = dep[2];
            if (isClassAvailable(classForArtifact(artifact), classLoader)) {
                logDetail(artifact + " already available, skipping");
                continue;
            }
            String fileName = artifact + "-" + version + ".jar";
            File file = new File(libsDir, fileName);
            if (!file.exists()) {
                logInfo("Downloading " + group + ":" + artifact + ":" + version + "...");
                boolean ok = tryDownload(group, artifact, version, repositories, file, plugin);
                if (!ok) {
                    logError(fileName + " download failed from all repositories!");
                    continue;
                }
                logSuccess("Downloaded: " + fileName);
            }
            try {
                injectClasspath(classLoader, file);
            } catch (Throwable e) {
                logError("Failed to inject " + file.getName());
                plugin.getLogger().log(Level.SEVERE, "", e);
            }
        }
    }

    private static int[] detectKotlinVersion(ClassLoader classLoader) {
        try {
            Class<?> kvClass = Class.forName("kotlin.KotlinVersion", true, classLoader);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Object current = lookup.findStaticGetter(kvClass, "CURRENT", kvClass).invoke();
            int major = (int) lookup.findVirtual(kvClass, "getMajor", MethodType.methodType(int.class)).invoke(current);
            int minor = (int) lookup.findVirtual(kvClass, "getMinor", MethodType.methodType(int.class)).invoke(current);
            int patch = (int) lookup.findVirtual(kvClass, "getPatch", MethodType.methodType(int.class)).invoke(current);
            return new int[]{major, minor, patch};
        } catch (Throwable e) {
            return null;
        }
    }

    // 环境已有 stdlib 时，reflect 和 annotations 可能仍缺失
    private static void ensureReflectAndAnnotations(JavaPlugin plugin, ClassLoader classLoader, int[] envVersion) {
        String envVersionStr = envVersion[0] + "." + envVersion[1] + "." + envVersion[2];

        String[] repositories = DEFAULT_REPOSITORIES;
        File configFile = new File(plugin.getDataFolder(), "blink.yml");
        if (configFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
            List<String> repos = yaml.getStringList("repositories");
            if (!repos.isEmpty()) {
                repositories = repos.toArray(new String[0]);
            }
        }

        String[][] extras = {
                {"org.jetbrains.kotlin", "kotlin-reflect", envVersionStr, "kotlin.reflect.full.KClasses"},
                {"org.jetbrains", "annotations", "24.1.0", "org.jetbrains.annotations.NotNull"}
        };

        File libsDir = new File(plugin.getDataFolder(), "libs");
        //noinspection ResultOfMethodCallIgnored
        libsDir.mkdirs();

        for (String[] dep : extras) {
            String group = dep[0], artifact = dep[1], version = dep[2], checkClass = dep[3];
            if (isClassAvailable(checkClass, classLoader)) continue;

            String fileName = artifact + "-" + version + ".jar";
            File file = new File(libsDir, fileName);
            if (!file.exists()) {
                logInfo("Downloading " + group + ":" + artifact + ":" + version + "...");
                boolean ok = tryDownload(group, artifact, version, repositories, file, plugin);
                if (!ok) {
                    logError(fileName + " download failed!");
                    continue;
                }
            }
            try {
                injectClasspath(classLoader, file);
            } catch (Throwable e) {
                logError("Failed to inject " + file.getName());
                plugin.getLogger().log(Level.SEVERE, "", e);
            }
        }
    }

    private static String classForArtifact(String artifact) {
        return switch (artifact) {
            case "kotlin-stdlib" -> "kotlin.KotlinVersion";
            case "kotlin-reflect" -> "kotlin.reflect.full.KClasses";
            case "annotations" -> "org.jetbrains.annotations.NotNull";
            default -> null;
        };
    }

    private static boolean isClassAvailable(String className, ClassLoader classLoader) {
        if (className == null) return false;
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean tryDownload(String group, String artifact, String version,
                                       String[] repos, File target, JavaPlugin plugin) {
        String groupPath = group.replace('.', '/');
        String fileName = artifact + "-" + version + ".jar";
        String path = groupPath + "/" + artifact + "/" + version + "/" + fileName;

        for (String repo : repos) {
            String repoBase = repo.endsWith("/") ? repo.substring(0, repo.length() - 1) : repo;
            String url = repoBase + "/" + path;
            try {
                logDetail("  Trying: " + repoBase);
                downloadFile(url, target);
                return true;
            } catch (Exception e) {
                logWarn("  " + repoBase + " failed: " + e.getMessage());
            }
        }
        return false;
    }

    private static void downloadFile(String urlStr, File target) throws Exception {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setInstanceFollowRedirects(true);

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                throw new RuntimeException("HTTP " + conn.getResponseCode());
            }

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = Files.newOutputStream(tmp.toPath())) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
            conn.disconnect();

            if (tmp.length() < 1024) {
                throw new RuntimeException("File too small (" + tmp.length() + " bytes)");
            }

            if (target.exists()) {
                //noinspection ResultOfMethodCallIgnored
                target.delete();
            }
            if (!tmp.renameTo(target)) {
                Files.copy(tmp.toPath(), target.toPath());
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        } finally {
            if (tmp.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        }
    }

    private static void injectClasspath(ClassLoader classLoader, File file) throws Throwable {
        URL url = file.toURI().toURL();

        Method addURL = findAddURL(classLoader);
        if (addURL != null) {
            try {
                addURL.setAccessible(true);
                addURL.invoke(classLoader, url);
                return;
            } catch (Exception ignored) { }
        }

        try {
            sun.misc.Unsafe unsafe = getUnsafe();
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            long offset = unsafe.staticFieldOffset(implLookupField);
            MethodHandles.Lookup trustedLookup = (MethodHandles.Lookup) unsafe.getObject(
                    MethodHandles.Lookup.class, offset);

            if (addURL != null) {
                MethodHandle handle = trustedLookup.unreflect(addURL);
                handle.invoke(classLoader, url);
                return;
            }

            Class<?> clazz = classLoader.getClass();
            while (clazz != null) {
                try {
                    MethodHandle handle = trustedLookup.findVirtual(clazz, "addURL",
                            MethodType.methodType(void.class, URL.class));
                    handle.invoke(classLoader, url);
                    return;
                } catch (NoSuchMethodException | IllegalAccessException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Cannot inject into ClassLoader "
                    + classLoader.getClass().getName() + " — all injection methods failed", e);
        }

        throw new IllegalStateException("Cannot inject into ClassLoader "
                + classLoader.getClass().getName() + " — no addURL method found");
    }

    private static Method findAddURL(ClassLoader classLoader) {
        Class<?> clazz = classLoader.getClass();
        while (clazz != null) {
            try {
                return clazz.getDeclaredMethod("addURL", URL.class);
            } catch (NoSuchMethodException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private static Unsafe getUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    // ==================== 控制台彩色日志 ====================

    /** 输出普通信息（白色） */
    private static void logInfo(String message) { log("\u00A7f", message); }

    /** 输出成功信息（绿色） */
    private static void logSuccess(String message) { log("\u00A7a", message); }

    /** 输出警告信息（黄色） */
    private static void logWarn(String message) { log("\u00A7e", message); }

    /** 输出错误信息（红色） */
    private static void logError(String message) { log("\u00A7c", message); }

    /** 输出次要信息（灰色） */
    private static void logDetail(String message) { log("\u00A77", message); }

    private static void log(String color, String message) {
        try {
            Bukkit.getConsoleSender().sendMessage(PREFIX + color + message);
        } catch (Exception e) {
            System.out.println("[Blink] " + message);
        }
    }
}
