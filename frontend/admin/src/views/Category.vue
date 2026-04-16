<template>
  <div class="category-page">
    <div class="page-header">
      <h3>分类管理</h3>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        新增分类
      </el-button>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="categoryId" label="ID" width="80" />
        <el-table-column prop="categoryName" label="分类名称" />
        <el-table-column prop="categoryRank" label="排序" width="100" />
        <el-table-column label="图标" width="80">
          <template #default="{ row }">
            <el-image
              v-if="row.categoryIcon"
              :src="row.categoryIcon"
              style="width: 36px; height: 36px; border-radius: 4px;"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.categoryId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.categoryRank" :min="0" />
        </el-form-item>
        <el-form-item label="分类图标">
          <el-upload
            :show-file-list="false"
            :before-upload="handleUpload"
            accept="image/*"
          >
            <el-image
              v-if="form.categoryIcon"
              :src="form.categoryIcon"
              style="width: 60px; height: 60px; border-radius: 6px;"
              fit="cover"
            />
            <el-button v-else size="small">上传图标</el-button>
          </el-upload>
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
import { getCategoryList, saveCategory, updateCategory, deleteCategory, uploadFile } from '../api/admin'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)

const form = ref({
  categoryId: null,
  categoryName: '',
  categoryRank: 0,
  categoryIcon: ''
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCategoryList({ page: 1, limit: 100 })
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
    form.value = { categoryId: null, categoryName: '', categoryRank: 0, categoryIcon: '' }
  }
  dialogVisible.value = true
}

const handleUpload = async (file) => {
  try {
    const res = await uploadFile(file)
    form.value.categoryIcon = res.data || res
  } catch (e) {
    ElMessage.error('上传失败')
  }
  return false
}

const handleSave = async () => {
  if (!form.value.categoryName) return ElMessage.warning('请输入分类名称')
  saving.value = true
  try {
    if (isEdit.value) {
      await updateCategory(form.value)
    } else {
      await saveCategory(form.value)
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
    await ElMessageBox.confirm('确定删除该分类吗？', '提示', { type: 'warning' })
    await deleteCategory([id])
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancel */ }
}

onMounted(() => fetchData())
</script>
