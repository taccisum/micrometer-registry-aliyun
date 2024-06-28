package io.github.taccisum.micrometer.registry.aliyun.sls;

import lombok.Data;
import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author taccisum - liaojinfeng6938@dingtalk.com
 * @since 2024/6/28
 */
@Data
@ConfigurationProperties(prefix = "management.metrics.export.sls")
public class AliyunSlsMetricsProperties extends StepRegistryProperties {
    /**
     * 阿里云 RAM AK
     */
    private String accessKey;
    /**
     * 阿里云 RAM SK
     */
    private String accessSecret;
    /**
     * 接入点
     */
    private String endpoint;
    /**
     * SLS Project
     */
    private String project;
    /**
     * SLS Log Store
     */
    private String logStore;
}
