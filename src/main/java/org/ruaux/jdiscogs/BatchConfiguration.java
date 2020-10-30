package org.ruaux.jdiscogs;

import static org.ruaux.jdiscogs.data.Fields.ARTIST;
import static org.ruaux.jdiscogs.data.Fields.ARTIST_ID;
import static org.ruaux.jdiscogs.data.Fields.GENRES;
import static org.ruaux.jdiscogs.data.Fields.ID;
import static org.ruaux.jdiscogs.data.Fields.TITLE;
import static org.ruaux.jdiscogs.data.Fields.YEAR;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.ruaux.jdiscogs.data.JobListener;
import org.ruaux.jdiscogs.data.MasterToMapConverter;
import org.ruaux.jdiscogs.data.NoopWriter;
import org.ruaux.jdiscogs.data.ReleaseToMapConverter;
import org.ruaux.jdiscogs.model.Master;
import org.ruaux.jdiscogs.model.Release;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.redis.RedisHashItemWriter;
import org.springframework.batch.item.redis.support.KeyMaker;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.step.redisearch.IndexCreateStep;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.index.CreateOptions;
import com.redislabs.lettusearch.index.Schema;
import com.redislabs.lettusearch.index.field.NumericField;
import com.redislabs.lettusearch.index.field.PhoneticMatcher;
import com.redislabs.lettusearch.index.field.TagField;
import com.redislabs.lettusearch.index.field.TextField;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	private final static Schema<String> RELEASE_SCHEMA = Schema.<String>builder()
			.field(TextField.<String>builder().name(ARTIST).sortable(true).build())
			.field(TagField.<String>builder().name(ID).sortable(true).build())
			.field(TextField.<String>builder().name(TITLE).sortable(true).build()).build();
	private final static Schema<String> MASTER_SCHEMA = Schema.<String>builder()
			.field(TextField.<String>builder().name(ARTIST).sortable(true).build())
			.field(TagField.<String>builder().name(ARTIST_ID).sortable(true).build())
			.field(TagField.<String>builder().name(GENRES).sortable(true).build())
			.field(TextField.<String>builder().name(TITLE).matcher(PhoneticMatcher.English).sortable(true).build())
			.field(NumericField.<String>builder().name(YEAR).sortable(true).build()).build();

	private final JobBuilderFactory jobBuilderFactory;
	private final StepBuilderFactory stepBuilderFactory;
	private final JobRepository jobRepository;
	private final JDiscogsProperties props;
	private final RedisURI redisURI;
	private final GenericObjectPoolConfig<StatefulRediSearchConnection<String, String>> poolConfig;

	public BatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
			JobRepository jobRepository, JDiscogsProperties props, RedisURI redisURI,
			GenericObjectPoolConfig<StatefulRediSearchConnection<String, String>> poolConfig) {
		this.jobBuilderFactory = jobBuilderFactory;
		this.stepBuilderFactory = stepBuilderFactory;
		this.jobRepository = jobRepository;
		this.props = props;
		this.redisURI = redisURI;
		this.poolConfig = poolConfig;
	}

	@Bean
	Job releases(TaskletStep releaseLoadStep) throws Exception {
		IndexCreateStep<String, String> indexCreateStep = indexCreateStep(props.getReleases().getIndex(),
				props.getReleases().getPrefix(), RELEASE_SCHEMA);
		return job("releases", indexCreateStep, releaseLoadStep);
	}

	@Bean
	Job masters(TaskletStep masterLoadStep) throws Exception {
		IndexCreateStep<String, String> indexCreateStep = indexCreateStep(props.getMasters().getIndex(),
				props.getMasters().getPrefix(), MASTER_SCHEMA);
		return job("masters", indexCreateStep, masterLoadStep);
	}

	private Job job(String jobName, IndexCreateStep<String, String> indexCreateStep, TaskletStep loadStep) {
		Flow flow = new FlowBuilder<Flow>(jobName + "Flow").start(indexCreateStep).next(loadStep).end();
		return jobBuilderFactory.get(jobName).start(flow).end().build();
	}

	private TaskExecutor taskExecutor() {
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(props.getThreads());
		return taskExecutor;
	}

	private <T> ItemReader<T> reader(Resource resource, String root, Jaxb2Marshaller marshaller) {
		return synchronizedReader(new StaxEventItemReaderBuilder<T>().name(root + "Reader")
				.addFragmentRootElements(root).resource(resource).unmarshaller(marshaller).build());
	}

	private <T> SynchronizedItemStreamReader<T> synchronizedReader(ItemStreamReader<T> reader) {
		return new SynchronizedItemStreamReaderBuilder<T>().delegate(reader).build();
	}

	private Resource resource(String urlString) throws IOException {
		URI uri = URI.create(urlString);
		Resource resource = getResource(uri);
		if (uri.getPath().endsWith(".gz")) {
			return new GZIPResource(resource);
		}
		return resource;
	}

	private Resource getResource(URI uri) throws MalformedURLException {
		if (uri.isAbsolute()) {
			return new UrlResource(uri);
		}
		return new FileSystemResource(uri.toString());
	}

	private IndexCreateStep<String, String> indexCreateStep(String index, String prefix, Schema<String> schema)
			throws Exception {
		CreateOptions<String, String> createOptions = CreateOptions.<String, String>builder()
				.prefixes(prefix + props.getKeySeparator()).build();
		IndexCreateStep<String, String> step = IndexCreateStep.builder().redisURI(redisURI).index(index).schema(schema)
				.createOptions(createOptions).ignoreErrors(true).build();
		step.setJobRepository(jobRepository);
		step.afterPropertiesSet();
		return step;
	}

	@SuppressWarnings("unchecked")
	@Bean
	TaskletStep releaseLoadStep(Jaxb2Marshaller marshaller) throws IOException {
		ItemReader<Release> reader = reader(resource(props.getReleases().getUrl()), "release", marshaller);
		ItemWriter<Release> writer = writer(
				KeyMaker.<Release>builder().prefix(props.getReleases().getPrefix()).extractors(Release::getId).build(),
				new ReleaseToMapConverter());
		return loadStep("release", reader, writer);
	}

	@SuppressWarnings("unchecked")
	@Bean
	TaskletStep masterLoadStep(Jaxb2Marshaller marshaller) throws IOException {
		ItemReader<Master> reader = reader(resource(props.getMasters().getUrl()), "master", marshaller);
		ItemWriter<Master> writer = writer(
				KeyMaker.<Master>builder().prefix(props.getMasters().getPrefix()).extractors(Master::getId).build(),
				new MasterToMapConverter(props));
		return loadStep("master", reader, writer);
	}

	private <T> TaskletStep loadStep(String name, ItemReader<T> reader, ItemWriter<T> writer) {
		return stepBuilderFactory.get(name + "-load-step").<T, T>chunk(props.getBatch()).reader(reader).writer(writer)
				.listener((ItemWriteListener<?>) JobListener.builder().name(name).build()).taskExecutor(taskExecutor())
				.throttleLimit(props.getThreads()).build();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	<T> ItemWriter<T> writer(Converter<T, String> keyConverter, Converter<T, Map<String, String>> mapConverter) {
		if (props.isNoOp()) {
			return new NoopWriter<>();
		}
		GenericObjectPoolConfig<StatefulConnection<String, String>> config = (GenericObjectPoolConfig) poolConfig;
		return RedisHashItemWriter.<T>builder().uri(redisURI).keyConverter(keyConverter).mapConverter(mapConverter)
				.poolConfig(config).build();
	}

}
