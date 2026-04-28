package com.hl.hlaicodemother.constant;

import java.io.File;

/**
 * Vue 工程模式代码输出目录：{@code tmp/code_output/vue_project_{appId}/v{n}/}
 */
public final class VueProjectOutputPaths {

    private VueProjectOutputPaths() {
    }

    public static String projectDirName(long appId) {
        return "vue_project_" + appId;
    }

    public static String versionSegment(int version) {
        return "v" + version;
    }

    /**
     * 某应用某版本的绝对根目录（文件写入、部署源目录）
     */
    public static String versionRootAbsolute(long appId, int version) {
        return AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + projectDirName(appId)
                + File.separator + versionSegment(version);
    }
}
