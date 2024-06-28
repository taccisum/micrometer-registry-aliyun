package io.github.taccisum.micrometer.registry.aliyun.sls.dto;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.log.common.LogItem;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * @author taccisum - liaojinfeng6938@dingtalk.com
 * @since 2024/6/28
 */
@Data
@Accessors(chain = true)
public abstract class BasicSlsMeterDTO {
    private String name;
    private Map<String, String> tags;
    private String type;
    private Long time;

    /**
     * @return 普通的 sls log store 存储 item
     */
    public LogItem toLogItem() {
        LogItem item = new LogItem();
        item.PushBack("time", this.time.toString());
        item.PushBack("type", this.type);
        item.PushBack("name", this.name);
        item.PushBack("tags", JSON.toJSONString(this.tags));
        return item;
    }

    /**
     * @return sls time series store 存储 item
     */
    public LogItem toMetricsLogItem() {
        return buildMetricsLogItem(this.name, this.tags, this.value(), this.secTime());
    }

    /**
     * @return meter 时间（单位：秒）
     */
    private Integer secTime() {
        return (int) (this.time / 1000);
    }

    protected abstract double value();

    /**
     * @param metricName the metric name, eg: http_requests_count
     * @param labels     labels map, eg: {'idc': 'idc1', 'ip': '192.0.2.0', 'hostname': 'appserver1'}
     * @param value      double value, eg: 1.234
     * @param timeInSec  timestamp(unit: second)
     * @return LogItem for time series store
     */
    static LogItem buildMetricsLogItem(String metricName, Map<String, String> labels, double value, Integer timeInSec) {
        final String labelsKey = "__labels__";
        final String timeKey = "__time_nano__";
        final String valueKey = "__value__";
        final String nameKey = "__name__";
        final LogItem item = new LogItem();
        int timeInSec_ = Optional.ofNullable(timeInSec)
                .orElse((int) (System.currentTimeMillis() / 1000));
        item.SetTime(timeInSec_);
        item.PushBack(timeKey, timeInSec_ + "000000");
        item.PushBack(nameKey, metricName);
        item.PushBack(valueKey, String.valueOf(value));

        // 按照字典序对 labels 排序, 如果您的 labels 已排序, 请忽略此步骤。
        TreeMap<String, String> sortedLabels = new TreeMap<String, String>(labels);
        StringBuilder labelsBuilder = new StringBuilder();

        boolean hasPrev = false;
        for (Map.Entry<String, String> entry : sortedLabels.entrySet()) {
            if (hasPrev) {
                labelsBuilder.append("|");
            }
            hasPrev = true;
            labelsBuilder.append(entry.getKey());
            labelsBuilder.append("#$#");
            labelsBuilder.append(entry.getValue());
        }
        item.PushBack(labelsKey, labelsBuilder.toString());
        return item;
    }
}
