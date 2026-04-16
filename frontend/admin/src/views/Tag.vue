<template>
  <div class="tag-page">
    <div class="page-header">
      <h3>标签管理</h3>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        新增标签
      </el-button>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="tagId" label="ID" width="80" />
        <el-table-column prop="tagName" label="标签名称" />
        <el-table-column prop="blogCount" label="文章数" width="100" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDelete(row.tagId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增标签" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标签名称" required>
          <el-input v-model="form.tagName" placeholder="请输入标签名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTagList, saveTag, deleteTag } from '../api/admin'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const form = ref({ tagName: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTagList({ page: 1, limit: 100 })
    const d = res.data || {}
    tableData.value = d.list || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openDialog = () => {
  form.value = { tagName: '' }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.tagName) return ElMessage.warning('请输入标签名称')
  saving.value = true
  try {
    await saveTag(form.value)
    ElMessage.success('添加成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该标签吗？', '提示', { type: 'warning' })
    await deleteTag([id])
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancel */ }
}

onMounted(() => fetchData())
</script>
