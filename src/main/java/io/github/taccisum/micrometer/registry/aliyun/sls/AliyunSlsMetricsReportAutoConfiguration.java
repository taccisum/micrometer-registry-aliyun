package io.github.taccisum.micrometer.registry.aliyun.sls;

import com.aliyun.openservices.aliyun.log.producer.LogProducer;
import com.aliyun.openservices.aliyun.log.producer.ProducerConfig;
import com.aliyun.openservices.aliyun.log.producer.ProjectConfig;
import io.micrometer.core.instrument.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author taccisum - liaojinfeng6938@dingtalk.com
 * @since 2024/6/28
 */
@Configuration
@ConditionalOnClass(LogProducer.class)
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@EnableConfigurationProperties(AliyunSlsMetricsProperties.class)
public class AliyunSlsMetricsReportAutoConfiguration {
    @Resource
    private AliyunSlsMetricsProperties properties;

    @Bean
    public AliyunSlsConfig aliyunSlsConfig() {
        return new AliyunSlsConfig(properties);
    }

    @Bean
    public AliyunSlsMeterRegistry aliyunSlsMeterRegistry(
            AliyunSlsConfig config,
            Clock clock,
            @Autowired(required = false) AliyunSlsMetersBuffer aliyunSlsMetersBuffer
    ) {
        AliyunSlsMeterRegistry registry = new AliyunSlsMeterRegistry(config, clock);
        registry.setBuffer(aliyunSlsMetersBuffer);
        return registry;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public LogProducer metricsLogProducer() {
        ProducerConfig config = new ProducerConfig();
        LogProducer producer = new LogProducer(config);
        ProjectConfig projectConfig = new ProjectConfig(
                properties.getProject(),
                properties.getEndpoint(),
                properties.getAccessKey(),
                properties.getAccessSecret()
        );
        producer.putProjectConfig(projectConfig);
        return producer;
    }

    @Bean
    @ConditionalOnMissingBean
    public AliyunSlsMetersBuffer aliyunSlsMetersBuffer(
            @Autowired @Qualifier("metricsLogProducer") LogProducer metricsLogProducer
    ) {
        return new AliyunSlsMetersBuffer(metricsLogProducer, properties.getProject(), properties.getLogStore());
    }
}
