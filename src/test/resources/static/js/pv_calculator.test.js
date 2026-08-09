import { describe, test, expect, beforeEach } from "vitest";
import "../../main/resources/static/js/pv_calculator.js";

describe("Spring Boot フロントエンドJSテスト", () => {
  let calculator;
  let validFinancialData;

  beforeEach(() => {
    calculator = new global.pv_calculator();

    // 各テスト毎のモックデータをセット
    validFinancialData = {
      non_operating_assets: {
        cash_and_equivalents: 500000000, // 5億円
        investment_securities: 300000000, // 3億円
      },
      interest_bearing_debt: {
        short_term_borrowings: 100000000, // 1億円
        current_portion_of_long_term_debt: 50000000, // 0.5億円
        corporate_bonds: 200000000, // 2億円
        long_term_borrowings: 400000000, // 4億円
        lease_obligations: 50000000, // 0.5億円
      },
      shares_outstanding: 10000000, // 1,000万株
    };
  });

  test("正常系: 正しいパラメータと決算データで適正株価 with DCF法 が正しく算出されること", () => {
    // 条件: FCF 1.2億, 成長率 5%, 割引率 9%, 永続成長率 2%
    const fairPrice = calculator.dcf_calc(
      120000000,
      0.05,
      0.09,
      0.02,
      validFinancialData,
    );
    // 事業価値(約19.87億円) ÷ 1000万株 = 約198.75円 になることを検証
    // 小数点第2位まで一致しているかチェック (198.75円)
    expect(fairPrice).toBeCloseTo(198.75, 2);
  });

  test("異常系: 割引率 <= 永続成長率 のときにエラーを投げること", () => {
    // 割引率(0.02) <= 永続成長率(0.02) でゴードンモデルが崩壊するケース
    expect(() => {
      calculator.dcf_calc(120000000, 0.05, 0.02, 0.02, validFinancialData);
    }).toThrow("割引率は永続成長率よりも大きくする必要があります。");
  });

  test("異常系: 発行済株式総数が 0 のときにエラーを投げること", () => {
    // 株式総数を 0 に上書き（ゼロ除算の防止チェック）
    validFinancialData.shares_outstanding = 0;

    expect(() => {
      calculator.dcf_calc(120000000, 0.05, 0.09, 0.02, validFinancialData);
    }).toThrow("発行済株式総数が不正です。");
  });
});

describe("PERマルチプル法 (per_multiple_calc)", () => {
  let calculator;

  beforeEach(() => {
    calculator = new global.pv_calculator();
  });

  test("正常系: EPS 100円, 目標PER 15倍で理論株価が1,500円になること", () => {
    expect(calculator.per_multiple_calc(100, 15)).toBeCloseTo(1500, 5);
  });

  test("異常系: EPSが赤字のときにエラーを投げること", () => {
    expect(() => calculator.per_multiple_calc(-10, 15)).toThrow(
      "EPSが赤字のため、PERマルチプル法は適用できません。",
    );
  });

  test("異常系: 目標PER倍率が0以下のときにエラーを投げること", () => {
    expect(() => calculator.per_multiple_calc(100, 0)).toThrow(
      "目標PER倍率は0より大きい値を指定してください。",
    );
  });
});

describe("配当割引モデル (ddm_calc)", () => {
  let calculator;

  beforeEach(() => {
    calculator = new global.pv_calculator();
  });

  test("正常系: 1株配当30円, 株主資本コスト8%, 配当成長率2%で理論株価が510円になること", () => {
    // 来期予想配当 = 30 * 1.02 = 30.6 / (0.08 - 0.02) = 510
    expect(calculator.ddm_calc(30, 0.08, 0.02)).toBeCloseTo(510, 5);
  });

  test("異常系: 株主資本コストが配当成長率以下のときにエラーを投げること", () => {
    expect(() => calculator.ddm_calc(30, 0.02, 0.02)).toThrow(
      "株主資本コストは永久配当成長率よりも大きくする必要があります。",
    );
  });

  test("異常系: 1株配当が0以下のときにエラーを投げること", () => {
    expect(() => calculator.ddm_calc(0, 0.08, 0.02)).toThrow(
      "1株配当が0以下のため、配当割引モデルは適用できません。",
    );
  });
});

