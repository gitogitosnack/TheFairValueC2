package org.example.web.stock.stockDetail.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 銘柄詳細画面のスライダー値を、その銘柄のパラメータ初期値としてDBに保存するためのフォーム。
 */
public record ParameterDefaultUpdateForm(List<ParameterDefaultItem> parameters) {

    public record ParameterDefaultItem(Integer parameterId, BigDecimal value) {
    }
}
