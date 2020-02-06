package org.ruaux.jdiscogs.data;

import java.util.List;

import org.ruaux.jdiscogs.data.xml.Master;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;

public class MasterWriter extends ItemStreamSupport implements ItemWriter<Master> {

	private MasterRepository repository;

	public MasterWriter(MasterRepository repository) {
		this.repository = repository;
	}

	@Override
	public void write(List<? extends Master> items) throws Exception {
		repository.saveAll(items);
	}

}
