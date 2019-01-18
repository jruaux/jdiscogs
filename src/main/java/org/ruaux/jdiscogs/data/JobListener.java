package org.ruaux.jdiscogs.data;

import java.text.NumberFormat;
import java.util.List;

import org.springframework.batch.core.ItemWriteListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobListener implements ItemWriteListener<Object> {

	private long count = 0;
	private long startTime = System.currentTimeMillis();
	private String entity;
	private NumberFormat formatter = NumberFormat.getIntegerInstance();

	public JobListener(String entity) {
		this.entity = entity;
	}

	@Override
	public void afterWrite(List<? extends Object> items) {
		count += items.size();
		double elapsedTimeInSeconds = (double) (System.currentTimeMillis() - startTime) / 1000;
		long itemsPerSecond = Math.round(count / elapsedTimeInSeconds);
		if (log.isDebugEnabled()) {
			log.debug("Wrote {} items ({} items/sec)", count, itemsPerSecond);
		} else {
			System.out.print(String.format("\rWrote %s %s items (%s items/sec)", formatter.format(count), entity,
					formatter.format(itemsPerSecond)));
		}
	}

	@Override
	public void beforeWrite(List<? extends Object> items) {
	}

	@Override
	public void onWriteError(Exception exception, List<? extends Object> items) {
	}

}
