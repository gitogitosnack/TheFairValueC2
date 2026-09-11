package org.example.web.batch.marketdata.job;

// パッケージインポート部
// 自作の例外クラス、Processor/Reader/Writer、エンティティなどをインポート
import org.example.web.batch.marketdata.client.RateLimitException;
import org.example.web.batch.marketdata.processor.DailyQuoteItemProcessor;
import org.example.web.batch.marketdata.reader.CompanyItemReader;
import org.example.web.batch.marketdata.writer.DailyQuoteItemWriter;
import org.example.web.entity.CompanyEntity;
import org.example.web.entity.DailyQuoteEntity;

// Spring BatchのJob/Step定義およびビルダー関連のインポート
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;

// SpringのDI（依存性注入）や設定クラス作成のためのアノテーション・アノテーション補助クラスのインポート
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @Configuration:
 *                 Spring IoCコンテナに対し、このクラスがBean定義（設定情報）を持つクラスであることを宣言します。
 *                 クラス内の @Bean アノテーションが付与されたメソッドがコンテナによって実行され、返り値がBeanとして登録されます。
 */
@Configuration
public class DailyQuoteSyncJobConfig {

    // 定数の定義（private static final）
    // CHUNK_SIZE: 1回のトランザクションで処理するデータの件数（20件ごとにコミット）
    private static final int CHUNK_SIZE = 20;

    // RETRY_LIMIT: 特定例外（RateLimitException）発生時にリトライを試みる最大回数（初回の実行1回 + リトライ2回 = 計3回）
    private static final int RETRY_LIMIT = 3;

    /**
     * @Bean:
     *        メソッドの返り値（Jobオブジェクト）をSpringコンテナのBeanとして登録します。
     *        メソッド名の "dailyQuoteSyncJob" がデフォルトのBean名になります。
     * 
     * @NonNull:
     *           引数が null でないことを明示するアノテーション（静的解析やIDEチェックに利用）。
     *           引数の JobRepository と Step は、Springによって自動的にDI（注入）されます。
     */
    @Bean
    public Job dailyQuoteSyncJob(@NonNull JobRepository jobRepository, @NonNull Step dailyQuoteSyncStep) {
        // JobBuilder: Jobを流暢なAPI（Method Chaining）で構築するためのビルダークラス。
        // 第1引数: Job識別名 ("dailyQuoteSyncJob")
        // 第2引数: Jobの実行状態や履歴を管理する JobRepository
        return new JobBuilder("dailyQuoteSyncJob", jobRepository)
                .start(dailyQuoteSyncStep) // 最初に実行する Step を指定
                .build(); // 設定を元に Job インスタンスを生成
    }

    /**
     * @Bean:
     *        StepオブジェクトをSpring Beanとして登録します。
     *        必要な依存オブジェクト（JobRepository, PlatformTransactionManager, Reader,
     *        Processor, Writer）は
     *        SpringのDIによって引数経由で注入されます。
     */
    @Bean
    public Step dailyQuoteSyncStep(
            @NonNull JobRepository jobRepository,
            @NonNull PlatformTransactionManager transactionManager, // トランザクション制御を行うマネージャー
            @NonNull CompanyItemReader companyItemReader, // データ読み込みを担当するComponent
            @NonNull DailyQuoteItemProcessor dailyQuoteItemProcessor, // データ加工・変換を担当するComponent
            @NonNull DailyQuoteItemWriter dailyQuoteItemWriter) { // データ書き込み（保存）を担当するComponent

        // StepBuilder: Stepを構築するためのビルダークラス。
        return new StepBuilder("dailyQuoteSyncStep", jobRepository)

                // .<入力型, 出力型>chunk(チャンクサイズ, トランザクションマネージャー):
                // Chunk指向のStepを設定します。
                // - 入力型: Readerから受け取る型（CompanyEntity）
                // - 出力型: Writerに引き渡す型（DailyQuoteEntity）
                // CHUNK_SIZE件ごとにReader→Processorを実行し、指定されたトランザクション境界でWriterを呼び出してDBコミットします。
                .<CompanyEntity, DailyQuoteEntity>chunk(CHUNK_SIZE, transactionManager)

                .reader(companyItemReader) // 使用する ItemReader の登録
                .processor(dailyQuoteItemProcessor) // 使用する ItemProcessor の登録
                .writer(dailyQuoteItemWriter) // 使用する ItemWriter の登録

                // faultTolerant():
                // Stepの耐障害性（リトライやスキップ機能）を有効化するための設定モードに切り替えます。
                .faultTolerant()

                // retry(例外クラス):
                // 指定した例外が発生した場合に、処理を中断せずリトライ対象とするよう指定します。
                .retry(RateLimitException.class)

                // retryLimit(回数):
                // リトライの最大試行回数を設定します。
                .retryLimit(RETRY_LIMIT)

                // backOffPolicy(ポリシー):
                // リトライ実行時の待機時間（インターバル）を制御するバックオフポリシーを設定します。
                .backOffPolicy(rateLimitBackOffPolicy())

                .build(); // 設定を元に Step インスタンスを生成
    }

    /**
     * 指数関数的バックオフ（Exponential BackOff）ポリシーを生成するヘルパーメソッド。
     * リトライ間隔を徐々に広げていくことで、外部API等の過負荷（Rate Limit）を解消しやすくします。
     * 
     * ※ staticメソッドとしているため、コンテキスト依存なしで呼び出し可能です。
     */
    static ExponentialBackOffPolicy rateLimitBackOffPolicy() {
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();

        // 初回リトライまでの待機時間（ミリ秒）: 2,000ms (2秒)
        // 数値リテラルのアンダースコア (2_000L) は視認性を高めるためのJava7以降の文法です。
        policy.setInitialInterval(2_000L);

        // 待機時間の乗数: 2.0 (リトライごとに待機時間を2倍にする)
        // 例: 1回目リトライ後=2秒、2回目リトライ後=4秒...
        policy.setMultiplier(2.0);

        // 最大待機時間（ミリ秒）: 30,000ms (30秒)
        // 計算上の待機時間がこれを超えても、最大30秒で頭打ちになります。
        policy.setMaxInterval(30_000L);

        return policy;
    }
}