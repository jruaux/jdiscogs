package org.ruaux.jdiscogs.data;

import java.util.List;

import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReleaseWriter extends ItemStreamSupport implements ItemWriter<Release> {

	@Autowired
	private ReleaseRepository repository;

	@Override
	public void write(List<? extends Release> items) throws Exception {
		repository.saveAll(items);
	}

}
