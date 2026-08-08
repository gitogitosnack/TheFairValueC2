package org.example.web.dao;

import org.example.web.entity.CompanyCompetitorEntity;
import org.seasar.doma.Dao;
import org.seasar.doma.Delete;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;

import java.util.List;
import java.util.Optional;

@Dao
@ConfigAutowireable
public interface CompanyCompetitorDao {

    @Select
    List<CompanyCompetitorEntity> selectByCompanyId(Integer companyId);

    @Select
    Optional<CompanyCompetitorEntity> selectByCompanyIdAndCompetitorCompanyId(Integer companyId,
            Integer competitorCompanyId);

    @Select
    Optional<CompanyCompetitorEntity> selectById(Integer id);

    @Insert
    int insert(CompanyCompetitorEntity entity);

    @Delete
    int delete(CompanyCompetitorEntity entity);
}
