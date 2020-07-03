package org.ruaux.jdiscogs.data;

import java.util.List;

import org.springframework.batch.item.ItemWriter;

public class NoopWriter<T> implements ItemWriter<T> {

	@Override
	public void write(List<? extends T> items) {
		// do nothing
	}

}
