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
import lombok.extern.slf4j.Slf4j;
import org.ruaux.jdiscogs.data.model.Master;
import org.ruaux.jdiscogs.data.model.Release;
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

@Slf4j
@Configuration
@EnableBatchProcessing
@EnableConfigurationProperties(JDiscogsBatchProperties.class)
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
    ItemReader<Release> releaseItemReader() throws IOException {
        return reader(Release.class, resource(props.getReleasesUrl()), "release");
    }

    @Bean
    ItemReader<Master> masterItemReader() throws IOException {
        return reader(Master.class, resource(props.getMastersUrl()), "master");
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
        return synchronizedReader(new StaxEventItemReaderBuilder<T>().name(root + "Reader")
                .addFragmentRootElements(root).resource(resource).unmarshaller(marshaller).build());
    }

    private <T> SynchronizedItemStreamReader<T> synchronizedReader(ItemStreamReader<T> reader) {
        return new SynchronizedItemStreamReaderBuilder<T>().delegate(reader).build();
    }

    private Resource resource(String url) throws IOException {
        URI uri = URI.create(url);
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
        return IndexLoadDecider.builder().connection(connection).index(props.getReleaseIndex()).minItemCount(props.getMinReleaseItemCount()).build();
    }

    @Bean
    IndexLoadDecider masterIndexLoadDecider() {
        return IndexLoadDecider.builder().connection(connection).index(props.getMasterIndex()).minItemCount(props.getMinMasterItemCount()).build();
    }

    @Bean
    IndexDropStep<String> releaseIndexDropStep() {
        return indexDropStep(props.getReleaseIndex());
    }

    @Bean
    IndexDropStep<String> masterIndexDropStep() {
        return indexDropStep(props.getMasterIndex());
    }

    private IndexDropStep<String> indexDropStep(String index) {
        return IndexDropStep.<String>builder().ignoreErrors(true).jobRepository(jobRepository).name(index + "IndexDropStep").connection(connection).index(index).build();
    }


    @Bean
    IndexCreateStep<String> releaseIndexCreateStep(Schema releaseSchema) {
        return indexCreateStep(props.getReleaseIndex(), releaseSchema);
    }

    @Bean
    IndexCreateStep<String> masterIndexCreateStep(Schema masterSchema) {
        return indexCreateStep(props.getMasterIndex(), masterSchema);
    }

    private IndexCreateStep<String> indexCreateStep(String index, Schema schema) {
        return IndexCreateStep.<String>builder().jobRepository(jobRepository).name(index + "IndexCreateStep").connection(connection).index(index)
                .schema(schema).build();
    }

    @Bean
    Job releaseLoadJob(IndexLoadDecider releaseIndexLoadDecider, IndexDropStep<String> releaseIndexDropStep, IndexCreateStep<String> releaseIndexCreateStep, TaskletStep releaseLoadStep) {
        return job("release", releaseIndexLoadDecider, releaseIndexDropStep, releaseIndexCreateStep, releaseLoadStep);
    }

    @Bean
    Job masterLoadJob(IndexLoadDecider masterIndexLoadDecider, IndexDropStep<String> masterIndexDropStep, IndexCreateStep<String> masterIndexCreateStep, TaskletStep masterLoadStep) {
        return job("master", masterIndexLoadDecider, masterIndexDropStep, masterIndexCreateStep, masterLoadStep);
    }

    private Job job(String name, IndexLoadDecider decider, IndexDropStep<String> indexDropStep, IndexCreateStep<String> indexCreateStep, TaskletStep loadStep) {
        TaskletStep skipStep = stepBuilderFactory.get(name + "NoFlow").tasklet(SkipStep.builder().name(name + " load job").build()).build();
        Flow flow = new FlowBuilder<Flow>(name + "Flow").start(decider)
                .on(IndexLoadDecider.PROCEED).to(indexDropStep).next(indexCreateStep).next(loadStep)
                .from(decider).on(IndexLoadDecider.SKIP).to(skipStep).end();
        return jobBuilderFactory.get(name + "Job").start(flow).end().build();
    }

    @Bean
    TaskletStep releaseLoadStep(ItemReader<Release> releaseItemReader, ItemProcessor<Release, Document<String, String>> releaseProcessor, ItemWriter<Document<String, String>> releaseWriter) {
        return loadStep("release", stepBuilderFactory, releaseItemReader, releaseProcessor, releaseWriter);
    }

    @Bean
    TaskletStep masterLoadStep(ItemReader<Master> masterItemReader, ItemProcessor<Master, Document<String, String>> masterProcessor, ItemWriter<Document<String, String>> masterWriter) {
        return loadStep("master", stepBuilderFactory, masterItemReader, masterProcessor, masterWriter);
    }

    private <T> TaskletStep loadStep(String name, StepBuilderFactory stepBuilderFactory, ItemReader<T> reader, ItemProcessor<T, Document<String, String>> processor, ItemWriter<Document<String, String>> writer) {
        return stepBuilderFactory.get(name + "LoadStep").<T, Document<String, String>>chunk(props.getBatchSize())
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