describe("EV/EBITDA倍率法 (ev_ebitda_calc)", () => {
  let calculator;

  beforeEach(() => {
    calculator = new global.pv_calculator();
  });

  test("正常系: EBITDA 10億円, 目標倍率8倍, 純有利子負債2億円, 発行株式1000万株で理論株価が780円になること", () => {
    const fairPrice = calculator.ev_ebitda_calc(
      1000000000,
      8,
      200000000,
      10000000,
    );
    expect(fairPrice).toBeCloseTo(780, 5);
  });

  test("異常系: EBITDAが0以下のときにエラーを投げること", () => {
    expect(() => calculator.ev_ebitda_calc(0, 8, 200000000, 10000000)).toThrow(
      "EBITDAが取得できないか0以下のため、EV/EBITDA倍率法は適用できません。",
    );
  });
});

describe("PBRマルチプル法 (pbr_multiple_calc)", () => {
  let calculator;

  beforeEach(() => {
    calculator = new global.pv_calculator();
  });

  test("正常系: BPS 800円, 目標PBR 1.2倍で理論株価が960円になること", () => {
    expect(calculator.pbr_multiple_calc(800, 1.2)).toBeCloseTo(960, 5);
  });

  test("異常系: BPSが取得できないときにエラーを投げること", () => {
    expect(() => calculator.pbr_multiple_calc(null, 1.2)).toThrow(
      "BPSが取得できないため算出できません。",
    );
  });
});

describe("PSRマルチプル法 (psr_multiple_calc)", () => {
  let calculator;

  beforeEach(() => {
    calculator = new global.pv_calculator();
  });

  test("正常系: 売上高50億円, 目標PSR 1.5倍, 発行株式1000万株で理論株価が750円になること", () => {
    const fairPrice = calculator.psr_multiple_calc(
      5000000000,
      1.5,
      10000000,
    );
    expect(fairPrice).toBeCloseTo(750, 5);
  });

  test("異常系: 売上高が0以下のときにエラーを投げること", () => {
    expect(() => calculator.psr_multiple_calc(0, 1.5, 10000000)).toThrow(
      "売上高が取得できないため算出できません。",
    );
  });
});

describe("残余利益モデル (rim_calc)", () => {
  let calculator;

  beforeEach(() => {
    calculator = new global.pv_calculator();
  });

  test("正常系: BPS 800円, 株主資本コスト8%, 予想ROE 12%, 残余利益持続率60%で理論株価が840円になること", () => {
    // denominator = 1 + 0.08 - 0.6 = 0.48
    // residual_income = 800 * (0.12 - 0.08) = 32
    // fair_price = 800 + (0.6 / 0.48) * 32 = 840
    const fairPrice = calculator.rim_calc(800, 0.08, 0.12, 0.6);
    expect(fairPrice).toBeCloseTo(840, 5);
  });

  test("異常系: BPSが取得できないときにエラーを投げること", () => {
    expect(() => calculator.rim_calc(0, 0.08, 0.12, 0.6)).toThrow(
      "BPSが取得できないため算出できません。",
    );
  });
});

describe("修正純資産法 (adjusted_book_value_calc)", () => {
  let calculator;

  beforeEach(() => {
    calculator = new global.pv_calculator();
  });

  test("正常系: 総資産100億円, 総負債55億円, 資産時価調整率10%, 清算コスト率5%, 発行株式1000万株で理論株価が522.5円になること", () => {
    // revalued_assets = 100億 * 1.1 = 110億
    // net_asset_value = (110億 - 55億) * 0.95 = 52.25億
    // fair_price = 52.25億 / 1000万株 = 522.5円
    const fairPrice = calculator.adjusted_book_value_calc(
      10000000000,
      5500000000,
      0.1,
      0.05,
      10000000,
    );
    expect(fairPrice).toBeCloseTo(522.5, 5);
  });

  test("異常系: 清算コスト率が範囲外(150%)のときにエラーを投げること", () => {
    expect(() =>
      calculator.adjusted_book_value_calc(
        10000000000,
        5500000000,
        0.1,
        1.5,
        10000000,
      ),
    ).toThrow("清算コスト率は0%〜100%の範囲で指定してください。");
  });
});

