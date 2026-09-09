package org.example.web.batch.marketdata.tasklet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.web.batch.marketdata.client.EdinetFinancialDataProviderImpl;
import org.example.web.batch.marketdata.client.RateLimitException;
import org.example.web.batch.marketdata.client.dto.EdinetDocumentListItem;
import org.example.web.batch.marketdata.client.dto.EdinetMatchedDocument;
import org.example.web.batch.marketdata.reader.EdinetMatchedDocumentItemReader;
import org.example.web.dao.CompanyDao;
import org.example.web.entity.CompanyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

// edinetDocumentListStep の実体（Tasklet、1回のみ実行）。
// EDINET の書類一覧 API（documents.json）をその日 1 回だけ取得し、docTypeCode で絞り込み、
// secCode（末尾 0 除去）と companies.code を突合した (companyId, docID) の一致リストを
// ExecutionContextPromotionListener 経由で Job の ExecutionContext へ格納する（設計書 4.3.2 参照）。
@Component
public class EdinetDocumentListTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(EdinetDocumentListTasklet.class);

    private final EdinetFinancialDataProviderImpl edinetFinancialDataProvider;
    private final CompanyDao companyDao;
    private final RetryTemplate retryTemplate;

    public EdinetDocumentListTasklet(
            EdinetFinancialDataProviderImpl edinetFinancialDataProvider,
            CompanyDao companyDao) {
        this.edinetFinancialDataProvider = edinetFinancialDataProvider;
        this.companyDao = companyDao;
        this.retryTemplate = buildRetryTemplate();
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate today = LocalDate.now();

        // documents.json の取得失敗は個別銘柄の失敗ではなくJob全体に影響するため、
        // ここで明示的に RetryTemplate による指数バックオフのリトライを行う（設計書 7 章参照）。
        List<EdinetDocumentListItem> documents = retryTemplate.execute(retryContext -> {
            log.info("EDINET 書類一覧を取得します: date={}, attempt={}", today, retryContext.getRetryCount() + 1);
            return edinetFinancialDataProvider.fetchDocumentList(today);
        });

        List<CompanyEntity> jpCompanies = companyDao.selectActiveByCountryCode("JP");
        Map<String, Integer> codeToCompanyId = jpCompanies.stream()
                .collect(Collectors.toMap(CompanyEntity::getCode, CompanyEntity::getId, (a, b) -> a));

        List<EdinetMatchedDocument> matched = new ArrayList<>();
        for (EdinetDocumentListItem doc : documents) {
            String secCode = doc.secCode();
            if (secCode == null || secCode.isBlank()) {
                continue;
            }
            // EDINET の secCode は5桁・末尾0埋め（例 44520）。末尾の0を除去して4桁の証券コードにする。
            String companyCode = secCode.length() == 5 && secCode.endsWith("0")
                    ? secCode.substring(0, 4)
                    : secCode;
            Integer companyId = codeToCompanyId.get(companyCode);
            if (companyId != null) {
                matched.add(new EdinetMatchedDocument(companyId, doc.docId(), doc.docTypeCode(), doc.periodEnd()));
            }
        }

        log.info("EDINET 書類一覧の絞り込み結果: 全{}件中 自社銘柄と一致={}件", documents.size(), matched.size());

        chunkContext.getStepContext().getStepExecution().getExecutionContext()
                .put(EdinetMatchedDocumentItemReader.EXECUTION_CONTEXT_KEY, matched);

        return RepeatStatus.FINISHED;
    }

    private static RetryTemplate buildRetryTemplate() {
        RetryTemplate template = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, Map.of(RateLimitException.class, true));
        template.setRetryPolicy(retryPolicy);
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2_000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(30_000L);
        template.setBackOffPolicy(backOffPolicy);
        return template;
    }
}
