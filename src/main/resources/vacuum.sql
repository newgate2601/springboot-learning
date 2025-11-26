-- phân tích workload của hệ thống - phân loại bảng theo kích thước và update pattern
-- (xem logic tại src/main/resources/image/vacuum-strategy)
WITH table_stats AS (
    SELECT
        schemaname,
        relname,
        n_live_tup as live_rows,
        n_dead_tup as dead_rows,
        n_tup_ins + n_tup_upd + n_tup_del as write_activity,
        round(n_dead_tup::numeric / greatest(n_live_tup, 1) * 100, 2) as dead_ratio,
        pg_size_pretty(pg_total_relation_size(relid)) as size,
        age(relfrozenxid) as freeze_age,
        last_autovacuum,
        last_autoanalyze,
        autovacuum_count,
        autoanalyze_count
    FROM pg_stat_all_tables
    WHERE schemaname NOT LIKE 'pg_%'
      AND relkind = 'r'
)
SELECT *,
    CASE
        WHEN live_rows > 1000000 AND write_activity > 1000 THEN 'LARGE_HIGH_UPDATE'
        WHEN live_rows > 1000000 AND write_activity <= 1000 THEN 'LARGE_LOW_UPDATE'
        WHEN live_rows BETWEEN 100000 AND 1000000 AND write_activity > 500 THEN 'MEDIUM_HIGH_UPDATE'
        WHEN live_rows BETWEEN 100000 AND 1000000 AND write_activity <= 500 THEN 'MEDIUM_LOW_UPDATE'
        WHEN live_rows < 100000 AND write_activity > 100 THEN 'SMALL_HIGH_UPDATE'
        ELSE 'SMALL_LOW_UPDATE'
    END as table_category
FROM table_stats
ORDER BY live_rows DESC, write_activity DESC;



