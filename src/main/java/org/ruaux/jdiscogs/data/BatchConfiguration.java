package org.ruaux.jdiscogs.data;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;

import org.ruaux.jdiscogs.JDiscogsConfiguration;
import org.ruaux.jdiscogs.data.xml.Master;
import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableBatchProcessing
@Slf4j
public class BatchConfiguration {

	private static final String JOBS_KEY = BatchConfiguration.class.getPackageName() + ".jobs";
	private static final Object VALUE_DONE = "done";
	@Autowired
	private JobLauncher launcher;
	@Autowired
	private JobBuilderFactory jobs;
	@Autowired
	private StepBuilderFactory steps;
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
	private TaskExecutor executor;
	@Autowired
	private StringRedisTemplate template;

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
		URI uri = URI.create(config.getData().getUrl().replace("{entity}", entityName));
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
	private <T> Job job(LoadJob job, ItemWriter<T> writer) throws MalformedURLException {
		Class<T> clazz = (Class<T>) job.getJobClass();
		String entityName = clazz.getSimpleName().toLowerCase();
		SimpleStepBuilder<T, T> builder = steps.get(job + "-step").<T, T>chunk(config.getData().getBatchSize());
		builder.reader(getReader(clazz));
		builder.writer(writer);
		builder.listener(new JobListener(entityName));
		builder.taskExecutor(executor);
		return jobs.get(job + "-job").start(builder.build()).build();
	}

	public void runJobs() throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException, MalformedURLException {
		if (config.getData().isSkip()) {
			return;
		}
		for (LoadJob job : config.getData().getJobs()) {
			Object status = template.opsForHash().get(JOBS_KEY, job.name());
			log.info("Job status: {}", status);
			if (!VALUE_DONE.equals(status)) {
				JobExecution execution = launcher.run(loadJob(job), new JobParameters());
				if (execution.getExitStatus().equals(ExitStatus.COMPLETED)) {
					template.opsForHash().put(JOBS_KEY, job.name(), VALUE_DONE);
				}
			}
		}
	}

	private Job loadJob(LoadJob job) throws MalformedURLException {
		switch (job) {
		case MasterDocs:
			return job(job, masterWriter);
		case MasterIndex:
			return job(job, masterIndexWriter);
		case ReleaseDocs:
			return job(job, releaseWriter);
		case ReleaseIndex:
			return job(job, releaseIndexWriter);
		case ReleaseDocsIndex:
			return job(job, new CompositeItemWriterBuilder<Release>()
					.delegates(Arrays.asList(releaseWriter, releaseIndexWriter)).build());
		default:
			return job(job, new CompositeItemWriterBuilder<Master>()
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
