package com.hl.hlaicodemother.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {

    public void buildProjectAsync(String projectPath) {
        // 在单独的线程中执行构建，避免阻塞主线程
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis())
                .start(() -> {
                    try {
                        buildVueProject(projectPath);
                    } catch (Exception e) {
                        log.error("异步构建 Vue 项目时发生异常：{}", e.getMessage(), e);
                    }
                });
    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目路径
     * @return 是否构建成功
     */
    public boolean buildVueProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在或无效：{}", projectPath);
            return false;
        }
        // 检查 package.json 文件是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("项目目录下不存在 package.json 文件：{}", projectPath);
        }
        log.info("开始构建 Vue 项目：{}", projectPath);
        // 执行 npm install 命令
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 命令执行失败");
            return false;
        }
        // 执行 npm run build 命令
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 命令执行失败");
            return false;
        }
        // 验证 dist 目录是否存在
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成：{}", distDir.getAbsolutePath());
            return false;
        }
        log.info("构建成功，dist 项目路径：{}", distDir.getAbsolutePath());
        return true;
    }


    /**
     * 执行 npm install 命令
     *
     * @param projectDir 项目目录
     * @return 是否构建成功
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("开始执行 npm install 命令");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300); // 超时时间设置为 300 秒
    }

    /**
     * 执行 npm build 命令
     *
     * @param projectDir 项目目录
     * @return 是否构建成功
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("开始执行 npm build 命令");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180); // 超时时间设置为 180 秒
    }

    private String buildCommand(String command) {
        if (isWindows()) {
            return command + ".cmd";
        }
        return command;
    }

    /**
     * 判断当前操作系统是否为 Windows
     *
     * @return true 表示当前操作系统为 Windows，false 表示当前操作系统为非 Windows
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    /**
     * 在指定目录下执行命令，并等待命令执行完成
     *
     * @param workingDir  工作目录
     * @param command     命令
     * @param timeoutSeconds 超时时间（秒）
     * @return 命令执行结果
     */
    private boolean executeCommand(File workingDir, String  command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令：{}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 使用空格分隔命令
            );
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("命令执行超时 （{} 秒），已自动结束进程：{}", timeoutSeconds, command);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功：{}", command);
                return true;
            } else {
                log.error("命令执行失败：{}，错误码：{}", command, exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败：{}，错误信息：{}", command, e.getMessage());
            return false;
        }
    }
}
