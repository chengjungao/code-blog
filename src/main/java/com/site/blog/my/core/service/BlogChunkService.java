package com.site.blog.my.core.service;

import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.entity.BlogChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 文章分块服务。
 * 采用混合策略：先按 Markdown 标题切分，超长段落再用滑动窗口二次切分。
 */
@Service
public class BlogChunkService {

	private static final int MAX_CHUNK_SIZE = 600;
	private static final int OVERLAP_SIZE = 100;
	/** 单篇文章最大索引字符数，超出部分截断，防止堆溢出 */
	private static final int MAX_CONTENT_LENGTH = 10000;
	/** 单篇文章最大分块数 */
	private static final int MAX_CHUNKS_PER_BLOG = 20;

	/** 匹配 Markdown 标题行：## 标题 或 ### 标题 */
	private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,3})\\s+(.+)$");

	/**
	 * 将一篇博客文章切分为多个分块
	 *
	 * @param blog 博客文章
	 * @return 分块列表
	 */
	public List<BlogChunk> splitBlog(Blog blog) {
		String content = blog.getBlogContent();
		if (content == null || content.trim().isEmpty()) {
			return new ArrayList<>();
		}

		// 超长文章截断，防止堆溢出
		if (content.length() > MAX_CONTENT_LENGTH) {
			content = content.substring(0, MAX_CONTENT_LENGTH);
		}

		// 1. 按 Markdown 标题切分为段落
		List<Section> sections = splitByHeadings(content);

		// 2. 对超长段落用滑动窗口二次切分，收集所有文本块
		List<String> textChunks = new ArrayList<>();
		List<String> titleChains = new ArrayList<>();
		for (Section section : sections) {
			String text = section.content.trim();
			if (text.isEmpty()) continue;

			if (text.length() <= MAX_CHUNK_SIZE) {
				textChunks.add(text);
				titleChains.add(section.titleChain);
			} else {
				// 滑动窗口二次切分
				List<String> subChunks = slidingWindowSplit(text);
				for (String sub : subChunks) {
					textChunks.add(sub);
					titleChains.add(section.titleChain);
				}
			}
		}

		// 3. 构建 BlogChunk 对象（限制最大分块数）
		List<BlogChunk> chunks = new ArrayList<>();
		int limit = Math.min(textChunks.size(), MAX_CHUNKS_PER_BLOG);
		for (int i = 0; i < limit; i++) {
			BlogChunk chunk = new BlogChunk();
			chunk.setId(blog.getBlogId() + "_" + i);
			chunk.setBlogId(blog.getBlogId());
			chunk.setBlogTitle(blog.getBlogTitle());
			chunk.setBlogSubUrl(blog.getBlogSubUrl());
			chunk.setChunkIndex(i);
			chunk.setChunkTitle(titleChains.get(i));
			chunk.setChunkContent(textChunks.get(i));
			chunks.add(chunk);
		}
		return chunks;
	}

	/**
	 * 按 Markdown 标题切分文章为段落，每个段落保留标题链上下文
	 */
	private List<Section> splitByHeadings(String content) {
		List<Section> sections = new ArrayList<>();
		String[] lines = content.split("\n");

		// 标题栈：index 0 = #, 1 = ##, 2 = ###
		String[] headingStack = new String[3];
		StringBuilder currentContent = new StringBuilder();
		boolean hasContent = false;

		for (String line : lines) {
			Matcher matcher = HEADING_PATTERN.matcher(line.trim());
			if (matcher.matches()) {
				// 遇到新标题，先保存之前的内容
				if (hasContent && currentContent.length() > 0) {
					sections.add(new Section(buildTitleChain(headingStack), currentContent.toString()));
					currentContent = new StringBuilder();
				}

				int level = matcher.group(1).length() - 1; // 0-based: # = 0, ## = 1, ### = 2
				String title = matcher.group(2).trim();
				headingStack[level] = title;
				// 清除更低级别的标题
				for (int i = level + 1; i < 3; i++) {
					headingStack[i] = null;
				}
				hasContent = true;
				// 将标题本身也加入内容（有助于搜索匹配）
				currentContent.append(line.trim()).append("\n");
			} else {
				currentContent.append(line).append("\n");
				hasContent = true;
			}
		}

		// 保存最后一段
		if (hasContent && currentContent.length() > 0) {
			sections.add(new Section(buildTitleChain(headingStack), currentContent.toString()));
		}

		// 如果整篇文章没有标题，返回整段作为一个 section
		if (sections.isEmpty()) {
			sections.add(new Section("", content));
		}

		return sections;
	}

	/**
	 * 构建标题链字符串，如 "搜索引擎架构 > 倒排索引"
	 */
	private String buildTitleChain(String[] headingStack) {
		StringBuilder chain = new StringBuilder();
		for (String h : headingStack) {
			if (h != null && !h.isEmpty()) {
				if (chain.length() > 0) {
					chain.append(" > ");
				}
				chain.append(h);
			}
		}
		return chain.toString();
	}

	/**
	 * 滑动窗口切分超长文本
	 */
	private List<String> slidingWindowSplit(String text) {
		List<String> chunks = new ArrayList<>();
		int start = 0;
		while (start < text.length()) {
			// 安全兑底：单个文本最多切 30 块，防止异常内容导致内存溢出
			if (chunks.size() >= 30) break;

			int end = Math.min(start + MAX_CHUNK_SIZE, text.length());

			// 尝试在句号、换行处断开，避免切断句子
			if (end < text.length()) {
				int breakPoint = findBreakPoint(text, start, end);
				if (breakPoint > start) {
					end = breakPoint;
				}
			}

			chunks.add(text.substring(start, end).trim());
			start = end - OVERLAP_SIZE;
			if (start >= text.length()) break;
			// 防止死循环：如果 overlap 后没有前进，强制前进
			if (start <= end - MAX_CHUNK_SIZE) {
				start = end;
			}
		}
		return chunks;
	}

	/**
	 * 在 [searchStart, searchEnd] 范围内寻找合适的断句点
	 */
	private int findBreakPoint(String text, int searchStart, int searchEnd) {
		// 优先在换行处断开（最后 20% 区间内）
		int searchFrom = searchEnd - (int) ((searchEnd - searchStart) * 0.2);
		if (searchFrom < searchStart) searchFrom = searchStart;

		for (int i = searchEnd; i >= searchFrom; i--) {
			char c = text.charAt(i - 1);
			if (c == '\n' || c == '。' || c == '！' || c == '？' || c == '.' || c == '!') {
				return i;
			}
		}
		return searchEnd; // 没找到合适的断点，就用原位置
	}

	/**
	 * 段落内部类：标题链 + 内容
	 */
	private static class Section {
		final String titleChain;
		final String content;

		Section(String titleChain, String content) {
			this.titleChain = titleChain;
			this.content = content;
		}
	}
}
