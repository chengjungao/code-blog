package com.site.blog.my.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.site.blog.my.core.dao.BlogMapper;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.solr.BlogSolrServer;

@Component
public class InitSolrIndex implements ApplicationRunner {
	
	@Autowired
	BlogMapper blogMapper;
	
	@Autowired
	BlogSolrServer blogSolrServer;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		List<Blog> blogs =  blogMapper.findAll();
		blogSolrServer.deleteAll();
		blogSolrServer.add(blogs);
	}

}
