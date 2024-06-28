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
public class SlsCounterDTO extends BasicSlsMeterDTO {
    private double count;

    public SlsCounterDTO() {
        this.setType("counter");
    }

    @Override
    public LogItem toLogItem() {
        LogItem item = super.toLogItem();
        item.PushBack("count", Double.toString(count));
        return item;
    }

    @Override
    protected double value() {
        return this.count;
    }
}
