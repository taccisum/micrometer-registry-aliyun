package io.github.taccisum.micrometer.registry.aliyun.sls;

import com.aliyun.openservices.aliyun.log.producer.LogProducer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import io.github.taccisum.micrometer.registry.aliyun.sls.dto.BasicSlsMeterDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云 SLS 指标缓冲区，用于缓冲 micrometer registry 记录的指标，批量上报，以减少网络 I/O 次数，提升性能
 *
 * @author taccisum - liaojinfeng6938@dingtalk.com
 * @since 2024/6/28
 */
@Slf4j
public class AliyunSlsMetersBuffer implements Closeable {
    protected static final int THRESHOLD = 1000;

    protected List<BasicSlsMeterDTO> meters = new ArrayList<>();

    /**
     * SLS Client
     */
    private final LogProducer logProducer;
    /**
     * 目标 Project
     */
    private final String project;
    /**
     * 目标 LogStore
     */
    private final String logStore;

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
            this.clear();
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

    public void clear() {
        if (this.meters != null) this.meters.clear();
    }

    @Override
    public void close() throws IOException {
        try {
            if (this.logProducer != null) this.logProducer.close();
        } catch (InterruptedException | ProducerException e) {
            throw new RuntimeException(e);
        }
    }
}
