package com.site.blog.my.core.service;

import com.site.blog.my.core.entity.BlogTagCount;
import com.site.blog.my.core.util.PageQueryUtil;
import com.site.blog.my.core.util.PageResult;

import java.util.List;

public interface TagService {

    /**
     * 查询标签的分页数据
     *
     * @param pageUtil
     * @return
     */
    PageResult getBlogTagPage(PageQueryUtil pageUtil);

    int getTotalTags();

    Boolean saveTag(String tagName);

    Boolean deleteBatch(Integer[] ids);

    List<BlogTagCount> getBlogTagCountForIndex();

    /**
     * 按分类作用域取热门标签（列表页侧边栏用）
     *
     * @param categoryName         只看该分类；为空则退化为「全站 +/- 排除」
     * @param excludeCategoryNames 需排除的分类名（技术笔记侧传生活类分类），可为空
     */
    List<BlogTagCount> getBlogTagCountForScope(String categoryName, List<String> excludeCategoryNames);

    /**
     * 反查标签归属的分类（该标签下已发布文章数最多的分类）
     * <p>
     * 供标签页让筛选条高亮对应类目使用。
     *
     * @param tagName 标签名
     * @return 分类名；标签不存在或无有效文章时返回 null
     */
    String getTopCategoryNameByTag(String tagName);
}
