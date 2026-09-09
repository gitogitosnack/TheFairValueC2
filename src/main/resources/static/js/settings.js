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
  // POST /rest_market_data_sync/quote, /rest_market_data_sync/financial-statement
  // ===================================
  const marketDataSyncButtons = [
    { role: "sync-quote", url: "/rest_market_data_sync/quote" },
    { role: "sync-financial-statement", url: "/rest_market_data_sync/financial-statement" },
  ];

  marketDataSyncButtons.forEach(({ role, url }) => {
    const button = document.querySelector(`[data-role='${role}']`);
    const status = document.querySelector(`[data-role='${role}-status']`);
    if (!button || !status) return;

    button.addEventListener("click", async () => {
      button.disabled = true;
      status.className = "marketdata-sync-status";
      status.textContent = "実行中...";

      try {
        const response = await fetch(url, { method: "POST" });
        const body = await response.json().catch(() => ({}));

        if (response.status === 409) {
          status.className = "marketdata-sync-status error";
          status.textContent = body.message || "現在実行中のため開始できません";
        } else if (!response.ok) {
          status.className = "marketdata-sync-status error";
          status.textContent = body.message || "実行に失敗しました";
        } else {
          status.className = "marketdata-sync-status success";
          const counts = `読込${body.readCount ?? 0}件 / 更新${body.writeCount ?? 0}件`;
          status.textContent = `${body.message || "実行が完了しました"}（${counts}）`;
        }
      } catch (e) {
        status.className = "marketdata-sync-status error";
        status.textContent = "通信に失敗しました";
      } finally {
        button.disabled = false;
      }
    });
  });

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
