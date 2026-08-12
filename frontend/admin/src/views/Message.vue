<template>
  <div class="message-page">
    <div class="page-header">
      <h3>留言管理</h3>
    </div>

    <el-card>
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="筛选状态" clearable @change="handleFilter" style="width: 140px">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
        </el-select>
        <el-button-group>
          <el-button type="success" size="small" :disabled="!selection.length" @click="handleBatchCheck">
            批量通过
          </el-button>
          <el-button type="danger" size="small" :disabled="!selection.length" @click="handleBatchDelete">
            批量删除
          </el-button>
        </el-button-group>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="messageId" label="ID" width="70" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
        <el-table-column prop="messageBody" label="留言内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.messageStatus === 1 ? 'success' : 'warning'" size="small">
              {{ row.messageStatus === 1 ? '已通过' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="messageCreateTime" label="时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.messageStatus === 0" size="small" type="success" @click="handleCheck(row.messageId)">
              通过
            </el-button>
            <el-button v-if="row.messageStatus === 1 && !row.replyBody" size="small" @click="openReply(row)">
              回复
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.messageId)">删除</el-button>
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

    <el-dialog v-model="replyVisible" title="回复留言" width="500px">
      <div class="reply-original">
        <p class="label">原留言：</p>
        <p>{{ currentMessage.nickname }}：{{ currentMessage.messageBody }}</p>
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
import { getMessageList, checkDoneMessages, replyMessage, deleteMessage } from '../api/admin'

const loading = ref(false)
const replying = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref(null)
const selection = ref([])
const replyVisible = ref(false)
const replyBody = ref('')
const currentMessage = ref({})

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, limit: pageSize.value }
    if (filterStatus.value !== null) {
      params.messageStatus = filterStatus.value
    }
    const res = await getMessageList(params)
    const d = res.data || {}
    tableData.value = d.list || []
    total.value = d.totalCount || d.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  page.value = 1
  fetchData()
}

const handleSelectionChange = (rows) => {
  selection.value = rows
}

const handleCheck = async (id) => {
  try {
    await checkDoneMessages([id])
    ElMessage.success('审核通过')
    fetchData()
  } catch (e) { console.error(e) }
}

const handleBatchCheck = async () => {
  const ids = selection.value.map(r => r.messageId)
  try {
    await checkDoneMessages(ids)
    ElMessage.success(`已通过 ${ids.length} 条留言`)
    fetchData()
  } catch (e) { console.error(e) }
}

const openReply = (row) => {
  currentMessage.value = row
  replyBody.value = ''
  replyVisible.value = true
}

const handleReply = async () => {
  if (!replyBody.value) return ElMessage.warning('请输入回复内容')
  replying.value = true
  try {
    await replyMessage({
      messageId: currentMessage.value.messageId,
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
    await ElMessageBox.confirm('确定删除该留言吗？', '提示', { type: 'warning' })
    await deleteMessage([id])
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancel */ }
}

const handleBatchDelete = async () => {
  const ids = selection.value.map(r => r.messageId)
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条留言吗？`, '提示', { type: 'warning' })
    await deleteMessage(ids)
    ElMessage.success('批量删除成功')
    fetchData()
  } catch (e) { /* cancel */ }
}

onMounted(() => fetchData())
</script>

<style scoped>
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

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
