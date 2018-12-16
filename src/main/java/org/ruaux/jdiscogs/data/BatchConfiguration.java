package org.ruaux.jdiscogs.data;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;

import org.ruaux.jdiscogs.JDiscogsConfiguration;
import org.ruaux.jdiscogs.data.xml.Master;
import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
	@Autowired
	private TaskExecutor taskExecutor;
	@Autowired
	private JobLauncher jobLauncher;

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

	@SuppressWarnings("unchecked")
	private <T> Job getJob(LoadJob job, ItemWriter<T> writer) throws MalformedURLException {
		Class<T> clazz = (Class<T>) job.getJobClass();
		String entityName = clazz.getSimpleName().toLowerCase();
		TaskletStep loadStep = stepBuilderFactory.get(job.name() + "-step").<T, T>chunk(config.getBatchSize())
				.reader(getReader(clazz)).writer(getWriter(writer)).listener(new JobListener(entityName))
				.taskExecutor(taskExecutor).build();
		return jobBuilderFactory.get(job.name() + "-job").incrementer(new RunIdIncrementer()).flow(loadStep).end()
				.build();
	}

	private <T> ItemWriter<T> getWriter(ItemWriter<T> writer) {
		if (config.isNoop()) {
			return new NoopItemWriter<T>();
		}
		return writer;
	}

	public void runJobs() throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException, MalformedURLException {
		for (LoadJob job : config.getJobs()) {
			jobLauncher.run(getLoadJob(job), new JobParameters());
		}
	}

	public Job getLoadJob(LoadJob job) throws MalformedURLException {
		switch (job) {
		case MasterDocs:
			return getJob(job, masterWriter);
		case MasterIndex:
			return getJob(job, masterIndexWriter);
		case ReleaseDocs:
			return getJob(job, releaseWriter);
		case ReleaseIndex:
			return getJob(job, releaseIndexWriter);
		case ReleaseDocsIndex:
			return getJob(job, new CompositeItemWriterBuilder<Release>()
					.delegates(Arrays.asList(releaseWriter, releaseIndexWriter)).build());
		default:
			return getJob(job, new CompositeItemWriterBuilder<Master>()
					.delegates(Arrays.asList(masterWriter, masterIndexWriter)).build());
		}
	}

	public enum LoadJob {
		MasterDocs(Master.class), MasterIndex(Master.class), MasterDocsIndex(Master.class), ReleaseDocs(Release.class),
		ReleaseIndex(Release.class), ReleaseDocsIndex(Release.class);

		private Class<?> clazz;

		private LoadJob(Class<?> clazz) {
			this.clazz = clazz;
		}

		public Class<?> getJobClass() {
			return clazz;
		}

	}

}
