package org.ruaux.jdiscogs.data;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;

import org.ruaux.jdiscogs.JDiscogsConfiguration;
import org.ruaux.jdiscogs.data.xml.Master;
import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;
	@Autowired
	public StepBuilderFactory stepBuilderFactory;
	@Autowired
	private MasterWriter masterWriter;
	@Autowired
	private ReleaseWriter releaseWriter;
	@Autowired
	private MasterIndexWriter masterIndexWriter;
	@Autowired
	private ReleaseIndexWriter releaseIndexWriter;
	@Autowired
	private JDiscogsConfiguration config;

	private <T> ItemReader<T> getReader(Class<T> entityClass) throws MalformedURLException {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(entityClass);
		String entityName = entityClass.getSimpleName().toLowerCase();
		return new StaxEventItemReaderBuilder<T>().name(entityName + "-reader").addFragmentRootElements(entityName)
				.resource(resource(entityName)).unmarshaller(marshaller).build();
	}

	private Resource resource(String entityName) throws MalformedURLException {
		URI uri = URI.create(config.getFileUrlTemplate().replace("{entity}", entityName));
		Resource resource = getResource(uri);
		if (uri.getPath().endsWith(".gz")) {
			try {
				return new GZIPResource(resource);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return resource;
	}

	private Resource getResource(URI uri) throws MalformedURLException {
		if (uri.isAbsolute()) {
			return new UrlResource(uri);
		}
		return new FileSystemResource(uri.toString());
	}

	public Job getReleaseLoadJob() throws MalformedURLException {
		TaskletStep loadStep = stepBuilderFactory.get("release-load-step")
				.<Release, Release>chunk(config.getBatchSize()).reader(getReader(Release.class)).writer(releaseWriter)
				.listener(new JobListener("release")).build();
		return jobBuilderFactory.get("release-load-job").incrementer(new RunIdIncrementer()).flow(loadStep).end()
				.build();
	}

	public Job getReleaseIndexJob() throws MalformedURLException {
		TaskletStep loadStep = stepBuilderFactory.get("release-index-step")
				.<Release, Release>chunk(config.getBatchSize()).reader(getReader(Release.class))
				.writer(releaseIndexWriter).listener(new JobListener("release")).build();
		return jobBuilderFactory.get("release-index-job").incrementer(new RunIdIncrementer()).flow(loadStep).end()
				.build();
	}

	public Job getReleaseIndexAndLoadJob() throws MalformedURLException {
		CompositeItemWriterBuilder<Release> compositeBuilder = new CompositeItemWriterBuilder<Release>();
		compositeBuilder.delegates(Arrays.asList(releaseIndexWriter, releaseWriter));
		TaskletStep loadStep = stepBuilderFactory.get("release-index-and-load-step")
				.<Release, Release>chunk(config.getBatchSize()).reader(getReader(Release.class))
				.writer(compositeBuilder.build()).listener(new JobListener("release")).build();
		return jobBuilderFactory.get("release-index-and-load-job").incrementer(new RunIdIncrementer()).flow(loadStep)
				.end().build();
	}

	public Job getMasterLoadJob() throws MalformedURLException {
		TaskletStep loadStep = stepBuilderFactory.get("master-load-step").<Master, Master>chunk(config.getBatchSize())
				.reader(getReader(Master.class)).writer(masterWriter).listener(new JobListener("master")).build();
		return jobBuilderFactory.get("master-load-job").incrementer(new RunIdIncrementer()).flow(loadStep).end()
				.build();
	}

	public Job getMasterIndexJob() throws MalformedURLException {
		TaskletStep loadStep = stepBuilderFactory.get("master-index-step").<Master, Master>chunk(config.getBatchSize())
				.reader(getReader(Master.class)).writer(masterIndexWriter).listener(new JobListener("master")).build();
		return jobBuilderFactory.get("master-index-job").incrementer(new RunIdIncrementer()).flow(loadStep).end()
				.build();
	}

}
