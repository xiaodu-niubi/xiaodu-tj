package com.xiaodu.message.thirdparty.ali;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "xd.sms.ali")
public class AliProperties {
    private String accessId;
    private String accessSecret;
}
