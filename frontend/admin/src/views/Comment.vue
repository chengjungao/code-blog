<template>
  <div class="comment-page">
    <div class="page-header">
      <h3>评论管理</h3>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="commentId" label="ID" width="80" />
        <el-table-column prop="blogTitle" label="文章" min-width="160" show-overflow-tooltip />
        <el-table-column prop="commentator" label="评论人" width="120" />
        <el-table-column prop="commentBody" label="评论内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.replyBody ? 'warning' : 'success'" size="small">
              {{ row.replyBody ? '回复' : '评论' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.replyBody" size="small" @click="openReply(row)">回复</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.commentId)">删除</el-button>
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

    <el-dialog v-model="replyVisible" title="回复评论" width="500px">
      <div class="reply-original">
        <p class="label">原评论：</p>
        <p>{{ currentComment.commentator }}：{{ currentComment.commentBody }}</p>
      </div>
      <el-input
        v-model="replyBody"
        type="textarea"
        :rows="4"
        placeholder="请输入回复内容"
      />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReply" :loading="replying">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCommentList, deleteComment, replyComment } from '../api/admin'

const loading = ref(false)
const replying = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const replyVisible = ref(false)
const replyBody = ref('')
const currentComment = ref({})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCommentList({ page: page.value, limit: pageSize.value })
    const d = res.data || {}
    tableData.value = d.list || []
    total.value = d.totalCount || d.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openReply = (row) => {
  currentComment.value = row
  replyBody.value = ''
  replyVisible.value = true
}

const handleReply = async () => {
  if (!replyBody.value) return ElMessage.warning('请输入回复内容')
  replying.value = true
  try {
    await replyComment({
      commentId: currentComment.value.commentId,
      replyBody: replyBody.value
    })
    ElMessage.success('回复成功')
    replyVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    replying.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该评论吗？', '提示', { type: 'warning' })
    await deleteComment([id])
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancel */ }
}

onMounted(() => fetchData())
</script>

<style scoped>
.reply-original {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}
.reply-original .label {
  font-weight: 600;
  margin-bottom: 4px;
  color: #1d1e2c;
}
.reply-original p {
  margin: 0;
  font-size: 14px;
  color: #606266;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
