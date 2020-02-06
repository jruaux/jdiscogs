package org.ruaux.jdiscogs.data;

import java.util.List;

import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;

public class ReleaseWriter extends ItemStreamSupport implements ItemWriter<Release> {

	private ReleaseRepository repository;

	public ReleaseWriter(ReleaseRepository repository) {
		this.repository = repository;
	}

	@Override
	public void write(List<? extends Release> items) throws Exception {
		repository.saveAll(items);
	}

}
