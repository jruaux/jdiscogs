package org.ruaux.jdiscogs.data;

import java.util.List;

import org.ruaux.jdiscogs.JDiscogsProperties;
import org.ruaux.jdiscogs.data.xml.Master;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class MasterWriter extends ItemStreamSupport implements ItemWriter<Master> {

	private MasterRepository repository;
	private JDiscogsProperties props;

	public MasterWriter(MasterRepository repository, JDiscogsProperties props) {
		this.repository = repository;
		this.props = props;
	}

	@Override
	public void write(List<? extends Master> items) throws Exception {
		if (props.noOp()) {
			return;
		}
		repository.saveAll(items);
	}

}
