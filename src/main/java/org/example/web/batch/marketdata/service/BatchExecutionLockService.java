package org.example.web.batch.marketdata.service;

// 起動経路（外部スケジューラ／内蔵スケジューラ／手動実行）をまたいだ排他制御のインタフェース。
// tryLock(batchType) / unlock(batchType) を提供する（設計書 3.4 参照）。
