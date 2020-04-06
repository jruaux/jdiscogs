package org.ruaux.jdiscogs.data;

import com.redislabs.lettusearch.RediSearchUtils;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.index.IndexInfo;
import io.lettuce.core.RedisCommandExecutionException;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

@Slf4j
@Builder
public class IndexLoadDecider implements JobExecutionDecider {

    public static final String PROCEED = "PROCEED";
    public static final String SKIP = "SKIP";

    @Setter
    private StatefulRediSearchConnection<String, ?> connection;
    @Setter
    private String index;
    @Setter
    private long minItemCount;

    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        try {
            IndexInfo info = RediSearchUtils.getInfo(connection.sync().ftInfo(index));
            boolean withinRange = info.getNumDocs()>=minItemCount;
            log.info("Index {}: numDocs={} minItemCount={}", index, info.getNumDocs(), minItemCount);
            return new FlowExecutionStatus(withinRange ? SKIP : PROCEED);
        } catch (RedisCommandExecutionException e) {
            return new FlowExecutionStatus(PROCEED);
        }
    }

}