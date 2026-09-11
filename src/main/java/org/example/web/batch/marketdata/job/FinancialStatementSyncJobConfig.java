package org.example.web.batch.marketdata.job;

// クライアント・DTO・Processor・Reader・Tasklet・Writer・Entityの各インポート
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

// Spring BatchのJob/Step構成クラス、およびリスナー関連のインポート
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener; // Step間でデータ（Context）を引き継ぐためのリスナー
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;

// Spring Core / Context 関連のインポート
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @Configuration:
 *                 このクラスがSpringのBean定義クラスであることを宣言します。
 *                 クラス内の @Bean メソッド群がSpringコンテナによって実行され、返り値がBeanとして登録されます。
 */
@Configuration
public class FinancialStatementSyncJobConfig {

        // 定数の定義（private static final）
        // CHUNK_SIZE: Chunk方式のStepにおけるコミット単位（10件ごと）
        private static final int CHUNK_SIZE = 10;

        // RETRY_LIMIT: RateLimitExceptionが発生した際のリトライ上限回数（計3回試行）
        private static final int RETRY_LIMIT = 3;

        /**
         * @Bean:
         *        財務諸表同期Job（financialStatementSyncJob）をBean登録します。
         *        引数の 3つの Step（米国株Step、EDINETリスト取得Step、日本株Step）はSpringによりDI注入されます。
         */
        @Bean
        public Job financialStatementSyncJob(
                        JobRepository jobRepository,
                        Step usFinancialStatementStep,
                        Step edinetDocumentListStep,
                        Step jpFinancialStatementStep) {

                // JobBuilder を使用して Job を構築
                return new JobBuilder("financialStatementSyncJob", jobRepository)
                                .start(usFinancialStatementStep) // ① 最初に米国株の財務諸表取得を実行
                                .next(edinetDocumentListStep) // ② 完了後、EDINET書類リストの取得Taskletを実行
                                .next(jpFinancialStatementStep) // ③ 完了後、日本株の財務諸表取得を実行
                                .build(); // ジョブインスタンスの生成
        }

        /**
         * @Bean:
         *        ① 米国株財務諸表取得 Step（Chunk方式）
         */
        @Bean
        public Step usFinancialStatementStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        UsCompanyItemReader usCompanyItemReader,
                        UsFinancialStatementItemProcessor usFinancialStatementItemProcessor,
                        FinancialStatementItemWriter financialStatementItemWriter) {

                return new StepBuilder("usFinancialStatementStep", jobRepository)
                                // .<入力型, 出力型>chunk(サイズ, トランザクションマネージャー)
                                // UsCompanyItemReader(CompanyEntity) -> UsFinancialStatementItemProcessor ->
                                // FinancialStatementItemWriter(FinancialStatementEntity)
                                .<CompanyEntity, FinancialStatementEntity>chunk(CHUNK_SIZE, transactionManager)
                                .reader(usCompanyItemReader)
                                .processor(usFinancialStatementItemProcessor)
                                .writer(financialStatementItemWriter)
                                // エラー耐性の設定（RateLimitException 発生時に指数バックオフでリトライ）
                                .faultTolerant()
                                .retry(RateLimitException.class)
                                .retryLimit(RETRY_LIMIT)
                                .backOffPolicy(DailyQuoteSyncJobConfig.rateLimitBackOffPolicy()) // 他の設定クラスのstaticヘルパーメソッドを再利用
                                .build();
        }

        /**
         * @Bean:
         *        ② EDINET書類一覧を取得する Tasklet Step（Tasklet方式）
         * 
         *        単発のAPI処理・前処理を行うため、Reader/Processor/Writerを使わない Tasklet 方式で定義されています。
         */
        @Bean
        public Step edinetDocumentListStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        EdinetDocumentListTasklet edinetDocumentListTasklet) {

                // ExecutionContextPromotionListener:
                // 通常、Step内で保存されたデータは「StepExecutionContext（そのStep内限定）」に閉じます。
                // このリスナーを設定すると、Step終了時に指定したキーのデータを「JobExecutionContext（Job全体共有）」へ昇格（Promotion）させ、
                // 後続の Step（jpFinancialStatementStep）から参照できるようにします。
                ExecutionContextPromotionListener promotionListener = new ExecutionContextPromotionListener();

                // setKeys(String[]):
                // JobExecutionContext へ昇格させたいデータのキー名を指定します（配列形式）。
                promotionListener.setKeys(new String[] { EdinetMatchedDocumentItemReader.EXECUTION_CONTEXT_KEY });

                return new StepBuilder("edinetDocumentListStep", jobRepository)
                                // .tasklet(Tasklet実装, トランザクションマネージャー):
                                // Chunk方式ではなく、1回限りのタスクを実行する Tasklet 方式としてStepを生成します。
                                .tasklet(edinetDocumentListTasklet, transactionManager)

                                // .listener(StepExecutionListener):
                                // 上記で作成した Context 昇格用リスナーを Step に登録します。
                                .listener(promotionListener)
                                .build();
        }

        /**
         * @Bean:
         *        ③ 日本株財務諸表取得 Step（Chunk方式）
         * 
         *        edinetDocumentListStep で取得され、JobExecutionContext に昇格された EDINET 書類データ群を
         *        Reader で読み込みます。
         */
        @Bean
        public Step jpFinancialStatementStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        EdinetMatchedDocumentItemReader edinetMatchedDocumentItemReader,
                        JpFinancialStatementItemProcessor jpFinancialStatementItemProcessor,
                        FinancialStatementItemWriter financialStatementItemWriter) {

                return new StepBuilder("jpFinancialStatementStep", jobRepository)
                                // .<入力型, 出力型>chunk(サイズ, トランザクションマネージャー)
                                // EdinetMatchedDocumentItemReader(EdinetMatchedDocument) ->
                                // JpFinancialStatementItemProcessor ->
                                // FinancialStatementItemWriter(FinancialStatementEntity)
                                .<EdinetMatchedDocument, FinancialStatementEntity>chunk(CHUNK_SIZE, transactionManager)
                                .reader(edinetMatchedDocumentItemReader)
                                .processor(jpFinancialStatementItemProcessor)
                                .writer(financialStatementItemWriter)
                                // エラー耐性の設定（RateLimitException 発生時に指数バックオフでリトライ）
                                .faultTolerant()
                                .retry(RateLimitException.class)
                                .retryLimit(RETRY_LIMIT)
                                .backOffPolicy(DailyQuoteSyncJobConfig.rateLimitBackOffPolicy())
                                .build();
        }
}