package com.xiaodu.common.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 阿里云OSS文件上传工具类
 */
@Slf4j
public class AliOssUtil {

    private final OSS ossClient;
    private final String bucketName;
    private final String endpoint;

    public AliOssUtil(OSS ossClient, String bucketName, String endpoint) {
        this.ossClient = ossClient;
        this.bucketName = bucketName;
        this.endpoint = endpoint;
    }

    /**
     * 文件上传（字节数组）
     *
     * @param bytes      文件字节数组
     * @param objectName 文件名称（存储在OSS的文件名）
     * @return 文件访问URL
     */
    public String upload(byte[] bytes, String objectName) {
        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            log.error("上传文件到OSS失败: {}", objectName, e);
            throw new RuntimeException("文件上传到OSS失败", e);
        }
        return getUrl(objectName);
    }

    /**
     * 上传文件（InputStream + contentType）
     *
     * @param inputStream 文件输入流
     * @param objectName  文件名称
     * @param contentType 文件类型
     * @return 文件访问URL
     */
    public String upload(InputStream inputStream, String objectName, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (contentType != null) {
                metadata.setContentType(contentType);
            }
            ossClient.putObject(bucketName, objectName, inputStream, metadata);
        } catch (Exception e) {
            log.error("上传文件到OSS失败: {}", objectName, e);
            throw new RuntimeException("文件上传到OSS失败", e);
        }
        return getUrl(objectName);
    }

    /**
     * 上传文件（InputStream + contentLength）
     *
     * @param key           文件唯一标识
     * @param inputStream   文件输入流
     * @param contentLength 文件大小
     * @return 文件访问URL
     */
    public String uploadFile(String key, InputStream inputStream, long contentLength) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            PutObjectRequest request = new PutObjectRequest(bucketName, key, inputStream, metadata);
            ossClient.putObject(request);
            return getUrl(key);
        } catch (Exception e) {
            log.error("上传文件到OSS失败: {}", key, e);
            throw new RuntimeException("文件上传到OSS失败", e);
        }
    }

    /**
     * 下载文件
     *
     * @param key 文件唯一标识
     * @return 文件输入流
     */
    public InputStream download(String key) {
        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, key);
            return ossClient.getObject(request).getObjectContent();
        } catch (Exception e) {
            log.error("下载文件失败: {}", key, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }

    /**
     * 删除文件
     *
     * @param key 文件唯一标识
     */
    public void delete(String key) {
        try {
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            log.error("删除文件失败: {}", key, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    /**
     * 批量删除文件
     *
     * @param keys 文件唯一标识集合
     */
    public void deleteFiles(List<String> keys) {
        if (CollUtils.isEmpty(keys)) {
            return;
        }
        try {
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName).withKeys(keys);
            ossClient.deleteObjects(request);
        } catch (Exception e) {
            log.error("批量删除文件失败: {}", keys, e);
            throw new RuntimeException("批量删除文件失败", e);
        }
    }

    /**
     * 生成存储在OSS的文件名
     * 格式：upload/xdxt/2026/01/29/uuid.extension
     *
     * @param originalFilename 原始文件名
     * @return OSS文件名
     */
    public static String generateObjectName(String originalFilename) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "upload/xdxt" + datePath + "/" + uuid + extension;
    }

    private String getUrl(String objectName) {
        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }
}
