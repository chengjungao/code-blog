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

    /** 按日期范围查询 TOP 页面排行 */
    List<Map<String, Object>> selectTopPages(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    /** 按日期范围查询 PV/UV 总量 */
    Map<String, Object> selectTotalSummary(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
