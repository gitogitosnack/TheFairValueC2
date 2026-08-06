// テスト実行時に logger 未定義エラーが出ないよう、簡易的なダミーを定義
const logger =
  typeof window !== "undefined" && window.logger
    ? window.logger
    : {
        info: (msg) => console.log(`[INFO] ${msg}`),
        error: (msg) => console.error(`[ERROR] ${msg}`),
      };

class pv_calculator {
  // DCF法を用いて適正現在株価を予測するロジック
  dcf_calc(
    fcf,
    growth_ratio,
    discount_ratio,
    perpetual_growth_ratio,
    financial_data,
  ) {
    logger.info("dcf_calc method started.");

    // 0. エラーハンドリング: 割引率が永続成長率以下の場合、ゴードン・モデルは成立しません
    if (discount_ratio <= perpetual_growth_ratio) {
      logger.error(
        "Discount ratio must be greater than perpetual growth ratio.",
      );
      throw new Error("割引率は永続成長率よりも大きくする必要があります。");
    }

    // 1. 1〜5年目の予測FCFとその事業価値の計算
    const term = 5;
    let discounted_years = [];
    let business_value = 0;
    let current_fcf;

    for (let i = 1; i <= term; i++) {
      current_fcf = fcf * Math.pow(1 + growth_ratio, i);
      let discounted_year = current_fcf / Math.pow(1 + discount_ratio, i);
      discounted_years.push(discounted_year);
    }

    for (const year of discounted_years) {
      business_value += year;
    }

    // 2. ターミナルバリュー（6年目以降の永続価値）の計算
    // 6年目のFCFを予測
    let year_6_fcf = current_fcf * (1 + perpetual_growth_ratio);
    // 5年時点でのターミナルバリューを算出（ゴードン・グロース・モデル）
    let terminal_value = year_6_fcf / (discount_ratio - perpetual_growth_ratio);
    // ターミナルバリューを事業価値に割引
    let discounted_terminal_value =
      terminal_value / Math.pow(1 + discount_ratio, term);

    // 3. 1〜5年目の事業価値とターミナルバリューの事業価値を合算
    business_value += discounted_terminal_value;

    // 非事業資産の集計
    const non_operating_assets = Object.values(
      financial_data.non_operating_assets,
    ).reduce((sum, value) => sum + value, 0);

    // 有利子負債の集計
    const interest_bearing_debt = Object.values(
      financial_data.interest_bearing_debt,
    ).reduce((sum, value) => sum + value, 0);

    // 株主価値の算出
    const equity_value =
      business_value + non_operating_assets - interest_bearing_debt;

    // 1株あたりの適正株価の算出
    const shares = financial_data.shares_outstanding;
    if (!shares || shares <= 0) {
      throw new Error("発行済株式総数が不正です。");
    }

    // 4. 現在の適正株価算出
    const fair_price = equity_value / shares;

    logger.info("dcf_calc method ended.");
    return fair_price;
  }

  // PERマルチプル法を用いて適正株価を算出するロジック
  // 理論株価 = 予想EPS × 目標PER倍率
  per_multiple_calc(eps, target_per) {
    logger.info("per_multiple_calc method started.");

    if (eps === null || eps === undefined) {
      throw new Error("EPSが取得できないため算出できません。");
    }
    if (eps <= 0) {
      throw new Error(
        "EPSが赤字のため、PERマルチプル法は適用できません。",
      );
    }
    if (!target_per || target_per <= 0) {
      throw new Error("目標PER倍率は0より大きい値を指定してください。");
    }

    const fair_price = eps * target_per;

    logger.info("per_multiple_calc method ended.");
    return fair_price;
  }

  // 配当割引モデル(DDM)を用いて適正株価を算出するロジック（ゴードン成長モデル）
  // 理論株価 = 来期予想1株配当 ÷ (株主資本コスト − 永久配当成長率)
  ddm_calc(dividend_per_share, cost_of_equity, dividend_growth) {
    logger.info("ddm_calc method started.");

    if (!dividend_per_share || dividend_per_share <= 0) {
      throw new Error(
        "1株配当が0以下のため、配当割引モデルは適用できません。",
      );
    }
    if (cost_of_equity <= dividend_growth) {
      throw new Error(
        "株主資本コストは永久配当成長率よりも大きくする必要があります。",
      );
    }

    const next_dividend = dividend_per_share * (1 + dividend_growth);
    const fair_price = next_dividend / (cost_of_equity - dividend_growth);

    logger.info("ddm_calc method ended.");
    return fair_price;
  }

