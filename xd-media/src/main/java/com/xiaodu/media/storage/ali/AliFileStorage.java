package com.xiaodu.media.storage.ali;

import com.xiaodu.common.exceptions.BadRequestException;
import com.xiaodu.common.exceptions.CommonException;
import com.xiaodu.common.utils.AliOssUtil;
import com.xiaodu.common.utils.AssertUtils;
import com.xiaodu.common.utils.CollUtils;
import com.xiaodu.media.storage.IFileStorage;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;

import static com.xiaodu.media.enums.FileErrorInfo.Msg.*;

@Slf4j
public class AliFileStorage implements IFileStorage {

    private final AliOssUtil aliOssUtil;

    public AliFileStorage(AliOssUtil aliOssUtil) {
        this.aliOssUtil = aliOssUtil;
    }

    @Override
    public String uploadFile(String key, InputStream inputStream, long contentLength) {
        AssertUtils.isNotBlank(key, FILE_KEY_IS_NULL);
        AssertUtils.isNotNull(inputStream);
        try {
            return aliOssUtil.uploadFile(key, inputStream, contentLength);
        } catch (Exception e) {
            log.error("上传文件[{}]失败 ", key, e);
            throw new CommonException("上传文件失败!", e);
        }
    }

    @Override
    public InputStream downloadFile(String key) {
        AssertUtils.isNotBlank(key, FILE_KEY_IS_NULL);
        try {
            return aliOssUtil.download(key);
        } catch (Exception e) {
            log.error("下载文件[{}]时发生异常：", key, e);
            throw new CommonException("文件下载异常。", e);
        }
    }

    @Override
    public void deleteFile(String key) {
        AssertUtils.isNotBlank(key, FILE_KEY_IS_NULL);
        try {
            aliOssUtil.delete(key);
        } catch (Exception e) {
            log.error("删除文件[{}]时发生异常：", key, e);
            throw new CommonException("删除异常。", e);
        }
    }

    @Override
    public void deleteFiles(List<String> keys) {
        if (CollUtils.isEmpty(keys)) {
            return;
        }
        if (keys.size() > 1000) {
            throw new BadRequestException(FILE_KEY_TOO_MANY);
        }
        try {
            aliOssUtil.deleteFiles(keys);
        } catch (Exception e) {
            log.error("批量删除文件[{}]时发生异常：", keys, e);
            throw new CommonException("删除异常。", e);
        }
    }
}
