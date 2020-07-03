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

    private final NumberFormat formatter = NumberFormat.getIntegerInstance();
    private final long startTime = System.currentTimeMillis();
    @Setter
    private String name;
    private long count = 0;
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
                log.info("Wrote {} {} items ({}/sec)", formatter.format(count), name, formatter.format(itemsPerSecond));
                lastPrint = System.currentTimeMillis();
            }
        }
    }

}
