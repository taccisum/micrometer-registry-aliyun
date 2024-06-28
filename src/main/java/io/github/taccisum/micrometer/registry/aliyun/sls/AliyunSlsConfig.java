package io.github.taccisum.micrometer.registry.aliyun.sls;

import io.micrometer.core.instrument.step.StepRegistryConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * @author taccisum - liaojinfeng6938@dingtalk.com
 * @since 2024/6/28
 */
public class AliyunSlsConfig extends StepRegistryPropertiesConfigAdapter<AliyunSlsMetricsProperties> implements StepRegistryConfig {
    public AliyunSlsConfig(AliyunSlsMetricsProperties properties) {
        super(properties);
    }

    @Override
    public String prefix() {
        return "management.metrics.export.sls";
    }
}
