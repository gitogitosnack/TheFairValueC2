package org.example.web.batch.marketdata.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// BatchExecutionLockService の実装クラス。PostgreSQL のアドバイザリロック
// （pg_try_advisory_lock / pg_advisory_unlock）でプロセスをまたいだ排他制御を行う（設計書 3.4 参照）。
//
// 注意: PostgreSQL のセッションレベル・アドバイザリロックは「取得したコネクション」上でのみ有効であり、
// 解放も同じコネクション上で行う必要がある。呼び出しごとに別コネクションが払い出されると
// tryLock で取得したロックを unlock で解放できなくなるため、DataSource から直接コネクションを取得し、
// unlock されるまでこのサービスのインスタンス内で明示的に保持し続ける。
@Service
public class BatchExecutionLockServiceImpl implements BatchExecutionLockService {

    private static final Logger log = LoggerFactory.getLogger(BatchExecutionLockServiceImpl.class);

    private final DataSource dataSource;
    private final Map<BatchType, Connection> heldConnections = new ConcurrentHashMap<>();

    public BatchExecutionLockServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public synchronized boolean tryLock(BatchType batchType) {
        try {
            Connection connection = dataSource.getConnection();
            try (PreparedStatement ps = connection.prepareStatement("select pg_try_advisory_lock(?)")) {
                ps.setInt(1, batchType.getLockKey());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    boolean acquired = rs.getBoolean(1);
                    if (acquired) {
                        heldConnections.put(batchType, connection);
                    } else {
                        connection.close();
                        log.info("[{}] 他経路で実行中のためロックを取得できませんでした", batchType);
                    }
                    return acquired;
                }
            }
        } catch (SQLException e) {
            log.error("[{}] アドバイザリロックの取得に失敗しました", batchType, e);
            return false;
        }
    }

    @Override
    public synchronized void unlock(BatchType batchType) {
        Connection connection = heldConnections.remove(batchType);
        if (connection == null) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement("select pg_advisory_unlock(?)")) {
            ps.setInt(1, batchType.getLockKey());
            ps.execute();
        } catch (SQLException e) {
            log.error("[{}] アドバイザリロックの解放に失敗しました", batchType, e);
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("[{}] ロック保持用コネクションのクローズに失敗しました", batchType, e);
            }
        }
    }
}
