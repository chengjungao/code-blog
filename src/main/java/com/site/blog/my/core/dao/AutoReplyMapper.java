package com.site.blog.my.core.dao;

import com.site.blog.my.core.entity.AutoReply;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AutoReplyMapper {
    int insertSelective(AutoReply record);

    int updateByPrimaryKeySelective(AutoReply record);

    int deleteBatch(@Param("ids") Integer[] ids);

    AutoReply selectByPrimaryKey(Long id);

    /** 关键词精确查询（keyword 唯一，用于新增/修改查重） */
    AutoReply selectByKeyword(@Param("keyword") String keyword);

    /** 管理端全量列表（含停用，按 id 升序） */
    List<AutoReply> selectAllList();

    /** 启用中的规则（微信匹配用，按 id 升序，先配置优先） */
    List<AutoReply> selectEnabledList();
}
