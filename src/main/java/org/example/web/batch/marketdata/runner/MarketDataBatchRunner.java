package org.example.web.batch.marketdata.runner;

import java.time.Instant;

import org.example.web.batch.marketdata.service.BatchExecutionLockService;
import org.example.web.batch.marketdata.service.BatchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// 外部スケジューラ用エントリポイント（CommandLineRunner）。
// 将来はバッチ専用メインクラス org.example.MarketDataBatchApplication から起動され、
// --job=dailyQuote / --job=financialStatement 引数でどちらの JobLauncher.run(job, jobParameters) を
// 呼ぶかを判定する。起動前に BatchExecutionLockService でロック取得を試みる（設計書 3.1・3.2・3.4 参照）。
//
// 現状は開発環境の単一プロセス運用（CLAUDE.md）のため、Web アプリと同一 JVM（mvn spring-boot:run）でも
// `--job=dailyQuote` 等の引数付きで起動されたときのみ動作するようにしてある
// （引数なしの通常起動では何もしない。バッチ専用 JAR への分離は将来対応、設計書 3.1 参照）。
@Component
public class MarketDataBatchRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MarketDataBatchRunner.class);

    private static final String ARG_PREFIX = "--job=";
    private static final String JOB_DAILY_QUOTE = "dailyQuote";
    private static final String JOB_FINANCIAL_STATEMENT = "financialStatement";

    private final JobLauncher jobLauncher;
    private final Job dailyQuoteSyncJob;
    private final Job financialStatementSyncJob;
    private final BatchExecutionLockService lockService;

    public MarketDataBatchRunner(
            JobLauncher jobLauncher,
            Job dailyQuoteSyncJob,
            Job financialStatementSyncJob,
            BatchExecutionLockService lockService) {
        this.jobLauncher = jobLauncher;
        this.dailyQuoteSyncJob = dailyQuoteSyncJob;
        this.financialStatementSyncJob = financialStatementSyncJob;
        this.lockService = lockService;
    }

    @Override
    public void run(String... args) throws Exception {
        String jobName = extractJobArg(args);
        if (jobName == null) {
            return;
        }

        Job job;
        BatchType batchType;
        switch (jobName) {
            case JOB_DAILY_QUOTE -> {
                job = dailyQuoteSyncJob;
                batchType = BatchType.DAILY_QUOTE;
            }
            case JOB_FINANCIAL_STATEMENT -> {
                job = financialStatementSyncJob;
                batchType = BatchType.FINANCIAL_STATEMENT;
            }
            default -> {
                log.warn("未知の --job 引数のため何もしません: {}", jobName);
                return;
            }
        }

        if (!lockService.tryLock(batchType)) {
            log.info("[{}] 他経路で実行中のため今回の外部スケジューラ起動はスキップします", batchType);
            return;
        }
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("triggerType", "SCHEDULED_EXTERNAL")
                    .addLong("time", Instant.now().toEpochMilli())
                    .toJobParameters();
            jobLauncher.run(job, jobParameters);
        } finally {
            lockService.unlock(batchType);
        }
    }

    private static String extractJobArg(String[] args) {
        for (String arg : args) {
            if (arg.startsWith(ARG_PREFIX)) {
                return arg.substring(ARG_PREFIX.length());
            }
        }
        return null;
    }
}
