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
  // ログアウト（認証機能が未実装のため、銘柄一覧へ戻すだけのモック動作）
  // ===================================
  const logoutButton = document.getElementById("btn-logout-settings");
  if (logoutButton) {
    logoutButton.addEventListener("click", () => {
      window.location.href = "/top";
    });
  }
});
