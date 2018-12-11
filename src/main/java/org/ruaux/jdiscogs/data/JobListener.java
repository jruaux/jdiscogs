package org.ruaux.jdiscogs.data;

import java.util.List;

import org.springframework.batch.core.ItemWriteListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobListener implements ItemWriteListener<Object> {

	private long count = 0;
	private String entity;

	public JobListener(String entity) {
		this.entity = entity;
	}

	@Override
	public void afterWrite(List<? extends Object> items) {
		count += items.size();
		log.info("Wrote {} {} items", count, entity);
	}

	@Override
	public void beforeWrite(List<? extends Object> items) {
	}

	@Override
	public void onWriteError(Exception exception, List<? extends Object> items) {
	}

}
