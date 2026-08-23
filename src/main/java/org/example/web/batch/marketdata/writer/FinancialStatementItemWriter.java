package org.example.web.batch.marketdata.writer;

// ItemWriter<FinancialStatementEntity>。chunk 単位で financial_statements へ INSERT し、
// INSERT した各行について AnalysisIndicatorRecalcService を呼び出し analysis_indicators を再計算・INSERT する。
// usFinancialStatementStep・jpFinancialStatementStep の両方で共用する（設計書 4.3.1・4.3.2・4.3.3 参照）。
