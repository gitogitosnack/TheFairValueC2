package org.example.web.batch.marketdata.client;

// EDINET は認証エラー等でも HTTP 200 を返し、本文に {"StatusCode": 401, "message": "..."} という
// エラー用の JSON を埋め込む（documents.json が results を含む正常レスポンスと形が異なる）。
// RestClient 側では例外にならないため、EdinetFinancialDataProviderImpl 側でこのレスポンス形を検知して
// 明示的に throw し、他のAPI呼び出し（4xx/5xx）と同様にバッチ失敗として扱えるようにする。
public class EdinetApiException extends RuntimeException {

    public EdinetApiException(String message) {
        super(message);
    }
}
