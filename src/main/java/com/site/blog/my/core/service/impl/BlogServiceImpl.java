package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.controller.vo.BlogDetailVO;
import com.site.blog.my.core.controller.vo.BlogListVO;
import com.site.blog.my.core.controller.vo.CategoryCountVO;
import com.site.blog.my.core.controller.vo.SimpleBlogListVO;
import com.site.blog.my.core.dao.*;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.entity.BlogCategory;
import com.site.blog.my.core.entity.BlogChunk;
import com.site.blog.my.core.entity.BlogTag;
import com.site.blog.my.core.entity.BlogTagRelation;
import com.site.blog.my.core.service.BlogService;
import com.site.blog.my.core.service.BlogChunkService;
import com.site.blog.my.core.solr.BlogSolrServer;
import com.site.blog.my.core.util.CategoryScope;
import com.site.blog.my.core.util.PageQueryUtil;
import com.site.blog.my.core.util.PageResult;
import com.site.blog.my.core.util.PatternUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class BlogServiceImpl implements BlogService {

    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private BlogCategoryMapper categoryMapper;
    @Autowired
    private BlogTagMapper tagMapper;
    @Autowired
    private BlogTagRelationMapper blogTagRelationMapper;
    @Autowired
    private BlogCommentMapper blogCommentMapper;
    
    @Autowired
    private BlogSolrServer blogSolrServer;
    @Autowired
    private BlogChunkService blogChunkService;

    /** 列表页每页条数（前台 3 列栅格，9 条刚好 3 行） */
    private static final int LIST_PAGE_SIZE = 9;

    @Override
    @Transactional
    public String saveBlog(Blog blog) {
        BlogCategory blogCategory = categoryMapper.selectByPrimaryKey(blog.getBlogCategoryId());
        if (blogCategory == null) {
            blog.setBlogCategoryId(0);
            blog.setBlogCategoryName("默认分类");
        } else {
            //设置博客分类名称
            blog.setBlogCategoryName(blogCategory.getCategoryName());
            //分类的排序值加1
            blogCategory.setCategoryRank(blogCategory.getCategoryRank() + 1);
        }
        //处理标签数据
        String[] tags = blog.getBlogTags().split(",");
        if (tags.length > 6) {
            return "标签数量限制为6";
        }
        //保存文章
        if (blogMapper.insertSelective(blog) > 0) {
        	
        	// 搜索索引失败不回滚文章保存：Solr 只是加速器，索引会在应用启动后全量重建
        	try {
        		blogSolrServer.add(blog);
        	} catch (Exception e) {
        		System.err.println("[WARN] Blog indexing failed for blog " + blog.getBlogId() + ": " + e.getMessage());
        	}
        	// 同步分块索引（仅发布状态的文章）
        	if (blog.getBlogStatus() != null && blog.getBlogStatus() == 1) {
        		try {
        			List<BlogChunk> chunks = blogChunkService.splitBlog(blog);
        			if (!chunks.isEmpty()) {
        				blogSolrServer.addChunks(chunks);
        			}
        		} catch (Exception e) {
        			System.err.println("[WARN] Chunk indexing failed for new blog: " + e.getMessage());
        		}
        	}
            //新增的tag对象
            List<BlogTag> tagListForInsert = new ArrayList<>();
            //所有的tag对象，用于建立关系数据
            List<BlogTag> allTagsList = new ArrayList<>();
            for (int i = 0; i < tags.length; i++) {
                BlogTag tag = tagMapper.selectByTagNameIncludeDeleted(tags[i]);
                if (tag == null) {
                    //不存在就新增
                    BlogTag tempTag = new BlogTag();
                    tempTag.setTagName(tags[i]);
                    tagListForInsert.add(tempTag);
                } else {
                    //已软删的同名标签恢复后复用（tag_name唯一索引不允许重复插入）
                    if (tag.getIsDeleted() != null && tag.getIsDeleted() == 1) {
                        tag.setIsDeleted((byte) 0);
                        tagMapper.updateByPrimaryKeySelective(tag);
                    }
                    allTagsList.add(tag);
                }
            }
            //新增标签数据并修改分类排序值
            if (!CollectionUtils.isEmpty(tagListForInsert)) {
                tagMapper.batchInsertBlogTag(tagListForInsert);
            }
            if (blogCategory != null) {
                categoryMapper.updateByPrimaryKeySelective(blogCategory);
            }
            List<BlogTagRelation> blogTagRelations = new ArrayList<>();
            //新增关系数据
            allTagsList.addAll(tagListForInsert);
            for (BlogTag tag : allTagsList) {
                BlogTagRelation blogTagRelation = new BlogTagRelation();
                blogTagRelation.setBlogId(blog.getBlogId());
                blogTagRelation.setTagId(tag.getTagId());
                blogTagRelations.add(blogTagRelation);
            }
            
            if (blogTagRelationMapper.batchInsert(blogTagRelations) > 0) {
                return "success";
            }
        }
        return "保存失败";
    }

    @Override
    public PageResult getBlogsPage(PageQueryUtil pageUtil) {
        List<Blog> blogList = blogMapper.findBlogList(pageUtil);
        int total = blogMapper.getTotalBlogs(pageUtil);
        PageResult pageResult = new PageResult(blogList, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }

    @Override
    public Boolean deleteBatch(Integer[] ids) {
    	if (blogMapper.deleteBatch(ids) > 0) {
    		String[] blogIds = new String[ids.length]; 
    		for(int i = 0; i < ids.length ; i ++) {
    			blogIds[i] = String.valueOf(ids[i]);
    			// 同步删除分块索引
    			try {
    				blogSolrServer.deleteChunksByBlogId(Long.valueOf(ids[i]));
    			} catch (Exception e) {
    				System.err.println("[WARN] Chunk deletion failed for blog " + ids[i] + ": " + e.getMessage());
    			}
    		}
    		blogSolrServer.delete(blogIds);
    		return true;
		}
       return false;
    }

    @Override
    public int getTotalBlogs() {
        return blogMapper.getTotalBlogs(null);
    }

    @Override
    public Blog getBlogById(Long blogId) {
        return blogMapper.selectByPrimaryKey(blogId);
    }

    @Override
    @Transactional
    public String updateBlog(Blog blog) {
        Blog blogForUpdate = blogMapper.selectByPrimaryKey(blog.getBlogId());
        if (blogForUpdate == null) {
            return "数据不存在";
        }
        blogForUpdate.setBlogTitle(blog.getBlogTitle());
        blogForUpdate.setBlogSubUrl(blog.getBlogSubUrl());
        blogForUpdate.setBlogContent(blog.getBlogContent());
        blogForUpdate.setBlogCoverImage(blog.getBlogCoverImage());
        blogForUpdate.setBlogStatus(blog.getBlogStatus());
        blogForUpdate.setEnableComment(blog.getEnableComment());
        BlogCategory blogCategory = categoryMapper.selectByPrimaryKey(blog.getBlogCategoryId());
        if (blogCategory == null) {
            blogForUpdate.setBlogCategoryId(0);
            blogForUpdate.setBlogCategoryName("默认分类");
        } else {
            //设置博客分类名称
            blogForUpdate.setBlogCategoryName(blogCategory.getCategoryName());
            blogForUpdate.setBlogCategoryId(blogCategory.getCategoryId());
            //分类的排序值加1
            blogCategory.setCategoryRank(blogCategory.getCategoryRank() + 1);
        }
        //处理标签数据
        String[] tags = blog.getBlogTags().split(",");
        if (tags.length > 6) {
            return "标签数量限制为6";
        }
        blogForUpdate.setBlogTags(blog.getBlogTags());
        //新增的tag对象
        List<BlogTag> tagListForInsert = new ArrayList<>();
        //所有的tag对象，用于建立关系数据
        List<BlogTag> allTagsList = new ArrayList<>();
        for (int i = 0; i < tags.length; i++) {
            BlogTag tag = tagMapper.selectByTagNameIncludeDeleted(tags[i]);
            if (tag == null) {
                //不存在就新增
                BlogTag tempTag = new BlogTag();
                tempTag.setTagName(tags[i]);
                tagListForInsert.add(tempTag);
            } else {
                //已软删的同名标签恢复后复用（tag_name唯一索引不允许重复插入）
                if (tag.getIsDeleted() != null && tag.getIsDeleted() == 1) {
                    tag.setIsDeleted((byte) 0);
                    tagMapper.updateByPrimaryKeySelective(tag);
                }
                allTagsList.add(tag);
            }
        }
        //新增标签数据不为空->新增标签数据
        if (!CollectionUtils.isEmpty(tagListForInsert)) {
            tagMapper.batchInsertBlogTag(tagListForInsert);
        }
        List<BlogTagRelation> blogTagRelations = new ArrayList<>();
        //新增关系数据
        allTagsList.addAll(tagListForInsert);
        for (BlogTag tag : allTagsList) {
            BlogTagRelation blogTagRelation = new BlogTagRelation();
            blogTagRelation.setBlogId(blog.getBlogId());
            blogTagRelation.setTagId(tag.getTagId());
            blogTagRelations.add(blogTagRelation);
        }
        //修改blog信息->修改分类排序值->删除原关系数据->保存新的关系数据
        if (blogCategory != null) {
            categoryMapper.updateByPrimaryKeySelective(blogCategory);
        }
        blogTagRelationMapper.deleteByBlogId(blog.getBlogId());
        blogTagRelationMapper.batchInsert(blogTagRelations);
        if (blogMapper.updateByPrimaryKeySelective(blogForUpdate) > 0) {
        	// 同 saveBlog：索引失败只告警，不回滚文章修改
        	try {
        		blogSolrServer.add(blogForUpdate);
        	} catch (Exception e) {
        		System.err.println("[WARN] Blog re-indexing failed for blog " + blogForUpdate.getBlogId() + ": " + e.getMessage());
        	}
        	// 同步分块索引：先删旧分块，再根据发布状态决定是否重建
        	try {
        		blogSolrServer.deleteChunksByBlogId(blogForUpdate.getBlogId());
        		if (blogForUpdate.getBlogStatus() != null && blogForUpdate.getBlogStatus() == 1) {
        			List<BlogChunk> chunks = blogChunkService.splitBlog(blogForUpdate);
        			if (!chunks.isEmpty()) {
        				blogSolrServer.addChunks(chunks);
        			}
        		}
        	} catch (Exception e) {
        		System.err.println("[WARN] Chunk re-indexing failed for blog " + blogForUpdate.getBlogId() + ": " + e.getMessage());
        	}
            return "success";
        }
        return "修改失败";
    }

    @Override
    public PageResult getBlogsForIndexPage(int page) {
        Map params = new HashMap();
        params.put("page", page);
        params.put("limit", LIST_PAGE_SIZE);
        params.put("blogStatus", 1);//过滤发布状态下的数据
        params.put("excludeCategoryNames", CategoryScope.LIFE_CATEGORY_NAMES);//排除生活类分类
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        List<Blog> blogList = blogMapper.findBlogList(pageUtil);
        List<BlogListVO> blogListVOS = getBlogListVOsByBlogs(blogList);
        int total = blogMapper.getTotalBlogs(pageUtil);
        PageResult pageResult = new PageResult(blogListVOS, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }

    @Override
    public List<CategoryCountVO> getCategoryCountsForScope(List<String> excludeCategoryNames) {
        return blogMapper.getCategoryCounts(excludeCategoryNames);
    }

    @Override
    public List<SimpleBlogListVO> getBlogListForIndexPage(int type) {
        List<SimpleBlogListVO> simpleBlogListVOS = new ArrayList<>();
        List<Blog> blogs = blogMapper.findBlogListByType(type, 9, CategoryScope.LIFE_CATEGORY_NAMES);
        if (!CollectionUtils.isEmpty(blogs)) {
            for (Blog blog : blogs) {
                SimpleBlogListVO simpleBlogListVO = new SimpleBlogListVO();
                BeanUtils.copyProperties(blog, simpleBlogListVO);
                simpleBlogListVOS.add(simpleBlogListVO);
            }
        }
        return simpleBlogListVOS;
    }

    @Override
    public BlogDetailVO getBlogDetail(Long id) {
        Blog blog = blogMapper.selectByPrimaryKey(id);
        //不为空且状态为已发布
        BlogDetailVO blogDetailVO = getBlogDetailVO(blog,true);
        if (blogDetailVO != null) {
            return blogDetailVO;
        }
        return null;
    }

    @Override
    public PageResult getBlogsPageByTag(String tagName, int page) {
        if (PatternUtil.validKeyword(tagName)) {
            BlogTag tag = tagMapper.selectByTagName(tagName);
            if (tag != null && page > 0) {
                Map param = new HashMap();
                param.put("page", page);
                param.put("limit", LIST_PAGE_SIZE);
                param.put("tagId", tag.getTagId());
                PageQueryUtil pageUtil = new PageQueryUtil(param);
                List<Blog> blogList = blogMapper.getBlogsPageByTagId(pageUtil);
                List<BlogListVO> blogListVOS = getBlogListVOsByBlogs(blogList);
                int total = blogMapper.getTotalBlogsByTagId(pageUtil);
                PageResult pageResult = new PageResult(blogListVOS, total, pageUtil.getLimit(), pageUtil.getPage());
                return pageResult;
            }
        }
        return null;
    }

    @Override
    public PageResult getBlogsPageByCategory(String categoryName, int page) {
        return getBlogsPageByCategoryAndTag(categoryName, null, page);
    }

    @Override
    public PageResult getBlogsPageByCategoryAndTag(String categoryName, String tagName, int page) {
        if (!PatternUtil.validKeyword(categoryName) || page <= 0) {
            return null;
        }
        BlogCategory blogCategory = categoryMapper.selectByCategoryName(categoryName);
        if ("默认分类".equals(categoryName) && blogCategory == null) {
            blogCategory = new BlogCategory();
            blogCategory.setCategoryId(0);
        }
        if (blogCategory == null) {
            return null;
        }
        Map param = new HashMap();
        param.put("page", page);
        param.put("limit", LIST_PAGE_SIZE);
        param.put("blogCategoryId", blogCategory.getCategoryId());
        param.put("blogStatus", 1);//过滤发布状态下的数据
        // 标签作为类目内的收窄条件：两个条件在同一条 SQL 里同时生效
        if (tagName != null && !tagName.trim().isEmpty()) {
            BlogTag tag = tagMapper.selectByTagName(tagName.trim());
            // 标签不存在时用 -1 兜底，保证查出空集，而不是静默退化成整个类目
            param.put("tagId", tag == null ? -1 : tag.getTagId());
        }
        PageQueryUtil pageUtil = new PageQueryUtil(param);
        List<Blog> blogList = blogMapper.findBlogList(pageUtil);
        List<BlogListVO> blogListVOS = getBlogListVOsByBlogs(blogList);
        int total = blogMapper.getTotalBlogs(pageUtil);
        return new PageResult(blogListVOS, total, pageUtil.getLimit(), pageUtil.getPage());
    }

    @Override
    public PageResult getBlogsPageBySearch(String keyword, int page) {
        if (page > 0 && PatternUtil.validKeyword(keyword)) {
            int rows = LIST_PAGE_SIZE;
            int start = (page - 1) * rows;
            try {
                PageResult pageResultTemp = blogSolrServer.search(keyword, page, rows);
                @SuppressWarnings("unchecked")
                List<BlogListVO> blogListVOS = getBlogListVOsByBlogs((List<Blog>) pageResultTemp.getList());
                pageResultTemp.setList(blogListVOS);
                return pageResultTemp;
            } catch (Exception e) {
                // Solr 不可用时降级到数据库 LIKE 搜索
                List<Blog> blogList = blogMapper.searchByKeyword(keyword, start, rows);
                int total = blogMapper.getSearchCount(keyword);
                List<BlogListVO> blogListVOS = getBlogListVOsByBlogs(blogList);
                return new PageResult(blogListVOS, total, rows, page);
            }
        }
        return null;
    }

    @Override
    public BlogDetailVO getBlogDetailBySubUrl(String subUrl) {
        Blog blog = blogMapper.selectBySubUrl(subUrl);
        //不为空且状态为已发布
        BlogDetailVO blogDetailVO = getBlogDetailVO(blog,false);
        if (blogDetailVO != null) {
            return blogDetailVO;
        }
        return null;
    }

    /**
     * 方法抽取
     *
     * @param blog
     * @return
     */
    private BlogDetailVO getBlogDetailVO(Blog blog,Boolean filter) {
	        if (blog == null || (filter && blog.getBlogStatus() != 1)) {
	        	return null;
	        }
            //增加浏览量（原子更新，避免并发问题）
            blogMapper.incrementBlogViews(blog.getBlogId());
            blog.setBlogViews(blog.getBlogViews() + 1);
            BlogDetailVO blogDetailVO = new BlogDetailVO();
            BeanUtils.copyProperties(blog, blogDetailVO);
            // 前端已用 markdown-it 渲染（含代码高亮/mermaid），后端直接返回原始 markdown，避免双重渲染
            BlogCategory blogCategory = categoryMapper.selectByPrimaryKey(blog.getBlogCategoryId());
            if (blogCategory == null) {
                blogCategory = new BlogCategory();
                blogCategory.setCategoryId(0);
                blogCategory.setCategoryName("默认分类");
            }
            if (!StringUtils.isEmpty(blog.getBlogTags())) {
                //标签设置
                List<String> tags = Arrays.asList(blog.getBlogTags().split(","));
                blogDetailVO.setBlogTags(tags);
            }
            //设置评论数
            Map params = new HashMap();
            params.put("blogId", blog.getBlogId());
            params.put("commentStatus", 1);//过滤审核通过的数据
            blogDetailVO.setCommentCount(blogCommentMapper.getTotalBlogComments(params));
            return blogDetailVO;
    }

    private List<BlogListVO> getBlogListVOsByBlogs(List<Blog> blogList) {
        List<BlogListVO> blogListVOS = new ArrayList<>();
        if (!CollectionUtils.isEmpty(blogList)) {
            for (Blog blog : blogList) {
                BlogListVO blogListVO = new BlogListVO();
                BeanUtils.copyProperties(blog, blogListVO);
                blogListVOS.add(blogListVO);
            }
        }
        return blogListVOS;
    }

}
