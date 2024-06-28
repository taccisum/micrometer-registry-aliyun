package io.github.taccisum.micrometer.registry.aliyun.sls.dto;

import com.aliyun.openservices.log.common.LogItem;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author taccisum - liaojinfeng6938@dingtalk.com
 * @since 2024/6/28
 */
@Data
@Accessors(chain = true)
public class SlsGaugeDTO extends BasicSlsMeterDTO {
    private Double value;

    public SlsGaugeDTO() {
        this.setType("gauge");
    }

    @Override
    public LogItem toLogItem() {
        LogItem item = super.toLogItem();
        item.PushBack("value", Double.toString(value));
        return item;
    }

    @Override
    protected double value() {
        return this.value;
    }
}
