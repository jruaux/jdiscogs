package org.ruaux.jdiscogs.data;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.ruaux.jdiscogs.data.xml.Master;
import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.redislabs.lettusearch.StatefulRediSearchConnection;

@Configuration
@EnableConfigurationProperties(JDiscogsBatchProperties.class)
@EnableBatchProcessing
public class JDiscogsBatchConfiguration implements InitializingBean {

	private static final String JOBS_KEY = JDiscogsBatchConfiguration.class.getPackage().getName() + ".jobs";
	private static final String VALUE_DONE = "done";
	private JDiscogsBatchProperties props;
	private ResourcelessTransactionManager transactionManager;
	private JobRepository jobRepository;
	private GenericObjectPool<StatefulRediSearchConnection<String, String>> pool;
	private ReleaseCodec codec;

	public JDiscogsBatchConfiguration(JDiscogsBatchProperties props,
			GenericObjectPool<StatefulRediSearchConnection<String, String>> pool, ReleaseCodec codec) {
		this.props = props;
		this.pool = pool;
		this.codec = codec;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.transactionManager = new ResourcelessTransactionManager();
		MapJobRepositoryFactoryBean jobRepositoryFactory = new MapJobRepositoryFactoryBean(transactionManager);
		jobRepositoryFactory.afterPropertiesSet();
		this.jobRepository = jobRepositoryFactory.getObject();
	}

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
		URI uri = URI.create(props.getDataUrl().replace("{entity}", entityName));
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
	private <T> Job job(LoadJob job, ItemWriter<T> writer) throws Exception {
		StepBuilderFactory stepBuilderFactory = new StepBuilderFactory(jobRepository, transactionManager);
		Class<T> clazz = (Class<T>) job.getJobClass();
		String entityName = clazz.getSimpleName().toLowerCase();
		SimpleStepBuilder<T, T> builder = stepBuilderFactory.get(job + "-step").<T, T>chunk(props.getBatchSize());
		builder.reader(getReader(clazz));
		builder.writer(writer);
		if (props.getThreads() > 1) {
			SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
			executor.setConcurrencyLimit(props.getThreads());
			builder.taskExecutor(executor);
			builder.throttleLimit(props.getThreads());
		}
		builder.listener(new JobListener(entityName));
		JobBuilderFactory jobBuilderFactory = new JobBuilderFactory(jobRepository);
		return jobBuilderFactory.get(job + "-job").start(builder.build()).build();
	}

	public void run(LoadJob... jobs) throws Exception {
		for (LoadJob job : jobs) {
			String name = job.name();
			try (StatefulRediSearchConnection<String, String> connection = pool.borrowObject()) {
				String status = connection.sync().hget(JOBS_KEY, name);
				if (VALUE_DONE.equals(status) && !props.isForceLoad()) {
					continue;
				}
				SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
				jobLauncher.setJobRepository(jobRepository);
				jobLauncher.setTaskExecutor(new SyncTaskExecutor());
				jobLauncher.afterPropertiesSet();
				JobExecution execution = jobLauncher.run(job(job, writer(job)), new JobParameters());
				if (execution.getExitStatus().equals(ExitStatus.COMPLETED)) {
					connection.sync().hset(JOBS_KEY, name, VALUE_DONE);
				}
			}
		}
	}

	private ItemWriter<?> writer(LoadJob job) {
		if (props.isNoOp()) {
			return new NoopWriter();
		}
		switch (job) {
		case Releases:
			return new ReleaseWriter(props, pool, codec);
		default:
			return new MasterWriter(props, pool);
		}
	}

	public enum LoadJob {
		Masters(Master.class), Releases(Release.class);

		private Class<?> clazz;

		private LoadJob(Class<?> clazz) {
			this.clazz = clazz;
		}

		public Class<?> getJobClass() {
			return clazz;
		}

	}

}
