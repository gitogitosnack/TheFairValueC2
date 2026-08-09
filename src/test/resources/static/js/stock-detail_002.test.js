import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import $ from "jquery";
import "../../../../main/resources/static/js/pv_calculator.js";

function baseChartData(overrides = {}) {
  return Object.assign(
    {
      fiscalYearLabels: ["2023", "2024", "2025"],
      roeList: [8, 9, 10],
      grossMarginList: [30, 31, 32],
      netMarginList: [5, 6, 7],
      epsList: [100, 110, 120],
      assetTurnoverList: [1, 1.1, 1.2],
      inventoryTurnoverList: [2, 2.1, 2.2],
      receivablesTurnoverList: [3, 3.1, 3.2],
      equityRatioList: [40, 41, 42],
      debtEquityRatioList: [1, 1, 1],
      interestCoverageRatioList: [5, 5, 5],
      fcfList: [100, 110, 120],
      operationCfMarginList: [10, 11, 12],
      companyCode: "7203",
      companyName: "トヨタ自動車",
    },
    overrides,
  );
}

function baseFinancialData(overrides = {}) {
  return Object.assign(
    {
      fcf: 120000000,
      sharesOutstanding: 10000000,
      eps: 100,
      bps: 800,
      dividendPerShare: 30,
      revenue: 5000000000,
      ebitda: 1000000000,
      totalAssets: 10000000000,
      totalEquity: 4500000000,
      cashAndEquivalents: 500000000,
      interestBearingDebt: 350000000,
    },
    overrides,
  );
}

function mainSectionsHtml() {
  return `
    <div class="model-param-group" data-model-id="1">
      <div class="slider-param-group">
        <input class="slider" type="range" min="10" max="20" step="1" value="15"
          data-unit="TIMES" data-parameter-code="target_per" />
        <span class="slider-value"></span>
      </div>
    </div>
    <div class="range-row" data-model-id="1">
      <span data-role="price-judgement"></span>
      <div data-role="range-bar"></div>
      <div data-role="price-marker"></div>
      <span data-role="insufficient-message" class="hidden"></span>
    </div>
    <div class="model-card" data-model-id="1">
      <span data-role="card-price"></span>
      <span data-role="card-price-unit"></span>
    </div>
    <button id="btn-save-defaults" data-code="7203">初期値として保存</button>
    <button class="tab-btn" data-tab="tab1">タブ1</button>
    <button class="tab-btn" data-tab="tab2">タブ2</button>
    <div id="tab1" class="tab-content block"></div>
    <div id="tab2" class="tab-content hidden"></div>
    <canvas id="profitChart"></canvas>
    <canvas id="efficiencyChart"></canvas>
    <canvas id="safetyChart"></canvas>
    <canvas id="cfChart"></canvas>`;
}

function competitorModalHtml() {
  return `
    <div id="compareModal" class="hidden">
      <span id="closeCompareModal"></span>
      <span id="cancelCompareModal"></span>
      <div id="competitorChips"></div>
      <input id="competitorSearchInput" />
      <datalist id="competitorCompanyOptions"></datalist>
      <div id="competitorAddError" class="hidden"></div>
      <span id="compareHeaderSelf"></span>
      <span id="compareHeaderA"></span>
      <span id="compareHeaderB"></span>
      <span id="compareHeaderC"></span>
      <button id="addCompetitorBtn"></button>
    </div>
    <button id="openCompareModal"></button>`;
}

function fetchMock({ competitors = [], companies = [] } = {}) {
  const calls = [];
  const fn = vi.fn((url, opts) => {
    calls.push({ url, opts });
    if (url.startsWith("/rest_company_competitors/by-company/")) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve(competitors) });
    }
    if (url === "/rest_company_competitors/companies") {
      return Promise.resolve({ ok: true, json: () => Promise.resolve(companies) });
    }
    if (url === "/rest_company_competitors/insert") {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    }
    if (url.startsWith("/rest_company_competitors/delete/")) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    }
    if (url.startsWith("/rest_stock_detail/")) {
      return Promise.resolve({ ok: true });
    }
    return Promise.reject(new Error("unexpected fetch url: " + url));
  });
  fn.calls = calls;
  return fn;
}

