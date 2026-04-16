package com.site.blog.my.core.solr;

import java.util.List;

import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.util.PageResult;

public interface BlogSolrServer {
	
	public void add(List<Blog> blogs);
	
	public void add(Blog blog);
	
	public void delete(String... id);
	
	public void deleteAll();
	
	public PageResult search(String keyword,int page,int rows);
}
