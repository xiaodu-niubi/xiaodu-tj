package com.xiaodu.media.config;

import com.xiaodu.media.enums.Platform;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "xd.platform")
public class PlatformProperties {
    private Platform file;
    private Platform media;
}
