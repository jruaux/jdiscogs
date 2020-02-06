package org.ruaux.jdiscogs.data;

import java.util.List;

import org.springframework.batch.item.ItemWriter;

public class NoopWriter implements ItemWriter<Object> {

	@Override
	public void write(List<?> items) throws Exception {
		// do nothing
	}

}
