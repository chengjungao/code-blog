package com.site.blog.my.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.site.blog.my.core.dao.BlogMapper;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.entity.BlogChunk;
import com.site.blog.my.core.service.BlogChunkService;
import com.site.blog.my.core.solr.BlogSolrServer;

@Component
public class InitSolrIndex implements ApplicationRunner {
	
	@Autowired
	BlogMapper blogMapper;
	
	@Autowired
	BlogSolrServer blogSolrServer;

	@Autowired
	BlogChunkService blogChunkService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// 异步重建 Solr 索引，不阻塞应用启动
		Thread indexer = new Thread(this::rebuildIndex, "solr-index-init");
		indexer.setDaemon(true);
		indexer.start();
	}

	/** 分块索引每批加载文章数，控制内存 */
	private static final int CHUNK_BATCH_SIZE = 2;

	private void rebuildIndex() {
		try {
			// 延迟 60 秒启动，避免应用刚启动时 CPU 负载过高
			Thread.sleep(60_000);

			long start = System.currentTimeMillis();
			System.out.println("[Solr] Background index rebuild started...");

			// 文章索引（不含 blogContent，轻量加载）
			List<Blog> blogs = blogMapper.findAll();
			blogSolrServer.deleteAll();
			blogSolrServer.add(blogs);
			int blogCount = blogs.size();

			// 分块索引：分页加载含 blogContent 的已发布文章，分批处理控制内存
			blogSolrServer.deleteAllChunks();
			int chunkCount = 0;
			int total = blogMapper.countPublished();
			int offset = 0;
			while (offset < total) {
				List<Blog> batch = blogMapper.findPublishedWithContent(offset, CHUNK_BATCH_SIZE);
				if (batch.isEmpty()) break;
				for (Blog blog : batch) {
					List<BlogChunk> chunks = blogChunkService.splitBlog(blog);
					if (!chunks.isEmpty()) {
						blogSolrServer.addChunks(chunks);
						chunkCount += chunks.size();
					}
				}
				offset += batch.size();
				// 批次间提示 GC，释放大文本内存
				batch.clear();
				System.gc();
			}

			long cost = System.currentTimeMillis() - start;
			System.out.println("[Solr] Index rebuild completed in " + cost + "ms (blogs: " + blogCount + ", chunks: " + chunkCount + ")");
		} catch (Exception e) {
			System.err.println("[WARN] Solr background index rebuild failed: " + e.getMessage());
		}
	}

}
