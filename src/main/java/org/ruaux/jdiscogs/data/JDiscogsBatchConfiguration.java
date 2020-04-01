package org.ruaux.jdiscogs.data;

import com.redislabs.lettusearch.RediSearchClient;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.index.Schema;
import com.redislabs.lettusearch.index.field.NumericField;
import com.redislabs.lettusearch.index.field.PhoneticMatcher;
import com.redislabs.lettusearch.index.field.TagField;
import com.redislabs.lettusearch.index.field.TextField;
import com.redislabs.lettusearch.search.Document;
import com.redislabs.springredisearch.RediSearchAutoConfiguration;
import org.ruaux.jdiscogs.data.model.Master;
import org.ruaux.jdiscogs.data.model.Release;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.redisearch.DocumentItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.step.redisearch.IndexCreateStep;
import org.springframework.batch.step.redisearch.IndexDropStep;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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

@Configuration
@EnableConfigurationProperties(JDiscogsBatchProperties.class)
@EnableBatchProcessing
@Import({RediSearchAutoConfiguration.class, XmlCodec.class})
public class JDiscogsBatchConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobRepository jobRepository;
    private JDiscogsBatchProperties props;
    private StatefulRediSearchConnection<String, String> connection;

    public JDiscogsBatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, JobRepository jobRepository, JDiscogsBatchProperties props, StatefulRediSearchConnection<String, String> connection) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.jobRepository = jobRepository;
        this.props = props;
        this.connection = connection;
    }

    @Bean
    Resource releasesResource() throws IOException {
        return resource("releases");
    }

    @Bean
    Resource mastersResource() throws IOException {
        return resource("masters");
    }

    @Bean
    ItemReader<Release> releaseItemReader(Resource releasesResource) {
        return reader(Release.class, releasesResource, "release");
    }

    @Bean
    ItemReader<Master> masterItemReader(Resource mastersResource) {
        return reader(Master.class, mastersResource, "master");
    }

    @Bean
    Schema releaseSchema() {
        return Schema.builder().field(TextField.builder().name(ARTIST).sortable(true).build())
                .field(TagField.builder().name(ID).sortable(true).build())
                .field(TextField.builder().name(TITLE).sortable(true).build()).build();
    }

    @Bean
    Schema masterSchema() {
        return Schema.builder().field(TextField.builder().name(ARTIST).sortable(true).build())
                .field(TagField.builder().name(ARTIST_ID).sortable(true).build())
                .field(TagField.builder().name(GENRES).sortable(true).build())
                .field(TextField.builder().name(TITLE).matcher(PhoneticMatcher.English).sortable(true).build())
                .field(NumericField.builder().name(YEAR).sortable(true).build()).build();
    }


    private <T> ItemReader<T> reader(Class<T> entityClass, Resource resource, String root) {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(entityClass);
        return synchronizedReader(new StaxEventItemReaderBuilder<T>().name(root + "-reader")
                .addFragmentRootElements(root).resource(resource).unmarshaller(marshaller).build());
    }

    private <T> SynchronizedItemStreamReader<T> synchronizedReader(ItemStreamReader<T> reader) {
        return new SynchronizedItemStreamReaderBuilder<T>().delegate(reader).build();
    }

    private Resource resource(String entity) throws IOException {
        URI uri = URI.create(props.getDataUrl().replace("{entity}", entity));
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

    @Bean
    IndexLoadDecider releaseIndexLoadDecider() {
        return IndexLoadDecider.builder().connection(connection).index(props.getReleaseIndex()).range(props.getReleaseItemCount()).build();
    }

    @Bean
    IndexLoadDecider masterIndexLoadDecider() {
        return IndexLoadDecider.builder().connection(connection).index(props.getMasterIndex()).range(props.getMasterItemCount()).build();
    }

    @Bean
    IndexDropStep releaseIndexDropStep() {
        return indexDropStep(props.getReleaseIndex());
    }

    @Bean
    IndexDropStep masterIndexDropStep() {
        return indexDropStep(props.getMasterIndex());
    }

    private IndexDropStep indexDropStep(String index) {
        return IndexDropStep.builder().ignoreErrors(true).jobRepository(jobRepository).name(index + "-index-drop-step").connection(connection).index(index).build();
    }


    @Bean
    IndexCreateStep releaseIndexCreateStep(Schema releaseSchema) {
        return indexCreateStep(props.getReleaseIndex(), releaseSchema);
    }

    @Bean
    IndexCreateStep masterIndexCreateStep(Schema masterSchema) {
        return indexCreateStep(props.getMasterIndex(), masterSchema);
    }

    private IndexCreateStep indexCreateStep(String index, Schema schema) {
        return IndexCreateStep.builder().jobRepository(jobRepository).name(index + "-index-create-step").connection(connection).index(index)
                .schema(schema).build();
    }

    @Bean
    Job releaseLoadJob(IndexLoadDecider releaseIndexLoadDecider, IndexDropStep releaseIndexDropStep, IndexCreateStep releaseIndexCreateStep, TaskletStep releaseLoadStep) {
        return job("release-load-job", releaseIndexLoadDecider, releaseIndexDropStep, releaseIndexCreateStep, releaseLoadStep);
    }

    @Bean
    Job masterLoadJob(IndexLoadDecider masterIndexLoadDecider, IndexDropStep masterIndexDropStep, IndexCreateStep masterIndexCreateStep, TaskletStep masterLoadStep) {
        return job("master-load-job", masterIndexLoadDecider, masterIndexDropStep, masterIndexCreateStep, masterLoadStep);
    }

    private Job job(String name, IndexLoadDecider decider, IndexDropStep indexDropStep, IndexCreateStep indexCreateStep, TaskletStep loadStep) {
        Flow flow = new FlowBuilder<Flow>("loadFlow")
                .start(decider)
                .on(IndexLoadDecider.YES)
                .to(indexDropStep).next(indexCreateStep).next(loadStep)
                .end();
        return jobBuilderFactory.get(name).incrementer(new RunIdIncrementer()).start(flow).end().build();
    }

    @Bean
    TaskletStep releaseLoadStep(ItemReader<Release> releaseItemReader, ItemProcessor<Release, Document<String, String>> releaseProcessor, ItemWriter<Document<String, String>> releaseWriter) {
        return loadStep(stepBuilderFactory, "releases", releaseItemReader, releaseProcessor, releaseWriter);
    }

    @Bean
    TaskletStep masterLoadStep(ItemReader<Master> masterItemReader, ItemProcessor<Master, Document<String, String>> masterProcessor, ItemWriter<Document<String, String>> masterWriter) {
        return loadStep(stepBuilderFactory, "masters", masterItemReader, masterProcessor, masterWriter);
    }

    private <T> TaskletStep loadStep(StepBuilderFactory stepBuilderFactory, String name, ItemReader<T> reader, ItemProcessor<T, Document<String, String>> processor, ItemWriter<Document<String, String>> writer) {
        return stepBuilderFactory.get(name + "-load-step").<T, Document<String, String>>chunk(props.getBatchSize())
                .reader(reader).processor(processor).writer(writer)
                .listener((ItemWriteListener<?>) JobListener.builder().name(name).build()).taskExecutor(jobTaskExecutor())
                .throttleLimit(props.getThreads()).build();
    }

    @Bean
    ReleaseProcessor releaseProcessor(XmlCodec codec) {
        return ReleaseProcessor.builder().codec(codec).build();
    }

    @Bean
    MasterProcessor masterProcessor(XmlCodec codec) {
        return MasterProcessor.builder().codec(codec).props(props).build();
    }

    @Bean
    ItemWriter<Document<String, String>> releaseWriter(RediSearchClient client) {
        return writer(client, props.getReleaseIndex());
    }

    @Bean
    ItemWriter<Document<String, String>> masterWriter(RediSearchClient client) {
        return writer(client, props.getMasterIndex());
    }

    ItemWriter<Document<String, String>> writer(RediSearchClient client, String index) {
        if (props.isNoOp()) {
            return new NoopWriter<>();
        }
        return DocumentItemWriter.<String, String>builder().connection(client.connect()).index(index).build();
    }

    @Bean
    TaskExecutor jobTaskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(props.getThreads());
        return taskExecutor;
    }

}
