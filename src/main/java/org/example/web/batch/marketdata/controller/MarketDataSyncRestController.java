package org.example.web.batch.marketdata.controller;

import java.time.Instant;
import java.util.Map;

import org.example.web.batch.marketdata.service.BatchExecutionLockService;
import org.example.web.batch.marketdata.service.BatchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 手動実行用 REST エンドポイント。
// POST /rest_market_data_sync/quote            … 全銘柄の日次株価を即時同期
// POST /rest_market_data_sync/financial-statement … 全銘柄の財務諸表を即時同期（米国株→日本株の順で逐次実行）
// ロック取得に失敗した場合は 409 相当のレスポンスを返す（設計書 3.4・4.4 参照）。
@RestController
@RequestMapping("/rest_market_data_sync")
public class MarketDataSyncRestController {

    private static final Logger log = LoggerFactory.getLogger(MarketDataSyncRestController.class);

    private final JobLauncher jobLauncher;
    private final Job dailyQuoteSyncJob;
    private final Job financialStatementSyncJob;
    private final BatchExecutionLockService lockService;

    public MarketDataSyncRestController(
            JobLauncher jobLauncher,
            Job dailyQuoteSyncJob,
            Job financialStatementSyncJob,
            BatchExecutionLockService lockService) {
        this.jobLauncher = jobLauncher;
        this.dailyQuoteSyncJob = dailyQuoteSyncJob;
        this.financialStatementSyncJob = financialStatementSyncJob;
        this.lockService = lockService;
    }

    @PostMapping("/quote")
    public ResponseEntity<Map<String, Object>> syncQuote() {
        return runJob(BatchType.DAILY_QUOTE, dailyQuoteSyncJob);
    }

    @PostMapping("/financial-statement")
    public ResponseEntity<Map<String, Object>> syncFinancialStatement() {
        return runJob(BatchType.FINANCIAL_STATEMENT, financialStatementSyncJob);
    }

    private ResponseEntity<Map<String, Object>> runJob(BatchType batchType, @NonNull Job job) {
        if (!lockService.tryLock(batchType)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "現在実行中のため開始できません"));
        }
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("triggerType", "MANUAL")
                    .addLong("time", Instant.now().toEpochMilli())
                    .toJobParameters();
            JobExecution execution = jobLauncher.run(job, jobParameters);
            boolean success = execution.getStatus() == BatchStatus.COMPLETED;
            return ResponseEntity.ok(Map.of(
                    "message", success ? "実行が完了しました" : "実行が完了しましたが一部失敗しました",
                    "status", execution.getStatus().toString(),
                    "readCount", sumStepMetric(execution, org.springframework.batch.core.StepExecution::getReadCount),
                    "writeCount", sumStepMetric(execution, org.springframework.batch.core.StepExecution::getWriteCount),
                    "skipCount", sumStepMetric(execution, org.springframework.batch.core.StepExecution::getSkipCount)));
        } catch (Exception e) {
            log.error("[{}] 手動実行でJob起動に失敗しました", batchType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "実行に失敗しました: " + e.getMessage()));
        } finally {
            lockService.unlock(batchType);
        }
    }

    private static long sumStepMetric(
            JobExecution execution,
            java.util.function.ToLongFunction<org.springframework.batch.core.StepExecution> extractor) {
        return execution.getStepExecutions().stream().mapToLong(extractor).sum();
    }
}
