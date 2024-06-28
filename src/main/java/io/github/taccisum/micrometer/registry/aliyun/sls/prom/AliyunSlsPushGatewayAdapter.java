package io.github.taccisum.micrometer.registry.aliyun.sls.prom;

import io.github.taccisum.micrometer.registry.aliyun.sls.AliyunSlsMetersBuffer;
import io.github.taccisum.micrometer.registry.aliyun.sls.dto.BasicSlsMeterDTO;
import io.github.taccisum.micrometer.registry.aliyun.sls.dto.SlsCounterDTO;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author taccisum - liaojinfeng6938@dingtalk.com
 * @since 2024/6/28
 */
public class AliyunSlsPushGatewayAdapter extends PushGateway {
    private static final String PREFIX = "prom_";
    private AliyunSlsMetersBuffer buffer;

    public AliyunSlsPushGatewayAdapter(AliyunSlsMetersBuffer buffer) {
        super("127.0.0.1");
        Objects.requireNonNull(buffer, "buffer");
        this.buffer = buffer;
    }

    @Override
    public void push(CollectorRegistry registry, String job) throws IOException {
        this.push(registry, job, new HashMap<>());
    }

    @Override
    public void push(CollectorRegistry registry, String job, Map<String, String> groupingKey) throws IOException {
        this.pushAdd(registry, job, groupingKey);
    }

    @Override
    public void pushAdd(CollectorRegistry registry, String job, Map<String, String> groupingKey) throws IOException {
        long now = System.currentTimeMillis();
        Enumeration<Collector.MetricFamilySamples> enumeration = registry.metricFamilySamples();
        while (enumeration.hasMoreElements()) {
            Collector.MetricFamilySamples next = enumeration.nextElement();
            switch (next.type) {
                case HISTOGRAM:
                    for (Collector.MetricFamilySamples.Sample sample : next.samples) {
                        BasicSlsMeterDTO dto = new SlsCounterDTO()
                                .setCount(sample.value)
                                .setTime(Optional.ofNullable(sample.timestampMs).orElse(now))
                                .setName(formatName(sample))
                                .setTags(toTags(sample));
                        this.buffer.put(dto);
                    }
                    break;
                default:
                    break;      // TODO:: support more sample types here
            }
        }
        this.buffer.flush();
    }


    private static String formatName(Collector.MetricFamilySamples.Sample sample) {
        return PREFIX + sample.name;
    }

    private static Map<String, String> toTags(Collector.MetricFamilySamples.Sample sample) {
        Map<String, String> res = new HashMap<>();
        for (int i = 0; i < sample.labelNames.size(); i++) {
            String labelName = sample.labelNames.get(i);
            String labelValue = sample.labelValues.get(i);
            res.put(labelName, labelValue);
        }

        return res;
    }
}