  // EV/EBITDA倍率法を用いて適正株価を算出するロジック
  // 理論株価 = (EBITDA × 目標EV/EBITDA倍率 − 純有利子負債) ÷ 発行済株式数
  ev_ebitda_calc(
    ebitda,
    target_ev_ebitda,
    net_interest_bearing_debt,
    shares_outstanding,
  ) {
    logger.info("ev_ebitda_calc method started.");

    if (!ebitda || ebitda <= 0) {
      throw new Error(
        "EBITDAが取得できないか0以下のため、EV/EBITDA倍率法は適用できません。",
      );
    }
    if (!target_ev_ebitda || target_ev_ebitda <= 0) {
      throw new Error(
        "目標EV/EBITDA倍率は0より大きい値を指定してください。",
      );
    }
    if (!shares_outstanding || shares_outstanding <= 0) {
      throw new Error("発行済株式総数が不正です。");
    }

    const enterprise_value = ebitda * target_ev_ebitda;
    const equity_value = enterprise_value - net_interest_bearing_debt;
    const fair_price = equity_value / shares_outstanding;

    logger.info("ev_ebitda_calc method ended.");
    return fair_price;
  }

  // PBRマルチプル法を用いて適正株価を算出するロジック
  // 理論株価 = 1株当たり純資産（BPS） × 目標PBR倍率
  pbr_multiple_calc(bps, target_pbr) {
    logger.info("pbr_multiple_calc method started.");

    if (!bps || bps <= 0) {
      throw new Error("BPSが取得できないため算出できません。");
    }
    if (!target_pbr || target_pbr <= 0) {
      throw new Error("目標PBR倍率は0より大きい値を指定してください。");
    }

    const fair_price = bps * target_pbr;

    logger.info("pbr_multiple_calc method ended.");
    return fair_price;
  }

  // PSRマルチプル法を用いて適正株価を算出するロジック
  // 理論株価 = (年間売上高 × 目標PSR倍率) ÷ 発行済株式数
  psr_multiple_calc(revenue, target_psr, shares_outstanding) {
    logger.info("psr_multiple_calc method started.");

    if (!revenue || revenue <= 0) {
      throw new Error("売上高が取得できないため算出できません。");
    }
    if (!target_psr || target_psr <= 0) {
      throw new Error("目標PSR倍率は0より大きい値を指定してください。");
    }
    if (!shares_outstanding || shares_outstanding <= 0) {
      throw new Error("発行済株式総数が不正です。");
    }

    const fair_price = (revenue * target_psr) / shares_outstanding;

    logger.info("psr_multiple_calc method ended.");
    return fair_price;
  }

  // 残余利益モデル(RIM)を用いて適正株価を算出するロジック（Ohlsonモデルの1株当たり簡易版）
  // 理論株価 = BPS + (残余利益持続率 ÷ (1 + 株主資本コスト − 残余利益持続率)) × BPS × (予想ROE − 株主資本コスト)
  rim_calc(bps, cost_of_equity, roe_forecast, residual_persistence) {
    logger.info("rim_calc method started.");

    if (!bps || bps <= 0) {
      throw new Error("BPSが取得できないため算出できません。");
    }
    if (residual_persistence < 0) {
      throw new Error("残余利益持続率は0%以上で指定してください。");
    }

    const denominator = 1 + cost_of_equity - residual_persistence;
    if (denominator <= 0) {
      throw new Error(
        "株主資本コストと残余利益持続率の組み合わせが不正です。",
      );
    }

    const residual_income = bps * (roe_forecast - cost_of_equity);
    const fair_price =
      bps + (residual_persistence / denominator) * residual_income;

    logger.info("rim_calc method ended.");
    return fair_price;
  }

