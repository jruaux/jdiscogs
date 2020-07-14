package org.ruaux.jdiscogs;

import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.index.Schema;
import com.redislabs.lettusearch.index.field.NumericField;
import com.redislabs.lettusearch.index.field.PhoneticMatcher;
import com.redislabs.lettusearch.index.field.TagField;
import com.redislabs.lettusearch.index.field.TextField;
import com.redislabs.lettusearch.search.Document;
import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.ruaux.jdiscogs.data.*;
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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.redisearch.RediSearchItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.step.redisearch.IndexCreateStep;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import static org.ruaux.jdiscogs.data.Fields.*;

@Slf4j
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    private final static Schema RELEASE_SCHEMA = Schema.builder().field(TextField.builder().name(ARTIST).sortable(true).build()).field(TagField.builder().name(ID).sortable(true).build()).field(TextField.builder().name(TITLE).sortable(true).build()).build();
    private final static Schema MASTER_SCHEMA = Schema.builder().field(TextField.builder().name(ARTIST).sortable(true).build()).field(TagField.builder().name(ARTIST_ID).sortable(true).build()).field(TagField.builder().name(GENRES).sortable(true).build()).field(TextField.builder().name(TITLE).matcher(PhoneticMatcher.English).sortable(true).build()).field(NumericField.builder().name(YEAR).sortable(true).build()).build();

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobRepository jobRepository;
    private final JDiscogsProperties props;
    private final RedisURI redisURI;
    private final GenericObjectPoolConfig<StatefulRediSearchConnection<String, String>> poolConfig;

    public BatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, JobRepository jobRepository, JDiscogsProperties props, RedisURI redisURI, GenericObjectPoolConfig<StatefulRediSearchConnection<String, String>> poolConfig) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.jobRepository = jobRepository;
        this.props = props;
        this.redisURI = redisURI;
        this.poolConfig = poolConfig;
    }

    @Bean
    Job releases(TaskletStep releaseLoadStep) throws Exception {
        IndexCreateStep<String, String> indexCreateStep = indexCreateStep(props.getData().getReleases().getIndex(), RELEASE_SCHEMA);
        return job("releases", indexCreateStep, releaseLoadStep);
    }

    @Bean
    Job masters(TaskletStep masterLoadStep) throws Exception {
        IndexCreateStep<String, String> indexCreateStep = indexCreateStep(props.getData().getMasters().getIndex(), MASTER_SCHEMA);
        return job("masters", indexCreateStep, masterLoadStep);
    }

    private Job job(String jobName, IndexCreateStep<String, String> indexCreateStep, TaskletStep loadStep) {
        Flow flow = new FlowBuilder<Flow>(jobName + "Flow").start(indexCreateStep).next(loadStep).end();
        return jobBuilderFactory.get(jobName).start(flow).end().build();
    }

    private TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(props.getData().getThreads());
        return taskExecutor;
    }

    private <T> ItemReader<T> reader(Resource resource, String root, Jaxb2Marshaller marshaller) {
        return synchronizedReader(new StaxEventItemReaderBuilder<T>().name(root + "Reader").addFragmentRootElements(root).resource(resource).unmarshaller(marshaller).build());
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

    private IndexCreateStep<String, String> indexCreateStep(String index, Schema schema) throws Exception {
        IndexCreateStep<String, String> step = IndexCreateStep.<String, String>builder().redisURI(redisURI).index(index).schema(schema).ignoreErrors(true).build();
        step.setJobRepository(jobRepository);
        step.afterPropertiesSet();
        return step;
    }

    @Bean
    TaskletStep releaseLoadStep(ReleaseProcessor releaseProcessor, Jaxb2Marshaller marshaller) throws IOException {
        ItemReader<Release> reader = reader(resource(props.getData().getReleases().getUrl()), "release", marshaller);
        ItemWriter<Document<String, String>> writer = writer(props.getData().getReleases().getIndex());
        return loadStep("release", reader, releaseProcessor, writer);
    }

    @Bean
    TaskletStep masterLoadStep(MasterProcessor masterProcessor, Jaxb2Marshaller marshaller) throws IOException {
        ItemReader<Master> reader = reader(resource(props.getData().getMasters().getUrl()), "master", marshaller);
        ItemWriter<Document<String, String>> writer = writer(props.getData().getMasters().getIndex());
        return loadStep("master", reader, masterProcessor, writer);
    }

    private <T> TaskletStep loadStep(String name, ItemReader<T> reader, ItemProcessor<T, Document<String, String>> processor, ItemWriter<Document<String, String>> writer) {
        return stepBuilderFactory.get(name + "LoadStep").<T, Document<String, String>>chunk(props.getData().getBatch()).reader(reader).processor(processor).writer(writer).listener((ItemWriteListener<?>) JobListener.builder().name(name).build()).
                taskExecutor(taskExecutor()).throttleLimit(props.getData().getThreads()).build();
    }

    ItemWriter<Document<String, String>> writer(String index) {
        if (props.getData().isNoOp()) {
            return new NoopWriter<>();
        }
        return RediSearchItemWriter.builder().redisURI(redisURI).poolConfig(poolConfig).index(index).build();
    }

}
