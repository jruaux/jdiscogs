package org.ruaux.jdiscogs.data;

import java.util.List;

import org.ruaux.jdiscogs.JDiscogsConfiguration;
import org.ruaux.jdiscogs.data.xml.Master;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MasterWriter extends ItemStreamSupport implements ItemWriter<Master> {

	@Autowired
	private MasterRepository repository;
	@Autowired
	private JDiscogsConfiguration config;

	@Override
	public void write(List<? extends Master> items) throws Exception {
		if (config.getData().isNoOp()) {
			return;
		}
		repository.saveAll(items);
	}

}
