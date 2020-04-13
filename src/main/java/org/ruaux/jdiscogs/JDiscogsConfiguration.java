package org.ruaux.jdiscogs;

import com.redislabs.lettusearch.RediSearchClient;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.index.Schema;
import com.redislabs.lettusearch.index.field.NumericField;
import com.redislabs.lettusearch.index.field.PhoneticMatcher;
import com.redislabs.lettusearch.index.field.TagField;
import com.redislabs.lettusearch.index.field.TextField;
import com.redislabs.lettusearch.search.Document;
import com.redislabs.springredisearch.RediSearchAutoConfiguration;
import com.thoughtworks.xstream.XStream;
import lombok.extern.slf4j.Slf4j;
import org.ruaux.jdiscogs.data.*;
import org.ruaux.jdiscogs.model.*;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.oxm.xstream.XStreamMarshaller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.ruaux.jdiscogs.data.Fields.*;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JDiscogsProperties.class)
@Import({RestTemplateAutoConfiguration.class, RediSearchAutoConfiguration.class, ReleaseProcessor.class, MasterProcessor.class, TextSanitizer.class, XmlCodec.class})
@EnableBatchProcessing
public class JDiscogsConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobRepository jobRepository;
    private final JDiscogsProperties props;
    private final StatefulRediSearchConnection<String, String> connection;

    public JDiscogsConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, JobRepository jobRepository, JDiscogsProperties props, StatefulRediSearchConnection<String, String> connection) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.jobRepository = jobRepository;
        this.props = props;
        this.connection = connection;
    }

    @Bean
    @ConditionalOnMissingBean(name = "discogsClient")
    public DiscogsClient discogsClient(JDiscogsProperties props, RestTemplateBuilder restTemplateBuilder) {
        DiscogsClient client = new DiscogsClient();
        client.setProps(props);
        client.setRestTemplate(restTemplateBuilder.build());
        return client;
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
    ItemReader<Release> releaseItemReader(XStreamMarshaller marshaller) throws IOException {
        return reader(marshaller, resource(props.getReleasesUrl()), "release");
    }

    @Bean
    ItemReader<Master> masterItemReader(XStreamMarshaller marshaller) throws IOException {
        return reader(marshaller, resource(props.getMastersUrl()), "master");
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

    @Bean
    public XStreamMarshaller xmlMarshaller() {
        XStreamMarshaller marshaller = new XStreamMarshaller();
        com.thoughtworks.xstream.XStream xStream = marshaller.getXStream();
        XStream.setupDefaultSecurity(xStream);
        xStream.allowTypes(new Class[]{Release.class, Master.class});
        Map<String, Class<?>> aliases = new HashMap<>();
        aliases.put("release", Release.class);
        aliases.put("master", Master.class);
        aliases.put("image", Image.class);
        aliases.put("artist", Artist.class);
        aliases.put("label", Entity.class);
        aliases.put("format", Format.class);
        aliases.put("description", String.class);
        aliases.put("genre", String.class);
        aliases.put("style", String.class);
        aliases.put("track", Track.class);
        aliases.put("identifier", Identifier.class);
        aliases.put("video", Video.class);
        aliases.put("company", Entity.class);
        marshaller.setAliases(aliases);
        Map<String, Class<?>> attributes = new HashMap<>();
        attributes.put("id", Long.class);
        marshaller.setUseAttributeFor(attributes);
        return marshaller;
    }


    private <T> ItemReader<T> reader(XStreamMarshaller marshaller, org.springframework.core.io.Resource resource, String root) {
        return synchronizedReader(new StaxEventItemReaderBuilder<T>().name(root + "Reader")
                .addFragmentRootElements(root).resource(resource).unmarshaller(marshaller).build());
    }

    private <T> SynchronizedItemStreamReader<T> synchronizedReader(ItemStreamReader<T> reader) {
        return new SynchronizedItemStreamReaderBuilder<T>().delegate(reader).build();
    }

    private org.springframework.core.io.Resource resource(String url) throws IOException {
        URI uri = URI.create(url);
        org.springframework.core.io.Resource resource = getResource(uri);
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
    TaskletStep releaseLoadStep(ItemReader<Release> reader, ReleaseProcessor processor, ItemWriter<Document<String, String>> releaseWriter) {
        return loadStep("release", stepBuilderFactory, reader, processor, releaseWriter);
    }

    @Bean
    TaskletStep masterLoadStep(ItemReader<Master> reader, MasterProcessor processor, ItemWriter<Document<String, String>> masterWriter) {
        return loadStep("master", stepBuilderFactory, reader, processor, masterWriter);
    }

    private <T> TaskletStep loadStep(String name, StepBuilderFactory stepBuilderFactory, ItemReader<T> reader, ItemProcessor<T, Document<String, String>> processor, ItemWriter<Document<String, String>> writer) {
        return stepBuilderFactory.get(name + "LoadStep").<T, Document<String, String>>chunk(props.getBatchSize()).reader(reader).processor(processor).writer(writer).listener((ItemWriteListener<?>) JobListener.builder().name(name).build()).build();
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

}