  // 修正純資産法を用いて適正株価を算出するロジック
  // 理論株価 = ((総資産 × (1 + 資産時価調整率) − 総負債) × (1 − 清算コスト率)) ÷ 発行済株式数
  adjusted_book_value_calc(
    total_assets,
    total_liabilities,
    asset_revaluation_rate,
    liquidation_cost_rate,
    shares_outstanding,
  ) {
    logger.info("adjusted_book_value_calc method started.");

    if (total_assets === null || total_assets === undefined) {
      throw new Error("総資産が取得できないため算出できません。");
    }
    if (total_liabilities === null || total_liabilities === undefined) {
      throw new Error("総負債が取得できないため算出できません。");
    }
    if (!shares_outstanding || shares_outstanding <= 0) {
      throw new Error("発行済株式総数が不正です。");
    }
    if (liquidation_cost_rate < 0 || liquidation_cost_rate > 1) {
      throw new Error(
        "清算コスト率は0%〜100%の範囲で指定してください。",
      );
    }

    const revalued_assets = total_assets * (1 + asset_revaluation_rate);
    const net_asset_value =
      (revalued_assets - total_liabilities) * (1 - liquidation_cost_rate);
    const fair_price = net_asset_value / shares_outstanding;

    logger.info("adjusted_book_value_calc method ended.");
    return fair_price;
  }

  // 標準正規分布の累積分布関数（誤差関数の近似式を利用）
  _standard_normal_cdf(x) {
    return (1 + this._erf(x / Math.sqrt(2))) / 2;
  }

  // 誤差関数の近似式 (Abramowitz and Stegun, 式7.1.26)
  _erf(x) {
    const sign = x < 0 ? -1 : 1;
    const abs_x = Math.abs(x);
    const a1 = 0.254829592;
    const a2 = -0.284496736;
    const a3 = 1.421413741;
    const a4 = -1.453152027;
    const a5 = 1.061405429;
    const p = 0.3275911;
    const t = 1 / (1 + p * abs_x);
    const y =
      1 -
      ((((a5 * t + a4) * t + a3) * t + a2) * t + a1) *
        t *
        Math.exp(-abs_x * abs_x);
    return sign * y;
  }

  // リアル・オプション分析（マートン・モデル）を用いて適正株価を算出するロジック
  // 株主価値 = V × N(d1) − D × e^(−rT) × N(d2)
  // 企業資産Vを原資産、有利子負債Dを行使価格とするコール・オプションとみなして評価する。
  real_option_calc(
    asset_value,
    debt_value,
    volatility,
    risk_free_rate,
    time_to_maturity,
    shares_outstanding,
  ) {
    logger.info("real_option_calc method started.");

    if (!asset_value || asset_value <= 0) {
      throw new Error("総資産が取得できないため算出できません。");
    }
    if (!shares_outstanding || shares_outstanding <= 0) {
      throw new Error("発行済株式総数が不正です。");
    }
    if (!volatility || volatility <= 0) {
      throw new Error(
        "資産ボラティリティは0より大きい値を指定してください。",
      );
    }
    if (!time_to_maturity || time_to_maturity <= 0) {
      throw new Error(
        "負債の平均残存年数は0より大きい値を指定してください。",
      );
    }

    const debt = debt_value || 0;
    let equity_value;

    if (debt <= 0) {
      // 負債が無ければコールオプションは常に行使されるため、株主価値は資産価値に一致する。
      equity_value = asset_value;
    } else {
      const sqrt_t = Math.sqrt(time_to_maturity);
      const d1 =
        (Math.log(asset_value / debt) +
          (risk_free_rate + 0.5 * volatility * volatility) *
            time_to_maturity) /
        (volatility * sqrt_t);
      const d2 = d1 - volatility * sqrt_t;
      equity_value =
        asset_value * this._standard_normal_cdf(d1) -
        debt *
          Math.exp(-risk_free_rate * time_to_maturity) *
          this._standard_normal_cdf(d2);
    }

    const fair_price = equity_value / shares_outstanding;

    logger.info("real_option_calc method ended.");
    return fair_price;
  }

  // パーセント表記(例: 6.0 = 6%)を計算用の小数(0.06)に変換する
  _pct(value) {
    const numeric = value === null || value === undefined ? 0 : value;
    return numeric / 100;
  }

