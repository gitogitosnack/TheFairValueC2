package org.example.web.dao;

import org.example.web.entity.CompanyEntity;
import org.seasar.doma.*;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

import java.util.List;
import java.util.Optional;

// @Dao:このインターフェースが DomaのDAOであることを示す最も重要なアノテーション。
// Domaはコンパイル時にこのアノテーション@Daoを検出し、データベース処理を具体的に記述した実装クラス（通常はStockListDaoImpl）を自動生成します。
@Dao
// このDAOをSpring BootなどのDIコンテナで管理可能にするためのアノテーション。
// このアノテーションを付与することによって、サービス層など他の階層から@Autowiredを使って、このDAOのDIを可能にする。
@ConfigAutowireable
public interface CompanyDao {

    @Select
    List<CompanyEntity> selectAll();

    @Select
    Optional<CompanyEntity> selectById(int id);

    /**
     * delete_flg = 0 の企業を全件取得します（銘柄データ定期取得バッチ・日次株価同期用）。
     */
    @Select
    List<CompanyEntity> selectAllActive();

    /**
     * 指定した国コード（countries.code、例 "US" / "JP"）に属する delete_flg = 0 の企業を取得します
     * （銘柄データ定期取得バッチ・財務諸表同期の市場別絞り込み用）。
     */
    @Select
    List<CompanyEntity> selectActiveByCountryCode(String countryCode);

    @Insert
    int insert(CompanyEntity code);

    @Update
    int update(CompanyEntity code);

    @Delete
    int delete(CompanyEntity code);

}
