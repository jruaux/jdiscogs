package org.ruaux.jdiscogs.data;

import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.batch.core.ItemWriteListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobListener implements ItemWriteListener<Object> {

	private long count = 0;
	private long startTime = System.currentTimeMillis();
	private String entity;
	private NumberFormat formatter = NumberFormat.getIntegerInstance();
	private long lastPrint;

	public JobListener(String entity) {
		this.entity = entity;
	}

	@Override
	public void afterWrite(List<? extends Object> items) {
		count += items.size();
		if ((System.currentTimeMillis() - lastPrint) > 3000) {
			long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
			if (seconds > 0) {
				long itemsPerSecond = count / seconds;
				if (log.isDebugEnabled()) {
					log.debug("Wrote {} items ({} items/sec)", count, itemsPerSecond);
				} else {
					log.info("Wrote {} {} items ({} items/sec)", formatter.format(count), entity,
							formatter.format(itemsPerSecond));
				}
				lastPrint = System.currentTimeMillis();
			}
		}
	}

	@Override
	public void beforeWrite(List<? extends Object> items) {
	}

	@Override
	public void onWriteError(Exception exception, List<? extends Object> items) {
	}

}
