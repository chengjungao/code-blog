package com.site.blog.my.core.solr;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.util.PageResult;

@Service
public class BlogSolrServerImpl implements BlogSolrServer {
	
	EmbeddedSolrServer solrServer;
	
	String queryStr = "default_search:(%s)";
	
	public BlogSolrServerImpl(@Value("${solr.home:}")String solrHome) {
		this.solrServer = new EmbeddedSolrServer(Paths.get(solrHome), "blog");
	}
	
	
	@Override
	public void add(List<Blog> blogs) {
		try {
			for (Blog blog : blogs) {
				blog.setId(String.valueOf(blog.getBlogId()));
			}
			solrServer.addBeans(blogs);
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Add Docs exception!",e);
		}
	}

	@Override
	public PageResult search(String keyword, int page, int rows) {
		SolrQuery params = new SolrQuery(String.format(queryStr, keyword));
		params.addFilterQuery("blogStatus:1");
		params.addFilterQuery("isDeleted:0");
		params.setSort("score", ORDER.desc);
		params.setRows(rows);
		params.setStart((page -1) * rows);
		try {
			QueryResponse query = solrServer.query(params);
			return new PageResult(query.getBeans(Blog.class) , (int)query.getResults().getNumFound(), rows, page);
		} catch (Exception e) {
			throw new RuntimeException("Search Docs exception!",e);
		}
	}

	@Override
	public void add(Blog blog) {
		try {
			blog.setId(String.valueOf(blog.getBlogId()));
			solrServer.addBean(blog);
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Add Doc exception!",e);
		}
	}

	@Override
	public void delete(String... id) {
		try {
			solrServer.deleteById(Arrays.asList(id));
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Delete Doc exception!",e);
		}
	}

	@Override
	public void deleteAll() {
		try {
			solrServer.deleteByQuery("*:*");
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Delete Doc exception!",e);
		}
	}

}
