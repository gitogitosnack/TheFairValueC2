package org.example.web.batch.marketdata.job;

import org.example.web.batch.marketdata.client.RateLimitException;
import org.example.web.batch.marketdata.client.dto.EdinetMatchedDocument;
import org.example.web.batch.marketdata.processor.JpFinancialStatementItemProcessor;
import org.example.web.batch.marketdata.processor.UsFinancialStatementItemProcessor;
import org.example.web.batch.marketdata.reader.EdinetMatchedDocumentItemReader;
import org.example.web.batch.marketdata.reader.UsCompanyItemReader;
import org.example.web.batch.marketdata.tasklet.EdinetDocumentListTasklet;
import org.example.web.batch.marketdata.writer.FinancialStatementItemWriter;
import org.example.web.entity.CompanyEntity;
import org.example.web.entity.FinancialStatementEntity;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

// financialStatementSyncJob の Spring Batch Bean 定義。
// usFinancialStatementStep → edinetDocumentListStep → jpFinancialStatementStep の順に
// 単一 Job 内で直列実行する（並行実行はしない、設計書 4.3 参照）。
@Configuration
public class FinancialStatementSyncJobConfig {

    private static final int CHUNK_SIZE = 10;
    private static final int RETRY_LIMIT = 3;

    @Bean
    public Job financialStatementSyncJob(
            JobRepository jobRepository,
            Step usFinancialStatementStep,
            Step edinetDocumentListStep,
            Step jpFinancialStatementStep) {
        return new JobBuilder("financialStatementSyncJob", jobRepository)
                .start(usFinancialStatementStep)
                .next(edinetDocumentListStep)
                .next(jpFinancialStatementStep)
                .build();
    }

    @Bean
    public Step usFinancialStatementStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            UsCompanyItemReader usCompanyItemReader,
            UsFinancialStatementItemProcessor usFinancialStatementItemProcessor,
            FinancialStatementItemWriter financialStatementItemWriter) {
        return new StepBuilder("usFinancialStatementStep", jobRepository)
                .<CompanyEntity, FinancialStatementEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(usCompanyItemReader)
                .processor(usFinancialStatementItemProcessor)
                .writer(financialStatementItemWriter)
                .faultTolerant()
                .retry(RateLimitException.class)
                .retryLimit(RETRY_LIMIT)
                .backOffPolicy(DailyQuoteSyncJobConfig.rateLimitBackOffPolicy())
                .build();
    }

    // EDINET の書類一覧をその日1回だけ取得・絞り込みする Tasklet Step。
    // 結果（EdinetMatchedDocument のリスト）は ExecutionContextPromotionListener により
    // Step の ExecutionContext から Job の ExecutionContext へ昇格され、
    // jpFinancialStatementStep の EdinetMatchedDocumentItemReader が読み取る（設計書 4.3.2 参照）。
    @Bean
    public Step edinetDocumentListStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            EdinetDocumentListTasklet edinetDocumentListTasklet) {
        ExecutionContextPromotionListener promotionListener = new ExecutionContextPromotionListener();
        promotionListener.setKeys(new String[] { EdinetMatchedDocumentItemReader.EXECUTION_CONTEXT_KEY });
        return new StepBuilder("edinetDocumentListStep", jobRepository)
                .tasklet(edinetDocumentListTasklet, transactionManager)
                .listener(promotionListener)
                .build();
    }

    @Bean
    public Step jpFinancialStatementStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            EdinetMatchedDocumentItemReader edinetMatchedDocumentItemReader,
            JpFinancialStatementItemProcessor jpFinancialStatementItemProcessor,
            FinancialStatementItemWriter financialStatementItemWriter) {
        return new StepBuilder("jpFinancialStatementStep", jobRepository)
                .<EdinetMatchedDocument, FinancialStatementEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(edinetMatchedDocumentItemReader)
                .processor(jpFinancialStatementItemProcessor)
                .writer(financialStatementItemWriter)
                .faultTolerant()
                .retry(RateLimitException.class)
                .retryLimit(RETRY_LIMIT)
                .backOffPolicy(DailyQuoteSyncJobConfig.rateLimitBackOffPolicy())
                .build();
    }
}
