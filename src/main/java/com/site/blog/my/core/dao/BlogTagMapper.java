package com.site.blog.my.core.dao;

import com.site.blog.my.core.entity.BlogTag;
import com.site.blog.my.core.entity.BlogTagCount;
import com.site.blog.my.core.util.PageQueryUtil;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BlogTagMapper {
    int deleteByPrimaryKey(Integer tagId);

    int insert(BlogTag record);

    int insertSelective(BlogTag record);

    BlogTag selectByPrimaryKey(Integer tagId);

    BlogTag selectByTagName(String tagName);

    BlogTag selectByTagNameIncludeDeleted(String tagName);

    int updateByPrimaryKeySelective(BlogTag record);

    int updateByPrimaryKey(BlogTag record);

    List<BlogTag> findTagList(PageQueryUtil pageUtil);

    List<BlogTagCount> getTagCount();

    /**
     * 按分类作用域统计热门标签（列表页侧边栏用）
     * <p>
     * 与 {@link #getTagCount()} 并存：后者用于 llms.txt 这类需要全站标签的场合。
     *
     * @param categoryName         只看该分类；为空则按 excludeCategoryNames 排除
     * @param excludeCategoryNames 需要排除的分类名（技术笔记侧排除生活类），可为 null
     */
    List<BlogTagCount> getTagCountByScope(@Param("categoryName") String categoryName,
                                          @Param("excludeCategoryNames") List<String> excludeCategoryNames);

    /**
     * 反查标签归属的分类：取该标签下已发布文章数最多的那个分类
     * <p>
     * 用于标签页（/tag/:name）让筛选条能高亮对应类目，并给出同类目的兄弟标签，
     * 支撑「类目 → 标签」二级联动。标签可跨分类，取占比最高者作为展示上下文。
     *
     * @param tagName 标签名
     * @return 分类名；标签不存在或无有效文章时为 null（调用方回退为「全部」）
     */
    String getTopCategoryNameByTag(@Param("tagName") String tagName);

    int getTotalTags(PageQueryUtil pageUtil);

    int deleteBatch(Integer[] ids);

    int batchInsertBlogTag(List<BlogTag> tagList);
}