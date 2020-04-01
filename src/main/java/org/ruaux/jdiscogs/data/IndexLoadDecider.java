package org.ruaux.jdiscogs.data;

import com.redislabs.lettusearch.RediSearchUtils;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import com.redislabs.lettusearch.index.IndexInfo;
import io.lettuce.core.RedisCommandExecutionException;
import lombok.Builder;
import lombok.Setter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

@Builder
public class IndexLoadDecider implements JobExecutionDecider {

    public static final String YES = "YES";
    public static final String NO = "NO";

    @Setter
    private StatefulRediSearchConnection<?, ?> connection;
    @Setter
    private String index;
    @Setter
    private Range range;

    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        try {
            IndexInfo info = RediSearchUtils.getInfo(connection.sync().ftInfo(index));
            return new FlowExecutionStatus(range.accept(info.getNumDocs()) ? YES : NO);
        } catch (RedisCommandExecutionException e) {
            return new FlowExecutionStatus(YES);
        }
    }

}