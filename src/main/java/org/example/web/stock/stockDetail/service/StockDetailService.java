package org.example.web.stock.stockDetail.service;

import org.example.web.stock.stockDetail.domain.ParameterDefaultUpdateForm;
import org.example.web.stock.stockDetail.domain.StockAnalysisResponse;

public interface StockDetailService {

    public StockAnalysisResponse getComprehensiveAnalysis(String code);

    /**
     * 画面で操作したスライダーの値を、その銘柄のパラメータ初期値としてDBに保存する。
     *
     * @param code 証券コード
     * @param form 保存対象のパラメータID・値のリスト
     */
    public void updateCompanyParameterDefaults(String code, ParameterDefaultUpdateForm form);

}
