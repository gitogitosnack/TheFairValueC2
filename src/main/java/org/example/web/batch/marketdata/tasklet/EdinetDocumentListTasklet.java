package org.example.web.batch.marketdata.tasklet;

// edinetDocumentListStep の実体（Tasklet、1回のみ実行）。
// EDINET の書類一覧 API（documents.json）をその日 1 回だけ取得し、docTypeCode で絞り込み、
// secCode（末尾 0 除去）と companies.code を突合した (companyId, docID) の一致リストを
// ExecutionContextPromotionListener 経由で Job の ExecutionContext へ格納する（設計書 4.3.2 参照）。
