package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: 銘柄データ定期取得バッチの内蔵スケジューラ（MarketDataSyncScheduler）用。
// 実際にスケジュール登録されるのは MARKETDATA_SCHEDULER_ENABLED=true のときのみ
// （@ConditionalOnProperty、docs/batch-market-data-sync-design.md 3.1・3.4 参照）。
@EnableScheduling
@SpringBootApplication
public class TheFairValue {
    public static void main(String[] args) {
        SpringApplication.run(TheFairValue.class, args);
    }
}