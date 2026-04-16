<template>
  <div class="link-page">
    <div class="page-header">
      <h3>友情链接</h3>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        新增链接
      </el-button>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="linkId" label="ID" width="80" />
        <el-table-column prop="linkName" label="链接名称" />
        <el-table-column prop="linkUrl" label="链接地址" min-width="200" show-overflow-tooltip />
        <el-table-column label="链接描述" prop="linkDescription" min-width="160" show-overflow-tooltip />
        <el-table-column label="排序" prop="linkRank" width="80" align="center" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.linkType === 1 ? 'warning' : 'success'" size="small">
              {{ row.linkType === 1 ? '友链' : '推荐' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.linkId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑链接' : '新增链接'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="链接名称" required>
          <el-input v-model="form.linkName" placeholder="请输入链接名称" />
        </el-form-item>
        <el-form-item label="链接地址" required>
          <el-input v-model="form.linkUrl" placeholder="请输入链接URL" />
        </el-form-item>
        <el-form-item label="链接描述">
          <el-input v-model="form.linkDescription" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.linkRank" :min="0" />
        </el-form-item>
        <el-form-item label="链接类型">
          <el-radio-group v-model="form.linkType">
            <el-radio :value="0">推荐</el-radio>
            <el-radio :value="1">友链</el-radio>
          </el-radio-group>
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
import { getLinkList, saveLink, updateLink, deleteLink } from '../api/admin'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)

const form = ref({
  linkId: null,
  linkName: '',
  linkUrl: '',
  linkDescription: '',
  linkRank: 0,
  linkType: 1
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLinkList({ page: 1, limit: 100 })
    const d = res.data || {}
    tableData.value = d.list || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  if (row) {
    isEdit.value = true
    form.value = { ...row }
  } else {
    isEdit.value = false
    form.value = { linkId: null, linkName: '', linkUrl: '', linkDescription: '', linkRank: 0, linkType: 1 }
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.linkName || !form.value.linkUrl) return ElMessage.warning('请填写必填项')
  saving.value = true
  try {
    if (isEdit.value) {
      await updateLink(form.value)
    } else {
      await saveLink(form.value)
    }
    ElMessage.success('保存成功')
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
    await ElMessageBox.confirm('确定删除该链接吗？', '提示', { type: 'warning' })
    await deleteLink([id])
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancel */ }
}

onMounted(() => fetchData())
</script>
