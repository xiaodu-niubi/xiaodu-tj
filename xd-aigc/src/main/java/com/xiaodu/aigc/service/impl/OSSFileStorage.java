package com.xiaodu.aigc.service.impl;

import com.xiaodu.aigc.service.FileStorage;
import com.xiaodu.common.exceptions.BadRequestException;
import com.xiaodu.common.exceptions.CommonException;
import com.xiaodu.common.utils.AliOssUtil;
import com.xiaodu.common.utils.AssertUtils;
import com.xiaodu.common.utils.CollUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

import static com.xiaodu.aigc.constants.FileErrorInfo.Msg.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OSSFileStorage implements FileStorage {

    private final AliOssUtil aliOssUtil;

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
