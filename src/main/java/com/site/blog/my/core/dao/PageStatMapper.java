package com.site.blog.my.core.dao;

import com.site.blog.my.core.entity.PageStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface PageStatMapper {

    /** PV 累加写入（ON DUPLICATE KEY UPDATE pv = pv + #{pv}） */
    int upsertPv(PageStat record);

    /** UV 覆盖写入（ON DUPLICATE KEY UPDATE uv = #{uv}） */
    int upsertUv(PageStat record);

    /** 按日期范围查询每日汇总（PV/UV） */
    List<Map<String, Object>> selectDailySummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 按日期范围查询每日汇总，并剔除「每日预渲染批次」。
     *
     * <p>1.4.9 之前，每天 02:00 的全量预渲染会在同一秒里把全站每个页面各写 1 次 PV
     * （实测每天 36~45 行、占当日 PV 的 85%~95%）。这批历史数据无法追溯，只能在读取时
     * 按「同一 create_time 秒内出现 ≥ batchRows 行」识别出来，每行扣掉 1 次 PV，
     * 让「DB 兜底的历史日期」与「明细口径的新日期」量级可比。
     *
     * @param batchRows 判定为批量插入的最小行数（真人不会在同一秒打开这么多页面）
     */
    List<Map<String, Object>> selectDailySummaryClean(@Param("startDate") String startDate,
                                                     @Param("endDate") String endDate,
                                                     @Param("batchRows") int batchRows);

    /** 按日期范围查询 TOP 页面排行 */
    List<Map<String, Object>> selectTopPages(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    /** 按日期范围查询 PV/UV 总量 */
    Map<String, Object> selectTotalSummary(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
