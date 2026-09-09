package org.example.web.batch.marketdata.service;

// バッチ種別ごとの PostgreSQL アドバイザリロックキー（設計書 3.4 参照）。
// 財務諸表同期は米国株・日本株を分けず単一のロックキーとする（11 章 回答 10）。
public enum BatchType {

    DAILY_QUOTE(1),
    FINANCIAL_STATEMENT(2);

    private final int lockKey;

    BatchType(int lockKey) {
        this.lockKey = lockKey;
    }

    public int getLockKey() {
        return lockKey;
    }
}
