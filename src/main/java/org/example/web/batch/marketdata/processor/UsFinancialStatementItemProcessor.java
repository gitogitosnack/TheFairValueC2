package org.example.web.batch.marketdata.processor;

// ItemProcessor<CompanyEntity, FinancialStatementEntity>。financial_statements の最新
// (fiscal_year, fiscal_quarter) と FmpFinancialDataProviderImpl.fetchLatestStatement(code) の結果を比較し、
// API 側が新しければ FinancialStatementEntity を返す。差分がなければ null を返し chunk から除外する（設計書 4.3.1 参照）。