  // 算出に必要な財務データ項目が揃っているかを検証する
  _requireFields(financial_data, fields, model_label) {
    const missing = fields.filter(
      (field) =>
        financial_data[field] === null || financial_data[field] === undefined,
    );
    if (missing.length > 0) {
      throw new Error(
        `${model_label}の算出に必要なデータが不足しているため計算できません。`,
      );
    }
  }

  // モデルコードに応じて適切な算出メソッドを呼び分ける共通の入口。
  // params: パラメータコード(例: "wacc")をキーとした画面表示単位（%は%のまま等）の値
  // financial_data: FinancialDataDto をそのままJSONにしたもの（キャメルケース）
  calculate(model_code, params, financial_data) {
    const p = params || {};
    const fd = financial_data || {};

    switch (model_code) {
      case "DCF": {
        this._requireFields(fd, ["fcf", "sharesOutstanding"], "DCF法");
        const wrapped_financial_data = {
          non_operating_assets: {
            cash_and_equivalents: fd.cashAndEquivalents || 0,
          },
          interest_bearing_debt: {
            total: fd.interestBearingDebt || 0,
          },
          shares_outstanding: fd.sharesOutstanding,
        };
        return this.dcf_calc(
          fd.fcf,
          this._pct(p.growth_in_5_years),
          this._pct(p.wacc),
          this._pct(p.terminal_growth),
          wrapped_financial_data,
        );
      }
      case "PER_MULTIPLE":
        this._requireFields(fd, ["eps"], "PERマルチプル法");
        return this.per_multiple_calc(fd.eps, p.target_per);
      case "DDM":
        this._requireFields(fd, ["dividendPerShare"], "配当割引モデル(DDM)");
        return this.ddm_calc(
          fd.dividendPerShare,
          this._pct(p.cost_of_equity),
          this._pct(p.dividend_growth),
        );
      case "EV_EBITDA": {
        this._requireFields(
          fd,
          ["ebitda", "sharesOutstanding"],
          "EV/EBITDA倍率法",
        );
        const net_debt =
          (fd.interestBearingDebt || 0) - (fd.cashAndEquivalents || 0);
        return this.ev_ebitda_calc(
          fd.ebitda,
          p.target_ev_ebitda,
          net_debt,
          fd.sharesOutstanding,
        );
      }
      case "PBR_MULTIPLE":
        this._requireFields(fd, ["bps"], "PBRマルチプル法");
        return this.pbr_multiple_calc(fd.bps, p.target_pbr);
      case "PSR_MULTIPLE":
        this._requireFields(
          fd,
          ["revenue", "sharesOutstanding"],
          "PSRマルチプル法",
        );
        return this.psr_multiple_calc(
          fd.revenue,
          p.target_psr,
          fd.sharesOutstanding,
        );
      case "RIM":
        this._requireFields(fd, ["bps"], "残余利益モデル(RIM)");
        return this.rim_calc(
          fd.bps,
          this._pct(p.cost_of_equity),
          this._pct(p.roe_forecast),
          this._pct(p.residual_persistence),
        );
      case "ADJUSTED_BOOK_VALUE": {
        this._requireFields(
          fd,
          ["totalAssets", "totalEquity", "sharesOutstanding"],
          "修正純資産法",
        );
        const total_liabilities = fd.totalAssets - fd.totalEquity;
        return this.adjusted_book_value_calc(
          fd.totalAssets,
          total_liabilities,
          this._pct(p.asset_revaluation_rate),
          this._pct(p.liquidation_cost_rate),
          fd.sharesOutstanding,
        );
      }
      case "REAL_OPTION":
        this._requireFields(
          fd,
          ["totalAssets", "sharesOutstanding"],
          "リアル・オプション分析",
        );
        return this.real_option_calc(
          fd.totalAssets,
          fd.interestBearingDebt || 0,
          this._pct(p.asset_volatility),
          this._pct(p.risk_free_rate),
          p.debt_maturity,
          fd.sharesOutstanding,
        );
      default:
        throw new Error(`未対応の評価モデルです: ${model_code}`);
    }
  }
}

// ブラウザ環境（window）を壊さず、Node.js環境（Vitest）の時だけグローバルに展開する
if (typeof global !== "undefined") {
  global.pv_calculator = pv_calculator;
}
