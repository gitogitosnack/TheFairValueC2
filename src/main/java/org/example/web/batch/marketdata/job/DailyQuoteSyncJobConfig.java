package org.example.web.batch.marketdata.job;

import org.example.web.batch.marketdata.client.RateLimitException;
import org.example.web.batch.marketdata.processor.DailyQuoteItemProcessor;
import org.example.web.batch.marketdata.reader.CompanyItemReader;
import org.example.web.batch.marketdata.writer.DailyQuoteItemWriter;
import org.example.web.entity.CompanyEntity;
import org.example.web.entity.DailyQuoteEntity;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

// dailyQuoteSyncJob / dailyQuoteSyncStep（chunk 指向、単一 Step）の Spring Batch Bean 定義。
// CompanyItemReader → DailyQuoteItemProcessor → DailyQuoteItemWriter の chunk 構成。
// skip/retry の faultTolerant 設定（RateLimitException のリトライ等）もここに持つ（設計書 4.2・7 章参照）。
@Configuration
public class DailyQuoteSyncJobConfig {

    private static final int CHUNK_SIZE = 20;
    private static final int RETRY_LIMIT = 3;

    @Bean
    public Job dailyQuoteSyncJob(JobRepository jobRepository, Step dailyQuoteSyncStep) {
        return new JobBuilder("dailyQuoteSyncJob", jobRepository)
                .start(dailyQuoteSyncStep)
                .build();
    }

    @Bean
    public Step dailyQuoteSyncStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            CompanyItemReader companyItemReader,
            DailyQuoteItemProcessor dailyQuoteItemProcessor,
            DailyQuoteItemWriter dailyQuoteItemWriter) {
        return new StepBuilder("dailyQuoteSyncStep", jobRepository)
                .<CompanyEntity, DailyQuoteEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(companyItemReader)
                .processor(dailyQuoteItemProcessor)
                .writer(dailyQuoteItemWriter)
                .faultTolerant()
                .retry(RateLimitException.class)
                .retryLimit(RETRY_LIMIT)
                .backOffPolicy(rateLimitBackOffPolicy())
                .build();
    }

    static ExponentialBackOffPolicy rateLimitBackOffPolicy() {
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        policy.setInitialInterval(2_000L);
        policy.setMultiplier(2.0);
        policy.setMaxInterval(30_000L);
        return policy;
    }
}
