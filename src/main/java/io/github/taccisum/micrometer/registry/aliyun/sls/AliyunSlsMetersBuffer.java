package io.github.taccisum.micrometer.registry.aliyun.sls;

import com.aliyun.openservices.aliyun.log.producer.LogProducer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import io.github.taccisum.micrometer.registry.aliyun.sls.dto.BasicSlsMeterDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;

/**
 * @author taccisum - liaojinfeng6938@dingtalk.com
 * @since 2023/3/29
 */
@Slf4j
public class AliyunSlsMetersBuffer {
    protected static final int THRESHOLD = 1000;

    protected List<BasicSlsMeterDTO> meters = new ArrayList<>();

    private LogProducer logProducer;
    private String project;
    private String logStore;

    public AliyunSlsMetersBuffer(LogProducer logProducer, String project, String logStore) {
        this.logProducer = logProducer;
        this.project = project;
        this.logStore = logStore;
    }

    public void put(BasicSlsMeterDTO dto) {
        meters.add(dto);
        this.cp();
    }

    /**
     * trigger check point once
     */
    private void cp() {
        if (meters.size() > THRESHOLD) {
            this.flush();
        }
    }

    public void flush() {
        if (this.meters.isEmpty()) return;

        ArrayList<LogItem> items = new ArrayList<>();
        final StopWatch sw = new StopWatch();
        int size = this.meters.size();
        log.debug("Start flush meters...");

        sw.start();
        synchronized (this) {
            for (BasicSlsMeterDTO meter : this.meters) {
                items.add(meter.toMetricsLogItem());
            }
            this.meters.clear();
        }
        sw.stop();
        if (items.isEmpty()) return;

        if (log.isDebugEnabled())
            log.debug("Finished conversions of {} meters in {}ms. Send to Aliyun sls now...", size, sw.getLastTaskTimeMillis());

        try {
            sw.start();
            logProducer.send(project, logStore, items, result -> {
                if (result.isSuccessful()) {
                    log.debug("Send meters into Aliyun logstore '{}' of project '{}' successfully.", logStore, project);
                } else {
                    log.warn("Fail to send meters into Aliyun logstore '{}' of project '{}'. err[{}]: {}",
                            logStore, project, result.getErrorCode(), result.getErrorMessage()
                    );
                }

                sw.stop();
                if (log.isDebugEnabled())
                    log.debug("Finished send of {} meters in {}ms.", items.size(), sw.getLastTaskTimeMillis());
            });
        } catch (InterruptedException | ProducerException e) {
            log.warn(String.format("Fail to flush meters into Aliyun logstore '%s' of project '%s'", logStore, project), e);
        }
    }
}
