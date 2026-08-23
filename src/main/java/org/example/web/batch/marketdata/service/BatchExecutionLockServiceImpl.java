package org.example.web.batch.marketdata.service;

// BatchExecutionLockService の実装クラス。PostgreSQL のアドバイザリロック
// （pg_try_advisory_lock / pg_advisory_unlock）でプロセスをまたいだ排他制御を行う。
// ロックキーはバッチ種別ごとの固定整数（日次株価=1、財務諸表=2）を使う（設計書 3.4 参照）。
