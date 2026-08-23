package org.example.web.batch.marketdata.service;

// 財務データ更新後の analysis_indicators（ROE・PER・自己資本比率等）再計算のインタフェース。
// 算出ロジック本体はここに実装せず、既存の評価モデル機能側（web.stock.valuationmodel 等）のロジックを
// 呼び出すだけのオーケストレーション役に徹する（設計書 4.3.3・11 章 回答 6 参照）。
