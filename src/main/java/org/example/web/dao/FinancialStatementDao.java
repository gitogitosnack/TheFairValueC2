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

    @Insert
    int insert(FinancialStatementEntity entity);

    @Update
    int update(FinancialStatementEntity entity);

    @Delete
    int delete(FinancialStatementEntity entity);
}