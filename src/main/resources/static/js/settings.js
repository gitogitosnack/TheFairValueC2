document.addEventListener("DOMContentLoaded", () => {
  // ===================================
  // 保存ボタン（サンプルUIのため実際にはAjax保存せず、トースト表示のみ）
  // ===================================
  document.querySelectorAll("[data-role^='save-']").forEach((button) => {
    button.addEventListener("click", () => {
      const toastRole = button.dataset.role.replace("save-", "") + "-toast";
      const toast = document.querySelector(`[data-role='${toastRole}']`);
      if (!toast) return;

      toast.classList.add("show");
      window.clearTimeout(toast._hideTimer);
      toast._hideTimer = window.setTimeout(() => {
        toast.classList.remove("show");
      }, 2000);
    });
  });

  // ===================================
  // テーマカラー：アクセントカラースウォッチ
  // ===================================
  const swatchRow = document.querySelector("[data-role='color-swatches']");
  if (swatchRow) {
    swatchRow.querySelectorAll(".color-swatch").forEach((swatch) => {
      swatch.addEventListener("click", () => {
        swatchRow
          .querySelectorAll(".color-swatch")
          .forEach((el) => el.classList.remove("selected"));
        swatch.classList.add("selected");
      });
    });
  }

  // ===================================
  // テーマカラー：ダーク/ライト表示モード切替
  // ===================================
  const modeToggle = document.querySelector("[data-role='mode-toggle']");
  if (modeToggle) {
    modeToggle.querySelectorAll(".mode-toggle-btn").forEach((button) => {
      button.addEventListener("click", () => {
        modeToggle
          .querySelectorAll(".mode-toggle-btn")
          .forEach((el) => el.classList.remove("active"));
        button.classList.add("active");
      });
    });
  }

  // ===================================
  // 財務データ更新（銘柄データ定期取得バッチ・全銘柄一括の手動実行ボタン）
  // 日次株価→財務諸表の順に POST /rest_market_data_sync/quote, /rest_market_data_sync/financial-statement を実行する。
  // 各ステップの成否・件数はボタン下のテキストに、成否の内訳（失敗時はエラーのジャンル別メッセージ）は
  // ダイアログにまとめて表示する（レスポンスの errorMessage はサーバ側で分類済みの定型文言のため、
  // 例外の詳細はサーバログのみに残る）。
  // ===================================
  const marketDataSyncButton = document.querySelector("[data-role='sync-market-data']");
  const marketDataSyncStatus = document.querySelector("[data-role='sync-market-data-status']");
  const marketDataSyncResultModal = document.querySelector("[data-role='market-data-sync-result-modal']");
  const marketDataSyncResultList = document.querySelector("[data-role='market-data-sync-result-list']");

  const closeMarketDataSyncResultModal = () => marketDataSyncResultModal?.classList.remove("show");

  if (marketDataSyncResultModal) {
    marketDataSyncResultModal.addEventListener("click", (event) => {
      if (event.target === marketDataSyncResultModal) closeMarketDataSyncResultModal();
    });
    marketDataSyncResultModal
      .querySelectorAll("[data-role='market-data-sync-result-close']")
      .forEach((el) => el.addEventListener("click", closeMarketDataSyncResultModal));
  }

  const renderMarketDataSyncResultDialog = (items) => {
    if (!marketDataSyncResultModal || !marketDataSyncResultList) return;

    marketDataSyncResultList.innerHTML = "";
    items.forEach(({ label, ok, text }) => {
      const li = document.createElement("li");
      li.className = `marketdata-sync-result-item ${ok ? "success" : "error"}`;

      const icon = document.createElement("i");
      icon.className = `fa-solid ${ok ? "fa-circle-check" : "fa-circle-exclamation"}`;

      const span = document.createElement("span");
      span.textContent = `${label}：${text}`;

      li.append(icon, span);
      marketDataSyncResultList.appendChild(li);
    });

    marketDataSyncResultModal.classList.add("show");
  };

  if (marketDataSyncButton && marketDataSyncStatus) {
    const marketDataSyncSteps = [
      { label: "日次株価", url: "/rest_market_data_sync/quote" },
      { label: "財務諸表", url: "/rest_market_data_sync/financial-statement" },
    ];

    marketDataSyncButton.addEventListener("click", async () => {
      marketDataSyncButton.disabled = true;
      marketDataSyncStatus.className = "marketdata-sync-status";

      const results = [];
      const dialogItems = [];
      let hasError = false;

      for (const { label, url } of marketDataSyncSteps) {
        marketDataSyncStatus.textContent = [...results, `${label}を実行中...`].join(" / ");

        try {
          const response = await fetch(url, { method: "POST" });
          const body = await response.json().catch(() => ({}));

          if (!body.success) {
            const detail = body.errorMessage || body.message || "実行に失敗しました";
            results.push(`${label}: ${detail}`);
            dialogItems.push({ label, ok: false, text: detail });
            hasError = true;
            break;
          }

          const counts = `読込${body.readCount ?? 0}件 / 更新${body.writeCount ?? 0}件`;
          const successText = `${body.message || "実行が完了しました"}（${counts}）`;
          results.push(`${label}: ${successText}`);
          dialogItems.push({ label, ok: true, text: successText });
        } catch (e) {
          const detail = "サーバーに接続できませんでした";
          results.push(`${label}: ${detail}`);
          dialogItems.push({ label, ok: false, text: detail });
          hasError = true;
          break;
        }
      }

      marketDataSyncStatus.className = `marketdata-sync-status ${hasError ? "error" : "success"}`;
      marketDataSyncStatus.textContent = results.join(" / ");
      marketDataSyncButton.disabled = false;

      renderMarketDataSyncResultDialog(dialogItems);
    });
  }

  // ===================================
  // ログアウト（認証機能が未実装のため、銘柄一覧へ戻すだけのモック動作）
  // ===================================
  const logoutButton = document.getElementById("btn-logout-settings");
  if (logoutButton) {
    logoutButton.addEventListener("click", () => {
      window.location.href = "/top";
    });
  }
});
