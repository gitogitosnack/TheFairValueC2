
package org.example.web.batch.marketdata.client;

import org.example.web.batch.marketdata.client.dto.FinancialStatementData;

// 財務データ取得の抽象化インタフェース。銘柄の country_id / currency_id から実装（Bean）を切り替える
// （EdinetFinancialDataProviderImpl / FmpFinancialDataProviderImpl、設計書 4.1 参照）。
public interface FinancialDataProvider {

    /**
     * 指定銘柄コードの最新四半期の財務諸表を取得する。取得対象が存在しない場合は null を返す。
     * 429（レート制限）の場合は {@link RateLimitException} を throw する。
     *
     * <p>
     * EDINET（日本株）は銘柄コード指定の単発取得 API を持たないため、
     * {@link EdinetFinancialDataProviderImpl} はこのメソッドを実装せず
     * {@code fetchDocumentList()} / {@code fetchStatementDocument()} による
     * 2 段構成（Tasklet → chunk指向Step）で対応する（設計書 4.3.2 参照）。
     */
    FinancialStatementData fetchLatestStatement(String code);
}