describe("リアル・オプション分析 (real_option_calc)", () => {
  let calculator;

  beforeEach(() => {
    calculator = new global.pv_calculator();
  });

  test("正常系: 負債が0のとき、株主価値は資産価値と一致すること", () => {
    const fairPrice = calculator.real_option_calc(
      10000000000,
      0,
      0.3,
      0.01,
      5,
      10000000,
    );
    expect(fairPrice).toBeCloseTo(1000, 5);
  });

  test("正常系: 負債があるとき、理論株価は負債0のときより小さく、かつ正の値になること", () => {
    const withDebt = calculator.real_option_calc(
      200000000,
      100000000,
      0.4,
      0.03,
      2,
      1000000,
    );
    const withoutDebt = calculator.real_option_calc(
      200000000,
      0,
      0.4,
      0.03,
      2,
      1000000,
    );
    expect(withDebt).toBeGreaterThan(0);
    expect(withDebt).toBeLessThan(withoutDebt);
  });

  test("異常系: 資産ボラティリティが0以下のときにエラーを投げること", () => {
    expect(() =>
      calculator.real_option_calc(10000000000, 100000000, 0, 0.01, 5, 10000000),
    ).toThrow("資産ボラティリティは0より大きい値を指定してください。");
  });
});

describe("モデル横断の共通入口 (calculate)", () => {
  let calculator;
  let financialData;

  beforeEach(() => {
    calculator = new global.pv_calculator();
    financialData = {
      fcf: 120000000,
      sharesOutstanding: 10000000,
      cashAndEquivalents: 500000000,
      interestBearingDebt: 350000000,
      eps: 100,
      bps: 800,
      dividendPerShare: 30,
      revenue: 5000000000,
      ebitda: 1000000000,
      totalAssets: 10000000000,
      totalEquity: 4500000000,
    };
  });

  test("DCF: パーセント表記のパラメータを小数に変換して dcf_calc を呼び出すこと", () => {
    const params = { growth_in_5_years: 5, wacc: 9, terminal_growth: 2 };
    const viaDispatcher = calculator.calculate("DCF", params, financialData);
    const viaDirectCall = calculator.dcf_calc(120000000, 0.05, 0.09, 0.02, {
      non_operating_assets: { cash_and_equivalents: 500000000 },
      interest_bearing_debt: { total: 350000000 },
      shares_outstanding: 10000000,
    });
    expect(viaDispatcher).toBeCloseTo(viaDirectCall, 5);
  });

  test("PER_MULTIPLE: パラメータと財務データから理論株価を算出すること", () => {
    const fairPrice = calculator.calculate(
      "PER_MULTIPLE",
      { target_per: 15 },
      financialData,
    );
    expect(fairPrice).toBeCloseTo(1500, 5);
  });

  test("異常系: 必要な財務データが不足しているときにエラーを投げること", () => {
    const incompleteData = { ...financialData, ebitda: null };
    expect(() =>
      calculator.calculate(
        "EV_EBITDA",
        { target_ev_ebitda: 8 },
        incompleteData,
      ),
    ).toThrow(
      "EV/EBITDA倍率法の算出に必要なデータが不足しているため計算できません。",
    );
  });

  test("異常系: 未対応のモデルコードが指定されたときにエラーを投げること", () => {
    expect(() =>
      calculator.calculate("UNKNOWN_MODEL", {}, financialData),
    ).toThrow("未対応の評価モデルです: UNKNOWN_MODEL");
  });
});
