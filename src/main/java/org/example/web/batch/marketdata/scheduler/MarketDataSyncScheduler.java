package org.example.web.batch.marketdata.scheduler;

import java.time.Instant;

import org.example.web.batch.marketdata.service.BatchExecutionLockService;
import org.example.web.batch.marketdata.service.BatchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 内蔵 @Scheduled エントリポイント。cron 式は環境変数（MarketDataScheduleProperties）で設定する。
// MARKETDATA_SCHEDULER_ENABLED=true のときのみ Bean 登録する（@ConditionalOnProperty）。
// 起動前に BatchExecutionLockService でロック取得を試みる（設計書 3.1・3.2・3.4 参照）。
//
// dailyQuoteSyncJob は単一 Step で delete_flg=0 の全企業（日米問わず）を処理するため、
// JP/US 用の cron を分けても実行される Job は同一である（設計書 4.2 参照）。
// (company_id, date) の UPSERT で冪等なため、同日に複数回実行されても安全である。
@Component
@ConditionalOnProperty(prefix = "marketdata.schedule", name = "scheduler-enabled", havingValue = "true")
public class MarketDataSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketDataSyncScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job dailyQuoteSyncJob;
    private final Job financialStatementSyncJob;
    private final BatchExecutionLockService lockService;

    public MarketDataSyncScheduler(
            JobLauncher jobLauncher,
            Job dailyQuoteSyncJob,
            Job financialStatementSyncJob,
            BatchExecutionLockService lockService) {
        this.jobLauncher = jobLauncher;
        this.dailyQuoteSyncJob = dailyQuoteSyncJob;
        this.financialStatementSyncJob = financialStatementSyncJob;
        this.lockService = lockService;
    }

    @Scheduled(cron = "${marketdata.schedule.quote-cron-jp}")
    public void quoteSyncJp() {
        runJob(BatchType.DAILY_QUOTE, dailyQuoteSyncJob, "quoteSyncJp");
    }

    @Scheduled(cron = "${marketdata.schedule.quote-cron-us}")
    public void quoteSyncUs() {
        runJob(BatchType.DAILY_QUOTE, dailyQuoteSyncJob, "quoteSyncUs");
    }

    @Scheduled(cron = "${marketdata.schedule.financial-cron}")
    public void financialStatementSync() {
        runJob(BatchType.FINANCIAL_STATEMENT, financialStatementSyncJob, "financialStatementSync");
    }

    private void runJob(BatchType batchType, Job job, String trigger) {
        if (!lockService.tryLock(batchType)) {
            log.info("[{}] 他経路で実行中のため内蔵スケジューラ起動（{}）はスキップします", batchType, trigger);
            return;
        }
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("triggerType", "SCHEDULED_INTERNAL")
                    .addLong("time", Instant.now().toEpochMilli())
                    .toJobParameters();
            jobLauncher.run(job, jobParameters);
        } catch (Exception e) {
            log.error("[{}] 内蔵スケジューラ起動（{}）でJob実行に失敗しました", batchType, trigger, e);
        } finally {
            lockService.unlock(batchType);
        }
    }
}
