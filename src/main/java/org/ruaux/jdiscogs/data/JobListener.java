package org.ruaux.jdiscogs.data;

import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.ItemListenerSupport;

import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class JobListener extends ItemListenerSupport<Object, Object> {

    private NumberFormat formatter = NumberFormat.getIntegerInstance();
    private @Setter
    String name;
    private long count = 0;
    private long startTime = System.currentTimeMillis();
    private long lastPrint;

    @Builder
    protected JobListener(String name) {
        this.name = name;
    }

    @Override
    public void afterWrite(List items) {
        count += items.size();
        if ((System.currentTimeMillis() - lastPrint) > 3000) {
            long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
            if (seconds > 0) {
                long itemsPerSecond = count / seconds;
                if (log.isDebugEnabled()) {
                    log.debug("Wrote {} items ({} items/sec)", count, itemsPerSecond);
                } else {
                    log.info("Wrote {} {} ({}/sec)", formatter.format(count), name, formatter.format(itemsPerSecond));
                }
                lastPrint = System.currentTimeMillis();
            }
        }
    }

}
