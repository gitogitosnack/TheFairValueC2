package org.example.web.dao;

import org.example.web.entity.DailyQuoteEntity;
import org.seasar.doma.*;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Dao
@ConfigAutowireable
public interface DailyQuoteDao {
    @Select
    List<DailyQuoteEntity> selectAll();

    @Select
    Optional<DailyQuoteEntity> selectById(Long id);

    /**
     * 指定企業の最新日付の日次株価（現在株価として扱う）を1件取得します。
     *
     * @param companyId 企業ID
     * @return 最新日付の日次株価。存在しない場合は empty
     */
    @Select
    Optional<DailyQuoteEntity> selectLatestByCompanyId(Integer companyId);

    /**
     * (company_id, date) の UNIQUE 制約に対応する 1 件を取得します。
     * 存在すれば UPDATE、存在しなければ INSERT する UPSERT ロジックに使う
     * （DailyQuoteItemWriter、設計書 4.2 参照）。
     */
    @Select
    Optional<DailyQuoteEntity> selectByCompanyIdAndDate(Integer companyId, LocalDate date);

    @Insert
    int insert(DailyQuoteEntity entity);

    @Update
    int update(DailyQuoteEntity entity);

    @Delete
    int delete(DailyQuoteEntity entity);
}