package org.example.web.stock.stockDetail.domain;

import java.util.List;

public class FinancialIndicatorDto {
    // 画面の下半分の5年分の分析指標情報を取得するDao
    // 年度ラベル
    private String tableTitle;
    private List<String> fiscalYearLabels;

    // 1st tab
    private String roeLabel;
    private List<Double> roeList;
    private String grossMarginLabel;
    private List<Double> grossMarginList;
    private String sgaRatioLabel;
    private List<Double> sgaRatioList;
    private String netMarginLabel;
    private List<Double> netMarginList;
    private String roaLabel;
    private List<Double> roaList;
    private String epsLabel;
    private List<Double> epsList;

    // 2nd tab
    private String assetTurnoverLabel;
    private List<Double> assetTurnoverList;
    private String inventoryTurnoverLabel;
    private List<Double> inventoryTurnoverList;
    private String receivablesTurnoverLabel;
    private List<Double> receivablesTurnoverList;

    // 3rd tab
    private String equityRatioLabel;
    private List<Double> equityRatioList;
    private String debtEquityRatioLabel;
    private List<Double> debtEquityRatioList;
    private String debtRatioLabel;
    private List<Double> debtRatioList;
    private String interestCoverageRatioLabel;
    private List<Double> interestCoverageRatioList;

    // 4th tab
    private String operationCfMarginLabel;
    private List<Double> operationCfMarginList;
    private String fcfLabel;
    private List<Double> fcfList;
    private String finCfLabel;
    private List<Double> finCfList;

    public String getTableTitle() {
        return tableTitle;
    }

    public void setTableTitle(String tableTitle) {
        this.tableTitle = tableTitle;
    }

    public List<String> getFiscalYearLabels() {
        return fiscalYearLabels;
    }

    public void setFiscalYearLabels(List<String> fiscalYearLabels) {
        this.fiscalYearLabels = fiscalYearLabels;
    }

    public String getRoeLabel() {
        return roeLabel;
    }

    public void setRoeLabel(String roeLabel) {
        this.roeLabel = roeLabel;
    }

    public List<Double> getRoeList() {
        return roeList;
    }

    public void setRoeList(List<Double> roeList) {
        this.roeList = roeList;
    }

    public String getGrossMarginLabel() {
        return grossMarginLabel;
    }

    public void setGrossMarginLabel(String grossMarginLabel) {
        this.grossMarginLabel = grossMarginLabel;
    }

    public List<Double> getGrossMarginList() {
        return grossMarginList;
    }

    public void setGrossMarginList(List<Double> grossMarginList) {
        this.grossMarginList = grossMarginList;
    }

    public String getSgaRatioLabel() {
        return sgaRatioLabel;
    }

    public void setSgaRatioLabel(String sgaRatioLabel) {
        this.sgaRatioLabel = sgaRatioLabel;
    }

    public List<Double> getSgaRatioList() {
        return sgaRatioList;
    }

    public void setSgaRatioList(List<Double> sgaRatioList) {
        this.sgaRatioList = sgaRatioList;
    }

    public String getNetMarginLabel() {
        return netMarginLabel;
    }

    public void setNetMarginLabel(String netMarginLabel) {
        this.netMarginLabel = netMarginLabel;
    }

    public List<Double> getNetMarginList() {
        return netMarginList;
    }

    public void setNetMarginList(List<Double> netMarginList) {
        this.netMarginList = netMarginList;
    }

    public String getRoaLabel() {
        return roaLabel;
    }

    public void setRoaLabel(String roaLabel) {
        this.roaLabel = roaLabel;
    }

    public List<Double> getRoaList() {
        return roaList;
    }

    public void setRoaList(List<Double> roaList) {
        this.roaList = roaList;
    }

    public String getEpsLabel() {
        return epsLabel;
    }

    public void setEpsLabel(String epsLabel) {
        this.epsLabel = epsLabel;
    }

    public List<Double> getEpsList() {
        return epsList;
    }

    public void setEpsList(List<Double> epsList) {
        this.epsList = epsList;
    }

    public String getAssetTurnoverLabel() {
        return assetTurnoverLabel;
    }

    public void setAssetTurnoverLabel(String assetTurnoverLabel) {
        this.assetTurnoverLabel = assetTurnoverLabel;
    }

