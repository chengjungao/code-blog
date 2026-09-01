package com.site.blog.my.core.solr;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.entity.BlogChunk;
import com.site.blog.my.core.util.PageResult;

@Service
public class BlogSolrServerImpl implements BlogSolrServer {
	
	EmbeddedSolrServer solrServer;
	
	String queryStr = "default_search:(%s)";
	
	public BlogSolrServerImpl(@Value("${solr.home:}")String solrHome) {
		try {
			this.solrServer = new EmbeddedSolrServer(Paths.get(solrHome), "blog");
		} catch (Exception e) {
			System.err.println("[WARN] Solr initialization failed, search will fall back to DB: " + e.getMessage());
			this.solrServer = null;
		}
	}
	
	
	@Override
	public void add(List<Blog> blogs) {
		if (solrServer == null) return;
		try {
			for (Blog blog : blogs) {
				blog.setId(String.valueOf(blog.getBlogId()));
				blog.setDocType("blog");
			}
			solrServer.addBeans(blogs);
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Add Docs exception!",e);
		}
	}

	@Override
	public PageResult search(String keyword, int page, int rows) {
		if (solrServer == null) throw new RuntimeException("Solr not available");
		SolrQuery params = new SolrQuery(String.format(queryStr, keyword));
		params.addFilterQuery("docType:blog");
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
		if (solrServer == null) return;
		try {
			blog.setId(String.valueOf(blog.getBlogId()));
			blog.setDocType("blog");
			solrServer.addBean(blog);
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Add Doc exception!",e);
		}
	}

	@Override
	public void delete(String... id) {
		if (solrServer == null) return;
		try {
			solrServer.deleteById(Arrays.asList(id));
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Delete Doc exception!",e);
		}
	}

	@Override
	public void deleteAll() {
		if (solrServer == null) return;
		try {
			solrServer.deleteByQuery("*:*");
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Delete Doc exception!",e);
		}
	}

	// ---- 分块索引相关（单 core 方案，通过 docType 区分） ----

	@Override
	public void addChunks(List<BlogChunk> chunks) {
		if (solrServer == null || chunks == null || chunks.isEmpty()) return;
		try {
			for (BlogChunk chunk : chunks) {
				chunk.setDocType("chunk");
			}
			solrServer.addBeans(chunks);
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Add Chunks exception!", e);
		}
	}

	@Override
	public void deleteChunksByBlogId(Long blogId) {
		if (solrServer == null) return;
		try {
			solrServer.deleteByQuery("docType:chunk AND blogId:" + blogId);
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Delete Chunks exception!", e);
		}
	}

	@Override
	public void deleteAllChunks() {
		if (solrServer == null) return;
		try {
			solrServer.deleteByQuery("docType:chunk");
			solrServer.commit();
		} catch (Exception e) {
			throw new RuntimeException("Delete All Chunks exception!", e);
		}
	}

	@Override
	public List<BlogChunk> retrieveChunks(String keywords, int topK) {
		if (solrServer == null) return Collections.emptyList();
		try {
			SolrQuery params = new SolrQuery(String.format(queryStr, keywords));
			params.addFilterQuery("docType:chunk");
			params.setRows(topK);
			params.setSort("score", ORDER.desc);
			params.setFields("blogId", "blogTitle", "blogSubUrl", "chunkTitle", "chunkContent", "chunkIndex");
			QueryResponse response = solrServer.query(params);
			return response.getBeans(BlogChunk.class);
		} catch (Exception e) {
			System.err.println("[WARN] Chunk retrieval failed: " + e.getMessage());
			return Collections.emptyList();
		}
	}

}
