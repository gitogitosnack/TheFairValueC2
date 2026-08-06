document.addEventListener("DOMContentLoaded", () => {
  // ===================================
  // シミュレーションパラメータセクション
  // ===================================

  // pv_calculator.js が読み込めていれば計算エンジンを準備する
  const calculator =
    typeof pv_calculator !== "undefined" ? new pv_calculator() : null;

  // 単位コードに応じたスライダー表示用の単位記号
  function unitSuffix(unit) {
    if (unit === "TIMES") return "倍";
    if (unit === "YEARS") return "年";
    return "%";
  }

  // モデルごとの現在のパラメータ値を保持するワーキングコピー。
  // スライダー操作で書き換わり、再計算のたびにこの値を使う。
  // { [modelId]: { modelCode, parameters: {code: value}, parameterDefs: [...] } }
  const modelState = {};
  (typeof theoreticalModels !== "undefined" ? theoreticalModels : []).forEach(
    (model) => {
      const parameters = {};
      (model.parameters || []).forEach((p) => {
        parameters[p.parameterCode] =
          p.defaultValue != null ? Number(p.defaultValue) : 0;
      });
      modelState[model.modelId] = {
        modelCode: model.modelCode,
        parameters: parameters,
        parameterDefs: model.parameters || [],
      };
    },
  );

  // << 将来5年間の成長率, 永久成長率(sliderの設定・動きを複数同時に制御) >>
  // 1. モデルごとのパラメータグループを取得し、スライダーが動いたら
  //    ラベル表示とワーキングコピーの両方を更新する。
  document.querySelectorAll(".model-param-group").forEach((modelGroup) => {
    const modelId = modelGroup.dataset.modelId;

    modelGroup.querySelectorAll(".slider-param-group").forEach((group) => {
      // グループ「内」にあるスライダーと表示用要素をピンポイントで取得
      const slider = group.querySelector(".slider");
      const valueDisplay = group.querySelector(".slider-value");
      const unit = slider.dataset.unit;
      const parameterCode = slider.dataset.parameterCode;

      const updateLabel = (value) => {
        valueDisplay.textContent = `${parseFloat(value).toFixed(2)}${unitSuffix(unit)}`;
      };

      // 初期状態の数値（HTMLに書いた初期値）を反映させる
      updateLabel(slider.value);

      // スライダーが動くたびに、ラベル・ワーキングコピーの更新に加えて
      // その場で理論株価を再計算し画面に反映する（ボタン操作は不要）
      slider.addEventListener("input", (event) => {
        const currentValue = parseFloat(event.target.value);
        updateLabel(currentValue);
        if (modelState[modelId]) {
          modelState[modelId].parameters[parameterCode] = currentValue;
        }
        recalculateAndRender();
      });
    });
  });

  // 円表示用のフォーマッタ（小数第1位まで）
  function formatYen(value) {
    return new Intl.NumberFormat("ja-JP", {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
  }

  // モデル1件分の理論株価（点推定値 + パラメータ感応度レンジ）を算出する。
  // 算出に必要なデータが無い/パラメータが不正な場合は { error } を返す。
  function evaluateModel(state) {
    if (!calculator) {
      return { error: "計算エンジンを読み込めませんでした。" };
    }

    let point;
    try {
      point = calculator.calculate(
        state.modelCode,
        state.parameters,
        typeof financialData !== "undefined" ? financialData : {},
      );
    } catch (error) {
      return { error: error.message };
    }

    // 各パラメータをスライダーの下限/上限まで振ったときの理論株価から
    // 感応度レンジ（低位〜高位の目安）を作る。
    const minParams = Object.assign({}, state.parameters);
    const maxParams = Object.assign({}, state.parameters);
    state.parameterDefs.forEach((def) => {
      if (def.minValue != null) {
        minParams[def.parameterCode] = Number(def.minValue);
      }
      if (def.maxValue != null) {
        maxParams[def.parameterCode] = Number(def.maxValue);
      }
    });

    let low = point;
    let high = point;
    try {
      const atMin = calculator.calculate(state.modelCode, minParams, financialData);
      const atMax = calculator.calculate(state.modelCode, maxParams, financialData);
      low = Math.min(atMin, atMax, point);
      high = Math.max(atMin, atMax, point);
    } catch (rangeError) {
      // レンジ側の算出に失敗しても、点推定が出ていればそれだけで表示を続ける
      low = point;
      high = point;
    }

    return { point: point, low: low, high: high };
  }

  // ===================================
  // 理論株価レンジ比較セクション
  // ===================================

  // 1行分（1モデル分）のレンジバー・判定ラベルを描画する
  function renderRangeRow(row, result) {
    const judgementEl = row.querySelector('[data-role="price-judgement"]');
    const rangeBarEl = row.querySelector('[data-role="range-bar"]');
    const priceMarkerEl = row.querySelector('[data-role="price-marker"]');
    const insufficientEl = row.querySelector(
      '[data-role="insufficient-message"]',
    );

    if (result.error) {
      judgementEl.textContent = "算出不可";
      judgementEl.className = "font-mono font-bold text-gray-500";
      rangeBarEl.classList.add("hidden");
      priceMarkerEl.classList.add("hidden");
      if (insufficientEl) {
        insufficientEl.textContent = result.error;
        insufficientEl.classList.remove("hidden");
      }
      return;
    }

    if (insufficientEl) {
      insufficientEl.classList.add("hidden");
    }
    rangeBarEl.classList.remove("hidden");
    priceMarkerEl.classList.remove("hidden");

    // 現在株価との乖離率で 割安 / 割高 / 適正水準 を判定する
    const cp = typeof currentPrice !== "undefined" ? currentPrice : 0;
    const diffRatio = result.point !== 0 ? (cp - result.point) / result.point : 0;

    let judgementClass;
    let judgementLabel;
    if (diffRatio <= -0.05) {
      judgementLabel = "割高";
      judgementClass = "font-mono font-bold text-rose-400";
    } else if (diffRatio >= 0.05) {
      judgementLabel = "割安";
      judgementClass = "font-mono font-bold text-emerald-400";
    } else {
      judgementLabel = "適正水準";
      judgementClass = "font-mono font-bold text-gray-300";
    }
    judgementEl.textContent = `${formatYen(result.point)} 円 (${judgementLabel})`;
    judgementEl.className = judgementClass;

    // レンジと現在株価の両方が収まるよう余白を持たせた表示スケールを作る
    const scaleMin = Math.min(result.low, cp) * 0.85;
    const scaleMax = Math.max(result.high, cp) * 1.15;
    const scaleWidth = scaleMax - scaleMin || 1;
    const toPercent = (value) =>
      Math.min(100, Math.max(0, ((value - scaleMin) / scaleWidth) * 100));

    const leftPercent = toPercent(result.low);
    const rightPercent = toPercent(result.high);
    rangeBarEl.style.left = `${leftPercent}%`;
    rangeBarEl.style.width = `${Math.max(rightPercent - leftPercent, 1)}%`;
    priceMarkerEl.style.left = `${toPercent(cp)}%`;
  }

  // ===================================
  // 各モデル結果セクション（モデル別詳細カード）
  // ===================================

  // 1枚分のモデル詳細カードの算出結果表示を描画する
  function renderModelCard(card, result) {
    const priceEl = card.querySelector('[data-role="card-price"]');
    const unitEl = card.querySelector('[data-role="card-price-unit"]');
    if (!priceEl) return;

    if (result.error) {
      priceEl.textContent = "算出不可";
      priceEl.className = "text-base text-gray-500";
      if (unitEl) unitEl.textContent = "";
      return;
    }

    priceEl.textContent = formatYen(result.point);
    priceEl.className = "";
    if (unitEl) unitEl.textContent = "円";
  }

  // モデルごとに再計算し、レンジ比較行と詳細カードの両方を再描画する
  function recalculateAndRender() {
    Object.keys(modelState).forEach((modelId) => {
      const result = evaluateModel(modelState[modelId]);

      const row = document.querySelector(
        `.range-row[data-model-id="${modelId}"]`,
      );
      if (row) {
        renderRangeRow(row, result);
      }

      const card = document.querySelector(
        `.model-card[data-model-id="${modelId}"]`,
      );
      if (card) {
        renderModelCard(card, result);
      }
    });
  }

  // 初期表示時にDBの既定値で1回算出する
  recalculateAndRender();

  // ===================================
  // パラメータ初期値の保存（DB更新）
  // ===================================

  // 「この値をパラメータ初期値としてDBに保存する」ボタン：
  // スライダーの現在値を、この銘柄のパラメータ初期値としてDBに保存する。
  // 数値の再計算自体はスライダー操作のたびに自動で行われるため、
  // このボタンの役割は「再計算」ではなく「今の値を初期値として確定・保存」。
  const saveDefaultsButton = document.getElementById("btn-save-defaults");
  if (saveDefaultsButton) {
    const defaultLabel = saveDefaultsButton.textContent;

    saveDefaultsButton.addEventListener("click", () => {
      const code = saveDefaultsButton.dataset.code;
      const parameters = [];
      Object.keys(modelState).forEach((modelId) => {
        const state = modelState[modelId];
        (state.parameterDefs || []).forEach((def) => {
          const value = state.parameters[def.parameterCode];
          if (value != null && !Number.isNaN(value)) {
            parameters.push({ parameterId: def.parameterId, value: value });
          }
        });
      });

      if (!code || parameters.length === 0) {
        return;
      }

      saveDefaultsButton.disabled = true;
      saveDefaultsButton.textContent = "保存中...";

      fetch(`/rest_stock_detail/${encodeURIComponent(code)}/parameter-defaults`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ parameters: parameters }),
      })
        .then((response) => {
          if (!response.ok) {
            throw new Error("保存に失敗しました");
          }
          saveDefaultsButton.textContent = "保存しました";
        })
        .catch(() => {
          saveDefaultsButton.textContent = "保存に失敗しました";
        })
        .finally(() => {
          setTimeout(() => {
            saveDefaultsButton.textContent = defaultLabel;
            saveDefaultsButton.disabled = false;
          }, 1500);
        });
    });
  }

  // ===================================
  // 主要指標セクション
  // ===================================

  // ===================================
  // 業績グラフセクション
  // ===================================
  $(".tab-btn").on("click", function () {
    const targetTab = $(this).data("tab");

    // ボタンの見た目の切り替え
    $(".tab-btn")
      .removeClass("border-emerald-500 text-emerald-400")
      .addClass("border-transparent text-gray-400 hover:text-white");
    $(this)
      .addClass("border-emerald-500 text-emerald-400")
      .removeClass("border-transparent text-gray-400");

    // コンテンツの切り替え表示
    $(".tab-content").addClass("hidden").removeClass("block");
    $("#" + targetTab)
      .removeClass("hidden")
      .addClass("block");
  });

  // 各カテゴリのグラフ初期化 (収益性)
  new Chart($("#profitChart"), {
    type: "line",
    data: {
      labels: chartData.fiscalYearLabels,
      datasets: [
        {
          label: "ROE",
          data: chartData.roeList,
          borderColor: "#2563eb",
          yAxisID: "y",
        },
        {
          label: "売上高総利益率",
          data: chartData.grossMarginList,
          borderColor: "#60a5fa",
          yAxisID: "y",
        },
        {
          label: "売上高純利益率",
          data: chartData.netMarginList,
          borderColor: "#94a3b8",
          yAxisID: "y",
        },
        {
          label: "EPS",
          data: chartData.epsList,
          backgroundColor: "rgba(4, 75, 48, 0.7)",
          type: "bar",
          yAxisID: "y1",
          barThickness: 30,
          maxBarThickness: 50,
        },
      ],
    },
    options: {
      maintainAspectRatio: false,
      scales: {
        y: {
          position: "right",
          grid: { drawOnChartArea: false },
          title: { display: true, text: "パーセント（％）" },
          beginAtZero: true,
        },
        y1: {
          position: "left",
          grid: { drawOnChartArea: false },
          title: { display: true, text: "EPS (円)" },
          beginAtZero: false,
          // 最小値の下に10%のバッファ（余白）を作る
          min: Math.min(...chartData.epsList.filter((v) => v !== null)) * 0.95,
          // 最大値の上に10%のバッファを作る
          suggestedMax:
            Math.max(...chartData.epsList.filter((v) => v !== null)) * 1.1,
        },
      },
    },
  });

  // まず最大値を計算する（efficiencyChartの外で実行）
  const allTurnoverValues = [
    ...chartData.assetTurnoverList,
    ...chartData.inventoryTurnoverList,
    ...chartData.receivablesTurnoverList,
  ].filter((v) => v !== null && v !== undefined);

  const maxVal =
    allTurnoverValues.length > 0 ? Math.max(...allTurnoverValues) : 10;

  // 計算した maxVal を使ってグラフを生成する
  new Chart($("#efficiencyChart"), {
    type: "line",
    data: {
      labels: chartData.fiscalYearLabels,
      datasets: [
        {
          label: "総資産回転率",
          data: chartData.assetTurnoverList,
          borderColor: "#94a3b8",
          backgroundColor: "#94a3b8",
          yAxisID: "y",
        },
        {
          label: "棚卸資産回転率",
          data: chartData.inventoryTurnoverList,
          borderColor: "#60a5fa",
          backgroundColor: "#60a5fa",
          yAxisID: "y",
        },
        {
          label: "売上債権回転率",
          data: chartData.receivablesTurnoverList,
          borderColor: "#2563eb",
          backgroundColor: "#2563eb",
          yAxisID: "y",
        },
      ],
    },
    options: {
      maintainAspectRatio: false,
      scales: {
        y: {
          beginAtZero: true,
          title: { display: true, text: "回転率 (回)" },
          suggestedMax: maxVal * 1.5, // ここで計算済みの変数を使う
        },
      },
    },
  });

  // 安全性グラフ (Safety Chart)
  // 自己資本比率を折れ線、D/Eレシオを棒グラフで表示する複合チャート
  new Chart($("#safetyChart"), {
    type: "bar",
    data: {
      labels: chartData.fiscalYearLabels,
      datasets: [
        {
          label: "自己資本比率 (%)",
          data: chartData.equityRatioList,
          type: "line",
          borderColor: "#10b981",
          backgroundColor: "#10b981",
          yAxisID: "y",
          tension: 0.1,
        },
        {
          label: "D/Eレシオ",
          data: chartData.debtEquityRatioList,
          backgroundColor: "rgba(148, 163, 184, 1.0)",
          yAxisID: "y1",
          barThickness: 20,
          maxBarThickness: 50,
        },
        {
          label: "インタレスト・カバレッジ・レシオ",
          data: chartData.interestCoverageRatioList,
          backgroundColor: "rgba(148, 163, 184, 0.5)",
          yAxisID: "y1",
          barThickness: 20,
          maxBarThickness: 50,
        },
      ],
    },
    options: {
      maintainAspectRatio: false,
      scales: {
        y: {
          position: "right",
          beginAtZero: true,
          title: { display: true, text: "自己資本比率 (%)" },
          // 右側のグリッド線を消してスッキリさせる
          grid: { drawOnChartArea: true },
          max:
            Math.max(...chartData.equityRatioList.filter((v) => v !== null)) *
            1.5,
        },
        y1: {
          type: "logarithmic", // ここを対数に設定
          position: "left",
          beginAtZero: true,
          title: { display: true, text: "倍率" },
          // 左側のグリッド線を消してスッキリさせる
          grid: { drawOnChartArea: false },
          // 対数軸の場合、0は表示できないので最小値に注意
          min: 0.1,
        },
      },
    },
  });

  // キャッシュフローグラフ (CF Chart)
  // 営業CFとフリーCFの推移を可視化する棒グラフ
  new Chart($("#cfChart"), {
    type: "bar",
    data: {
      labels: chartData.fiscalYearLabels,
      datasets: [
        {
          label: "フリーCF",
          data: chartData.fcfList,
          backgroundColor: "rgba(148, 163, 184, 0.2)",
          barThickness: 20,
          maxBarThickness: 50,
        },
        {
          label: "営業CFマージン",
          data: chartData.operationCfMarginList,
          type: "line",
          backgroundColor: "#020508",
          borderColor: "#020306",
          yAxisID: "y1",
          tension: 0.1,
        },
      ],
    },
    options: {
      maintainAspectRatio: false,
      interaction: {
        mode: "index",
        intersect: false,
      },
      scales: {
        y: {
          position: "left",
          beginAtZero: false,
          title: { display: true, text: "フリーCF (百万円)" },
          min: Math.min(...chartData.fcfList.filter((v) => v !== null)) * 0.95,
        },
        y1: {
          position: "right",
          beginAtZero: true,
          title: { display: true, text: "営業CFマージン (%)" },
          // 右側のグリッドを消してスッキリ
          grid: { drawOnChartArea: false },
          max:
            Math.max(
              ...chartData.operationCfMarginList.filter((v) => v !== null),
            ) * 1.5,
        },
      },
    },
  });

  // ===================================
  // 共通処理セクション
  // ===================================
});
