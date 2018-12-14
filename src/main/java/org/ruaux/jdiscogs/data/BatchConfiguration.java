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
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
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
		return synchronizedReader(new StaxEventItemReaderBuilder<T>().name(entityName + "-reader")
				.addFragmentRootElements(entityName).resource(resource(entityName)).unmarshaller(marshaller).build());
	}

	private <T> SynchronizedItemStreamReader<T> synchronizedReader(ItemStreamReader<T> reader) {
		return new SynchronizedItemStreamReaderBuilder<T>().delegate(reader).build();
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

	@Bean
	public TaskExecutor taskExecutor() {
		SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor("spring-batch");
		asyncTaskExecutor.setConcurrencyLimit(config.getConcurrencyLimit());
		return asyncTaskExecutor;
	}

	private <T> Job getJob(String jobName, String stepName, Class<T> clazz, ItemWriter<T> writer)
			throws MalformedURLException {
		TaskletStep loadStep = stepBuilderFactory.get(stepName).<T, T>chunk(config.getBatchSize())
				.reader(getReader(clazz)).writer(getWriter(writer)).listener(new JobListener(stepName + "-listener"))
				.taskExecutor(taskExecutor()).build();
		return jobBuilderFactory.get(jobName).incrementer(new RunIdIncrementer()).flow(loadStep).end().build();
	}

	private <T> ItemWriter<T> getWriter(ItemWriter<T> writer) {
		if (config.isNoopWriters()) {
			return new NoopItemWriter<T>();
		}
		return null;
	}

	public Job getReleaseLoadJob() throws MalformedURLException {
		return getJob("release-load-job", "release-load-step", Release.class, releaseWriter);
	}

	public Job getReleaseIndexJob() throws MalformedURLException {
		return getJob("release-index-job", "release-index-step", Release.class, releaseIndexWriter);
	}

	public Job getReleaseIndexAndLoadJob() throws MalformedURLException {
		return getJob("release-index-and-load-job", "release-index-and-load-step", Release.class,
				new CompositeItemWriterBuilder<Release>().delegates(Arrays.asList(releaseIndexWriter, releaseWriter))
						.build());
	}

	public Job getMasterLoadJob() throws MalformedURLException {
		return getJob("master-load-job", "master-load-step", Master.class, masterWriter);
	}

	public Job getMasterIndexJob() throws MalformedURLException {
		return getJob("master-index-job", "master-index-step", Master.class, masterIndexWriter);
	}

}