    public List<Double> getAssetTurnoverList() {
        return assetTurnoverList;
    }

    public void setAssetTurnoverList(List<Double> assetTurnoverList) {
        this.assetTurnoverList = assetTurnoverList;
    }

    public String getInventoryTurnoverLabel() {
        return inventoryTurnoverLabel;
    }

    public void setInventoryTurnoverLabel(String inventoryTurnoverLabel) {
        this.inventoryTurnoverLabel = inventoryTurnoverLabel;
    }

    public List<Double> getInventoryTurnoverList() {
        return inventoryTurnoverList;
    }

    public void setInventoryTurnoverList(List<Double> inventoryTurnoverList) {
        this.inventoryTurnoverList = inventoryTurnoverList;
    }

    public String getReceivablesTurnoverLabel() {
        return receivablesTurnoverLabel;
    }

    public void setReceivablesTurnoverLabel(String receivablesTurnoverLabel) {
        this.receivablesTurnoverLabel = receivablesTurnoverLabel;
    }

    public List<Double> getReceivablesTurnoverList() {
        return receivablesTurnoverList;
    }

    public void setReceivablesTurnoverList(List<Double> receivablesTurnoverList) {
        this.receivablesTurnoverList = receivablesTurnoverList;
    }

    public String getEquityRatioLabel() {
        return equityRatioLabel;
    }

    public void setEquityRatioLabel(String equityRatioLabel) {
        this.equityRatioLabel = equityRatioLabel;
    }

    public List<Double> getEquityRatioList() {
        return equityRatioList;
    }

    public void setEquityRatioList(List<Double> equityRatioList) {
        this.equityRatioList = equityRatioList;
    }

    public String getDebtEquityRatioLabel() {
        return debtEquityRatioLabel;
    }

    public void setDebtEquityRatioLabel(String debtEquityRatioLabel) {
        this.debtEquityRatioLabel = debtEquityRatioLabel;
    }

    public List<Double> getDebtEquityRatioList() {
        return debtEquityRatioList;
    }

    public void setDebtEquityRatioList(List<Double> debtEquityRatioList) {
        this.debtEquityRatioList = debtEquityRatioList;
    }

    public String getDebtRatioLabel() {
        return debtRatioLabel;
    }

    public void setDebtRatioLabel(String debtRatioLabel) {
        this.debtRatioLabel = debtRatioLabel;
    }

    public List<Double> getDebtRatioList() {
        return debtRatioList;
    }

    public void setDebtRatioList(List<Double> debtRatioList) {
        this.debtRatioList = debtRatioList;
    }

    public String getInterestCoverageRatioLabel() {
        return interestCoverageRatioLabel;
    }

    public void setInterestCoverageRatioLabel(String interestCoverageRatioLabel) {
        this.interestCoverageRatioLabel = interestCoverageRatioLabel;
    }

    public List<Double> getInterestCoverageRatioList() {
        return interestCoverageRatioList;
    }

    public void setInterestCoverageRatioList(List<Double> interestCoverageRatioList) {
        this.interestCoverageRatioList = interestCoverageRatioList;
    }

    public String getFcfLabel() {
        return fcfLabel;
    }

    public void setFcfLabel(String fcfLabel) {
        this.fcfLabel = fcfLabel;
    }

    public List<Double> getFcfList() {
        return fcfList;
    }

    public void setFcfList(List<Double> fcfList) {
        this.fcfList = fcfList;
    }

    public String getOperationCfMarginLabel() {
        return operationCfMarginLabel;
    }

    public void setOperationCfMarginLabel(String operationCfMarginLabel) {
        this.operationCfMarginLabel = operationCfMarginLabel;
    }

    public List<Double> getOperationCfMarginList() {
        return operationCfMarginList;
    }

    public void setOperationCfMarginList(List<Double> operationCfMarginList) {
        this.operationCfMarginList = operationCfMarginList;
    }

    public String getFinCfLabel() {
        return finCfLabel;
    }

    public void setFinCfLabel(String finCfLabel) {
        this.finCfLabel = finCfLabel;
    }

    public List<Double> getFinCfList() {
        return finCfList;
    }

    public void setFinCfList(List<Double> finCfList) {
        this.finCfList = finCfList;
    }
}
