package org.ruaux.jdiscogs.data;

import java.util.List;

import org.ruaux.jdiscogs.JDiscogsProperties;
import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class ReleaseWriter extends ItemStreamSupport implements ItemWriter<Release> {

	private ReleaseRepository repository;
	private JDiscogsProperties config;

	public ReleaseWriter(ReleaseRepository repository, JDiscogsProperties config) {
		this.repository = repository;
		this.config = config;
	}

	@Override
	public void write(List<? extends Release> items) throws Exception {
		if (config.noOp()) {
			return;
		}
		repository.saveAll(items);
	}

}
