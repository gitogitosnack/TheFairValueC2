package org.example;

// バッチ専用メインクラス（将来対応、現時点では未使用）。
// WebApplicationType.NONE で組み込み Tomcat を起動せず、--job=dailyQuote / --job=financialStatement
// 引数で MarketDataBatchRunner に対象 Job を判定させ、完了後 System.exit(SpringApplication.exit(...)) で
// プロセスを終了する。pom.xml の spring-boot-maven-plugin で TheFairValue と別 classifier の
// 実行可能 JAR として出力する想定（設計書 3.1「起動プロセスの分離」参照）。
