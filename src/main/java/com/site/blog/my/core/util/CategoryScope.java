package com.site.blog.my.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 分类作用域常量（单一来源）。
 * <p>
 * 站点按用途分成两个互不出圈的域：
 * <ul>
 *   <li>技术笔记（/notes）：列表与标签都必须排除生活类分类</li>
 *   <li>生活杂记（/life）：只展示生活类分类</li>
 * </ul>
 * 原先这份名单散在 BlogServiceImpl 内部常量与 Life.vue 里，标签查询没有拿到它，
 * 导致「读书心得 / 做菜笔记」下的标签漏进技术笔记侧边栏。统一收敛到这里。
 */
public final class CategoryScope {

    /**
     * 生活类分类名（技术笔记列表/标签需排除；与 Life.vue 的列配置保持一致）
     * <p>
     * ⚠️ 必须用 {@code new ArrayList<>(...)} 这种「public 具体集合类」，不要换成
     * {@code Collections.unmodifiableList(...)}、{@code Arrays.asList(...)} 或 {@code List.of(...)}。
     * <p>
     * 原因：这份名单会作为 {@code excludeCategoryNames} 传给 MyBatis，mapper XML 里用
     * {@code <if test="excludeCategoryNames != null and excludeCategoryNames.size() > 0">} 判断，
     * 而 MyBatis 的 OGNL 对「非 public 类」的方法要先 setAccessible 才能反射调用。
     * 上面那三种写法返回的实现类（Collections$UnmodifiableRandomAccessList / Arrays$ArrayList /
     * ImmutableCollections$List12）都是包级私有类，在 JDK 17+ 的模块强封装下会抛
     * InaccessibleObjectException（JDK 8 无模块系统，所以线上容器不复现，只有本地 JDK 23 会炸）。
     * <p>
     * 这类名单是全站唯一来源，各处以只读方式使用，不在此类外修改。
     */
    public static final List<String> LIFE_CATEGORY_NAMES =
            new ArrayList<>(Arrays.asList("读书心得", "做菜笔记"));

    private CategoryScope() {
    }
}
