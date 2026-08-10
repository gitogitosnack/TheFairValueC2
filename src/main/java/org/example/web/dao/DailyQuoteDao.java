package org.example.web.dao;

import org.example.web.entity.DailyQuoteEntity;
import org.seasar.doma.*;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;
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

    @Insert
    int insert(DailyQuoteEntity entity);

    @Update
    int update(DailyQuoteEntity entity);

    @Delete
    int delete(DailyQuoteEntity entity);
}