package com.xiaodu.media.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.xiaodu.common.utils.AliOssUtil;
import com.xiaodu.media.storage.IFileStorage;
import com.xiaodu.media.storage.ali.AliFileStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AliProperties.class)
public class AliConfig {

    @Bean
    @ConditionalOnProperty(prefix = "xd.platform", name = "file", havingValue = "ALI")
    public OSS aliOssClient(AliProperties prop){
        return new OSSClientBuilder()
                .build(prop.getOss().getEndpoint(), prop.getAccessId(), prop.getAccessKey());
    }

    @Bean
    @ConditionalOnProperty(prefix = "xd.platform", name = "file", havingValue = "ALI")
    public AliOssUtil aliOssUtil(OSS aliOssClient, AliProperties prop) {
        return new AliOssUtil(aliOssClient, prop.getOss().getBucket(), prop.getOss().getEndpoint());
    }

    @Bean
    @ConditionalOnProperty(prefix = "xd.platform", name = "file", havingValue = "ALI")
    public IFileStorage aliFileStorage(AliOssUtil aliOssUtil) {
        return new AliFileStorage(aliOssUtil);
    }
}
