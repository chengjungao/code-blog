package com.site.blog.my.core.solr;

import java.util.List;

import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.entity.BlogChunk;
import com.site.blog.my.core.util.PageResult;

public interface BlogSolrServer {
	
	public void add(List<Blog> blogs);
	
	public void add(Blog blog);
	
	public void delete(String... id);
	
	public void deleteAll();
	
	public PageResult search(String keyword,int page,int rows);

	// ---- 分块索引相关 ----

	/** 批量添加分块到 chunk core */
	void addChunks(List<BlogChunk> chunks);

	/** 删除指定文章的所有分块 */
	void deleteChunksByBlogId(Long blogId);

	/** 清空所有分块 */
	void deleteAllChunks();

	/** 根据关键词召回相关分块 */
	List<BlogChunk> retrieveChunks(String keywords, int topK);
}
