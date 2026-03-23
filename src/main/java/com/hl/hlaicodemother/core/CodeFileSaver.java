package com.hl.hlaicodemother.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.ai.model.HtmlCodeResult;
import com.hl.hlaicodemother.ai.model.MultiFileCodeResult;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存器
 * 负责将 AI 生成的代码结果保存到本地文件系统中，支持不同类型的代码结果（如 HTML、CSS、JS 等），并返回保存的文件路径
 */
@Deprecated
public class CodeFileSaver {

    // 文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存 HTML 代码结果
     * @param htmlCodeResult
     * @return
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult) {
        // 构建唯一目录路径
        String dirPath = buildUniqueDirPath(CodeGenTypeEnum.HTML.getValue());
        // 保存 HTML 文件
        writeToFile(dirPath, "index.html", htmlCodeResult.getHtmlCode());
        return new File(dirPath);
    }

    /**
     * 保存多文件代码结果（HTML、CSS、JS）
     * @param multiFileCodeResult
     * @return
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult multiFileCodeResult)  {
        // 构建唯一目录路径
        String dirPath = buildUniqueDirPath(CodeGenTypeEnum.MULTI_FILE.getValue());
        // 保存多个文件
        String htmlCode = multiFileCodeResult.getHtmlCode();
        String cssCode = multiFileCodeResult.getCssCode();
        String jsCode = multiFileCodeResult.getJsCode();
        writeToFile(dirPath, "index.html", htmlCode);
        writeToFile(dirPath, "style.css", cssCode);
        writeToFile(dirPath, "script.js", jsCode);
        return new File(dirPath);
    }

    /**
     * 构建唯一目录路劲：tmp/code_output/bizType_雪花ID
     * @param bizType
     * @return
     */
    private static String buildUniqueDirPath(String bizType) {
        String uniqueDirPath = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirPath;
        // 创建目录
        if (!new File(dirPath).exists()) {
            try {
                FileUtil.mkdir(dirPath);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建文件目录失败: " + dirPath);
            }
        }
        return dirPath;
    }

    /**
     * 保存单个文件
     * @param dirPath
     * @param fileName
     * @param content
     * @throws Exception
     */
    private static void writeToFile(String dirPath, String fileName, String content) {
        String filePath = dirPath + File.separator + fileName;
        // 创建文件
        try {
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存文件失败: " + filePath);
        }
    }
}
