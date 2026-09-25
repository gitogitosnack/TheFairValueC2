package org.example.web.batch.marketdata.controller;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.example.web.batch.marketdata.client.EdinetApiException;
import org.example.web.batch.marketdata.client.RateLimitException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

// 手動実行結果を画面ダイアログに出すための大まかなエラー分類。
// 原因例外の詳細（スタックトレース等）はサーバログのみに残し、画面には
// ジャンルごとの定型文言のみを返す（設計は settings 画面のダイアログ要件を参照）。
enum MarketDataSyncFailureCategory {

    LOCKED("既に更新処理が実行中です。完了してから再実行してください。"),
    RATE_LIMIT("外部APIのレート制限に達しました。時間を置いてから再実行してください。"),
    AUTH("外部APIの認証に失敗しました。APIキーの設定を確認してください。"),
    CONNECTION("外部サービスに接続できませんでした。サービスが起動しているか確認してください。"),
    UNKNOWN("更新処理中にエラーが発生しました。詳細はサーバログを確認してください。");

    private final String message;

    MarketDataSyncFailureCategory(String message) {
        this.message = message;
    }

    String getMessage() {
        return message;
    }

    static MarketDataSyncFailureCategory classify(Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            if (t instanceof RateLimitException) {
                return RATE_LIMIT;
            }
            if (t instanceof HttpClientErrorException httpEx) {
                int status = httpEx.getStatusCode().value();
                if (status == 401 || status == 403) {
                    return AUTH;
                }
            }
            if (t instanceof EdinetApiException) {
                // EDINET は認証エラー等でも HTTP 200 を返し、本文にエラーコードを埋め込むため
                // HttpClientErrorException にはならない（EdinetFinancialDataProviderImpl 参照）。
                return AUTH;
            }
            if (t instanceof ResourceAccessException || t instanceof ConnectException || t instanceof SocketTimeoutException) {
                return CONNECTION;
            }
        }
        return UNKNOWN;
    }
}
