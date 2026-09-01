package com.site.blog.my.core.entity;

import org.apache.solr.client.solrj.beans.Field;

/**
 * 文章分块实体，仅用于 Solr 索引，不入库 MySQL
 */
public class BlogChunk {

	@Field("id")
	private String id;

	@Field("docType")
	private String docType;

	@Field("blogId")
	private Long blogId;

	@Field("blogTitle")
	private String blogTitle;

	@Field("blogSubUrl")
	private String blogSubUrl;

	@Field("chunkIndex")
	private Integer chunkIndex;

	@Field("chunkTitle")
	private String chunkTitle;

	@Field("chunkContent")
	private String chunkContent;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDocType() {
		return docType;
	}

	public void setDocType(String docType) {
		this.docType = docType;
	}

	public Long getBlogId() {
		return blogId;
	}

	public void setBlogId(Long blogId) {
		this.blogId = blogId;
	}

	public String getBlogTitle() {
		return blogTitle;
	}

	public void setBlogTitle(String blogTitle) {
		this.blogTitle = blogTitle;
	}

	public String getBlogSubUrl() {
		return blogSubUrl;
	}

	public void setBlogSubUrl(String blogSubUrl) {
		this.blogSubUrl = blogSubUrl;
	}

	public Integer getChunkIndex() {
		return chunkIndex;
	}

	public void setChunkIndex(Integer chunkIndex) {
		this.chunkIndex = chunkIndex;
	}

	public String getChunkTitle() {
		return chunkTitle;
	}

	public void setChunkTitle(String chunkTitle) {
		this.chunkTitle = chunkTitle;
	}

	public String getChunkContent() {
		return chunkContent;
	}

	public void setChunkContent(String chunkContent) {
		this.chunkContent = chunkContent;
	}
}
