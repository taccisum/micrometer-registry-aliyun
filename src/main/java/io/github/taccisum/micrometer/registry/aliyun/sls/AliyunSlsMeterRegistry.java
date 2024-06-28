package io.github.taccisum.micrometer.registry.aliyun.sls;

import com.alibaba.fastjson.JSON;
import io.github.taccisum.micrometer.registry.aliyun.sls.dto.BasicSlsMeterDTO;
import io.github.taccisum.micrometer.registry.aliyun.sls.dto.SlsCounterDTO;
import io.github.taccisum.micrometer.registry.aliyun.sls.dto.SlsGaugeDTO;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author taccisum - liaojinfeng6938@dingtalk.com
 * @since 2024/6/28
 */
@Slf4j
public class AliyunSlsMeterRegistry extends StepMeterRegistry {
    @Setter
    private AliyunSlsMetersBuffer buffer;

    public AliyunSlsMeterRegistry(AliyunSlsConfig config, Clock clock) {
        super(config, clock);
        start(new NamedThreadFactory("metrics-aliyun-sls-publisher"));
    }

    @Override
    protected void publish() {
        long time = clock.wallTime();
        for (Meter meter : this.getMeters()) {
            Meter.Id id = meter.getId();
            String name = this.getConventionName(id);

            List<BasicSlsMeterDTO> dtos = new ArrayList<>();        // TODO:: 这里有些奇怪，为啥需要这个 dtos
            String type;

            if (meter instanceof Gauge gauge) {
                type = "gauge";
                dtos.add(new SlsGaugeDTO().setValue(gauge.value()));
            } else if (meter instanceof Counter counter) {
                double count = counter.count();
                type = "counter";
                dtos.add(new SlsCounterDTO().setCount(count));
//            } else if (meter instanceof Timer) {
//                Timer timer = (Timer) meter;
//                timer.max(TimeUnit.MILLISECONDS);
//                continue;
            } else {
                log.warn("Ignore yet unsupported meter '{}' of type: {}", id.getName(), meter.getClass());
                continue;
            }
            if (!dtos.isEmpty()) {
                Map<String, String> tags = ls2map(this.getConventionTags(id));
                for (BasicSlsMeterDTO dto : dtos) {
                    dto.setName(name).setType(type).setTags(tags).setTime(time);
                    if (buffer != null) {
                        buffer.put(dto);
                    } else {
                        log.info(JSON.toJSONString(dto));
                    }
                }
            }
        }

        if (buffer != null) buffer.flush();
    }

    private Map<String, String> ls2map(List<Tag> tags) {
        Map<String, String> map = new HashMap<>();
        for (Tag tag : tags) {
            map.put(tag.getKey(), tag.getValue());
        }
        return map;
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }
}
