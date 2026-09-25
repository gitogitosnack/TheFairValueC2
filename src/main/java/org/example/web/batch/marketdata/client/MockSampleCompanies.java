package org.example.web.batch.marketdata.client;

import java.util.List;

// モックモード（marketdata.api.mock-enabled=true）で株価・財務諸表のサンプルデータを返す対象銘柄。
// 日本株・米国株・フィリピン株から2社ずつ、companies に実際に登録済みのコードを選定している。
// MockStockPriceProviderImpl / MockFinancialDataProviderImpl のサンプルデータの key、および
// EdinetDocumentListTasklet がモック時に日本株の対象を絞り込む際に参照する（実際の EDINET 呼び出しを行わないため）。
public final class MockSampleCompanies {

    // トヨタ自動車, ソニーグループ
    public static final List<String> JP_CODES = List.of("7203", "6758");
    // Apple, Microsoft
    public static final List<String> US_CODES = List.of("AAPL", "MSFT");
    // BDOユニバンク, フィリピン諸島銀行（BPI）
    public static final List<String> PH_CODES = List.of("BDO", "BPI");

    // EDINET は銘柄コード指定の単発取得APIを持たず docID 単位でしか書類を取得できないため、
    // モック時は EdinetDocumentListTasklet がこのプレフィックス＋銘柄コードを docID として
    // 合成し、JpFinancialStatementItemProcessor 側で銘柄コードへ復元する（実際の EDINET 通信は行わない）。
    public static final String JP_MOCK_DOC_ID_PREFIX = "MOCK-";

    private MockSampleCompanies() {
    }
}
