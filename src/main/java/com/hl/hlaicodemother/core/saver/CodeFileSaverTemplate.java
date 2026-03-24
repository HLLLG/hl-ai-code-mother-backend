package com.hl.hlaicodemother.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.constant.AppConstant;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存器模板类（模板方法模式）
 * @param <T>
 */
public abstract class CodeFileSaverTemplate<T> {

    // 文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 保存代码文件的模板方法
     * 定义了保存代码文件的基本流程
     *
     */
    public final File saveCode(T result, Long appId) {
        // 1. 验证输入参数
        validateInput(result);
        // 2. 构建目录
        String dirPath = buildUniqueDirPath(appId);
        // 3. 保存代码文件
        saveFiles(result, dirPath);
        // 4. 返回目录文件对象
        return new File(dirPath);
    }

    /**
     * 验证输入参数是否合法
     *
     * @param result 代码内容
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码结果对象不能为空");
        }
    }

    /**
     * 构建唯一目录路劲：tmp/code_output/bizType_雪花ID
     * @return
     */
    protected  String buildUniqueDirPath(Long appId) {
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        String bizType = getCodeType().getValue();
        String uniqueDirPath = StrUtil.format("{}_{}", bizType, appId);
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
    protected final void writeToFile(String dirPath, String fileName, String content) {
        String filePath = dirPath + File.separator + fileName;
        // 创建文件
        try {
            if (StrUtil.isNotBlank(content)) {
                FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
            }
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存文件失败: " + filePath);
        }
    }

    /**
     * 获取代码类型（由子类实现）
     * @return
     */
    protected abstract CodeGenTypeEnum getCodeType();


    /**
     * 保存代码文件（由子类实现）
     * @param result
     * @param dirPath
     */
    protected abstract void saveFiles(T result, String dirPath);

}
