<template>
  <div class="blog-list">
    <div class="page-header">
      <h3>博客管理</h3>
      <el-button type="primary" @click="goEdit()">
        <el-icon><Plus /></el-icon>
        新增文章
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索标题/分类"
          clearable
          style="width: 300px;"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="blogId" label="ID" width="70" />
        <el-table-column prop="blogTitle" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="blogCategoryName" label="分类" width="120" />
        <el-table-column prop="blogTags" label="标签" min-width="150" show-overflow-tooltip />
        <el-table-column prop="blogViews" label="浏览" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.blogStatus === 1 ? 'success' : 'info'" size="small">
              {{ row.blogStatus === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="goEdit(row.blogId)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.blogId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBlogList, deleteBlog } from '../api/admin'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getBlogList({
      page: page.value,
      limit: pageSize.value,
      keyword: keyword.value
    })
    const d = res.data || {}
    tableData.value = d.list || []
    total.value = d.totalCount || d.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const goEdit = (id) => {
  if (id) {
    router.push(`/blog/edit/${id}`)
  } else {
    router.push('/blog/edit')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该文章吗？', '提示', { type: 'warning' })
    await deleteBlog([id])
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancel */ }
}

onMounted(() => fetchData())
</script>

<style scoped>
.search-bar {
  margin-bottom: 16px;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
