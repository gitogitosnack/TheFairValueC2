package org.example.web.dao;

import org.example.web.entity.FinancialStatementEntity;
import org.seasar.doma.*;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;
import java.util.List;
import java.util.Optional;

@Dao
@ConfigAutowireable
public interface FinancialStatementDao {
    @Select
    List<FinancialStatementEntity> selectAll();

    @Select
    Optional<FinancialStatementEntity> selectById(Long id);

    /**
     * 指定企業の最新の通期（Q4）決算データを 1 件取得します。
     *
     * @param companyId 企業ID
     * @return 最新通期の財務諸表。存在しない場合は empty
     */
    @Select
    Optional<FinancialStatementEntity> selectLatestAnnualByCompanyId(Integer companyId);

    /**
     * 指定企業の最新四半期（fiscal_year, fiscal_quarter の組で最大のもの）を1件取得します。
     * API 側の最新四半期と比較し、差分があるかどうかを判定するために使う
     * （UsFinancialStatementItemProcessor、設計書 4.3.1 参照）。
     */
    @Select
    Optional<FinancialStatementEntity> selectLatestByCompanyId(Integer companyId);

    /**
     * (company_id, fiscal_year, fiscal_quarter) の UNIQUE 制約に対応する 1 件を取得します。
     * 既に取り込み済みかどうかの判定・再実行時の UPSERT に使う（FinancialStatementItemWriter 参照）。
     */
    @Select
    Optional<FinancialStatementEntity> selectByCompanyIdAndFiscalPeriod(
            Integer companyId, Integer fiscalYear, String fiscalQuarter);

    @Insert
    int insert(FinancialStatementEntity entity);

    @Update
    int update(FinancialStatementEntity entity);

    @Delete
    int delete(FinancialStatementEntity entity);
}