function flush() {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

async function loadModule() {
  vi.resetModules();
  // document.addEventListener("DOMContentLoaded", ...) を実際に発火させると
  // テストをまたいで登録済みハンドラが積み重なってしまうため、
  // 登録されたハンドラを捕まえて直接1回だけ呼び出す。
  const addSpy = vi.spyOn(document, "addEventListener");
  await import("../../../../main/resources/static/js/stock-detail_002.js");
  const registeredCall = addSpy.mock.calls.find(([type]) => type === "DOMContentLoaded");
  addSpy.mockRestore();
  registeredCall[1]();
  await flush();
}

describe("stock-detail_002.js", () => {
  let chartCalls;

  beforeEach(() => {
    global.$ = $;
    global.jQuery = $;
    chartCalls = [];
    global.Chart = class {
      constructor(ctx, config) {
        chartCalls.push({ ctx, config });
      }
    };
    global.chartData = baseChartData();
    global.currentPrice = 2500;
    global.financialData = baseFinancialData();
    global.theoreticalModels = [
      {
        modelId: 1,
        modelCode: "PER_MULTIPLE",
        parameters: [
          {
            parameterId: 100,
            parameterCode: "target_per",
            defaultValue: 15,
            minValue: 10,
            maxValue: 20,
          },
        ],
      },
    ];
    global.fetch = fetchMock();
    document.body.innerHTML = mainSectionsHtml() + competitorModalHtml();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    delete global.chartData;
    delete global.currentPrice;
    delete global.financialData;
    delete global.theoreticalModels;
    delete global.Chart;
    delete global.fetch;
  });

  test("初期表示時に既定値でモデルカードとレンジ行が算出されること", async () => {
    await loadModule();

    // PER_MULTIPLE: eps(100) * target_per(15) = 1500
    expect(document.querySelector('[data-role="card-price"]').textContent).toBe("1,500.0");
    expect(document.querySelector('[data-role="card-price-unit"]').textContent).toBe("円");
    expect(document.querySelector('[data-role="price-judgement"]').textContent).toContain(
      "1,500.0 円",
    );
    expect(document.querySelector('[data-role="price-judgement"]').textContent).toContain(
      "割安",
    );
  });

  test("チャートが4件生成されること", async () => {
    await loadModule();

    expect(chartCalls).toHaveLength(4);
  });

  test("スライダーを動かすとラベルと算出結果が再計算されること", async () => {
    await loadModule();

    const slider = document.querySelector(".slider");
    slider.value = "18";
    slider.dispatchEvent(new Event("input"));

    expect(document.querySelector(".slider-value").textContent).toBe("18.00倍");
    // PER_MULTIPLE: eps(100) * target_per(18) = 1800
    expect(document.querySelector('[data-role="card-price"]').textContent).toBe("1,800.0");
    expect(document.querySelector('[data-role="price-judgement"]').textContent).toContain(
      "1,800.0 円",
    );
  });

  test("計算に必要なデータが不足しているとき算出不可と表示されること", async () => {
    global.theoreticalModels = [
      {
        modelId: 1,
        modelCode: "EV_EBITDA",
        parameters: [{ parameterId: 200, parameterCode: "target_ev_ebitda", defaultValue: 8 }],
      },
    ];
    global.financialData = baseFinancialData({ ebitda: null });

    await loadModule();

    expect(document.querySelector('[data-role="card-price"]').textContent).toBe("算出不可");
    expect(document.querySelector('[data-role="price-judgement"]').textContent).toBe("算出不可");
    expect(document.querySelector('[data-role="range-bar"]').classList.contains("hidden")).toBe(
      true,
    );
    const insufficientEl = document.querySelector('[data-role="insufficient-message"]');
    expect(insufficientEl.classList.contains("hidden")).toBe(false);
    expect(insufficientEl.textContent).toContain("データが不足しているため計算できません");
  });

  test("タブボタンを押すと表示タブが切り替わること", async () => {
    await loadModule();

    document.querySelector('.tab-btn[data-tab="tab2"]').click();

    expect(document.getElementById("tab1").classList.contains("hidden")).toBe(true);
    expect(document.getElementById("tab2").classList.contains("hidden")).toBe(false);
    expect(document.getElementById("tab2").classList.contains("block")).toBe(true);
  });

  test("保存ボタンを押すとパラメータ初期値がPOSTされ成功時にボタン表示が変わること", async () => {
    await loadModule();
    const button = document.getElementById("btn-save-defaults");

    button.click();
    expect(button.textContent).toBe("保存中...");
    expect(button.disabled).toBe(true);

    await flush();

    expect(button.textContent).toBe("保存しました");
    const call = global.fetch.calls.find((c) => c.url.startsWith("/rest_stock_detail/"));
    expect(call.url).toBe("/rest_stock_detail/7203/parameter-defaults");
    expect(call.opts.method).toBe("POST");
    const body = JSON.parse(call.opts.body);
    expect(body.parameters).toEqual([{ parameterId: 100, value: 15 }]);
  });

  test("保存に失敗したときボタン表示が失敗メッセージになること", async () => {
    global.fetch = vi.fn(() => Promise.resolve({ ok: false }));
    await loadModule();
    const button = document.getElementById("btn-save-defaults");

    button.click();
    await flush();

    expect(button.textContent).toBe("保存に失敗しました");
  });

  test("コード属性が無いとき保存ボタンを押してもfetchが呼ばれないこと", async () => {
    document.getElementById("btn-save-defaults").removeAttribute("data-code");
    await loadModule();

    document.getElementById("btn-save-defaults").click();

    const saveCalls = global.fetch.calls.filter((c) => c.url.startsWith("/rest_stock_detail/"));
    expect(saveCalls).toHaveLength(0);
  });

  test("比較モーダルを開くと競合企業と候補一覧を取得すること", async () => {
    global.fetch = fetchMock({
      competitors: [{ id: 1, competitorCode: "7267", competitorName: "ホンダ" }],
      companies: [
        { code: "7203", name: "トヨタ自動車" },
        { code: "7267", name: "ホンダ" },
      ],
    });
    await loadModule();

    document.getElementById("openCompareModal").click();
    await flush();

    expect(document.getElementById("compareHeaderSelf").textContent).toBe(
      "トヨタ自動車 (7203)",
    );
    expect(document.getElementById("competitorChips").textContent).toContain("ホンダ (7267)");
    // 自社は候補から除外されること
    const optionValues = Array.from(
      document.querySelectorAll("#competitorCompanyOptions option"),
    ).map((o) => o.value);
    expect(optionValues).toEqual(["ホンダ (7267)"]);
  });

  test("候補一覧に無い文字列で競合追加しようとするとエラーが表示されること", async () => {
    global.fetch = fetchMock({
      companies: [{ code: "7267", name: "ホンダ" }],
    });
    await loadModule();
    document.getElementById("openCompareModal").click();
    await flush();

    document.getElementById("competitorSearchInput").value = "存在しない企業";
    document.getElementById("addCompetitorBtn").click();

    expect(document.getElementById("competitorAddError").classList.contains("hidden")).toBe(
      false,
    );
    expect(document.getElementById("competitorAddError").textContent).toBe(
      "候補一覧から企業を選択してください。",
    );
    const insertCalls = global.fetch.calls.filter((c) => c.url === "/rest_company_competitors/insert");
    expect(insertCalls).toHaveLength(0);
  });

  test("候補一覧にある企業を選んで追加すると登録APIが呼ばれ検索欄がクリアされること", async () => {
    global.fetch = fetchMock({
      companies: [{ code: "7267", name: "ホンダ" }],
    });
    await loadModule();
    document.getElementById("openCompareModal").click();
    await flush();

    document.getElementById("competitorSearchInput").value = "ホンダ (7267)";
    document.getElementById("addCompetitorBtn").click();
    await flush();

    const insertCalls = global.fetch.calls.filter((c) => c.url === "/rest_company_competitors/insert");
    expect(insertCalls).toHaveLength(1);
    const body = JSON.parse(insertCalls[0].opts.body);
    expect(body).toEqual({ companyCode: "7203", competitorCompanyCode: "7267" });
    expect(document.getElementById("competitorSearchInput").value).toBe("");
  });

  test("競合チップの×ボタンを押すと削除APIが呼ばれること", async () => {
    global.fetch = fetchMock({
      competitors: [{ id: 42, competitorCode: "7267", competitorName: "ホンダ" }],
    });
    await loadModule();
    document.getElementById("openCompareModal").click();
    await flush();

    document.querySelector("#competitorChips button").click();
    await flush();

    const deleteCalls = global.fetch.calls.filter((c) =>
      c.url.startsWith("/rest_company_competitors/delete/"),
    );
    expect(deleteCalls).toHaveLength(1);
    expect(deleteCalls[0].url).toBe("/rest_company_competitors/delete/42");
    expect(deleteCalls[0].opts.method).toBe("DELETE");
  });
});
