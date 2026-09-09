package org.example.web.batch.marketdata.service;

// 起動経路（外部スケジューラ／内蔵スケジューラ／手動実行）をまたいだ排他制御のインタフェース。
// tryLock(batchType) / unlock(batchType) を提供する（設計書 3.4 参照）。
public interface BatchExecutionLockService {

    /**
     * 指定バッチ種別のロック取得を試みる。取得できれば true、既に他経路が実行中であれば false を返す。
     */
    boolean tryLock(BatchType batchType);

    /**
     * tryLock(batchType) で取得したロックを解放する。呼び出し側は try-finally で必ず呼ぶこと。
     */
    void unlock(BatchType batchType);
}
