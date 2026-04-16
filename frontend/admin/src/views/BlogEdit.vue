<template>
  <div class="blog-edit">
    <div class="page-header">
      <h3>{{ isEdit ? '编辑文章' : '发布文章' }}</h3>
      <el-button @click="router.back()">返回列表</el-button>
    </div>

    <el-card>
      <el-form :model="form" label-width="80px">
        <el-form-item label="文章标题" required>
          <el-input v-model="form.blogTitle" placeholder="请输入文章标题" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.blogCategoryId" placeholder="选择分类" style="width: 100%;">
                <el-option
                  v-for="c in categories"
                  :key="c.categoryId"
                  :label="c.categoryName"
                  :value="c.categoryId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签">
              <el-input v-model="form.blogTags" placeholder="多个标签用逗号分隔" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="自定义路径">
          <el-input v-model="form.blogSubUrl" placeholder="如: springboot-mybatis，默认为ID" />
        </el-form-item>

        <el-form-item label="文章内容" required>
          <MdEditor
            v-model="form.blogContent"
            :style="{ height: '520px', width: '100%' }"
            :preview="true"
            @on-upload-img="handleEditorUpload"
            placeholder="请输入 Markdown 内容..."
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="封面图片">
              <div class="cover-upload">
                <el-image
                  v-if="form.blogCoverImage"
                  :src="form.blogCoverImage"
                  fit="cover"
                  style="width: 200px; height: 120px; border-radius: 6px;"
                />
                <div v-else class="cover-placeholder">暂无封面</div>
                <div class="cover-actions">
                  <el-upload
                    :show-file-list="false"
                    :before-upload="handleUpload"
                    accept="image/*"
                  >
                    <el-button size="small">上传封面</el-button>
                  </el-upload>
                  <el-button size="small" @click="randomCover">随机封面</el-button>
                </div>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文章状态">
              <el-radio-group v-model="form.blogStatus">
                <el-radio :value="1">发布</el-radio>
                <el-radio :value="0">草稿</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="允许评论">
              <el-radio-group v-model="form.enableComment">
                <el-radio :value="0">允许</el-radio>
                <el-radio :value="1">禁止</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存文章</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { saveBlog, getBlogEditData, uploadFile } from '../api/admin'

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const categories = ref([])

const isEdit = computed(() => !!route.params.id)

const form = ref({
  blogId: null,
  blogTitle: '',
  blogCategoryId: null,
  blogTags: '',
  blogSubUrl: '',
  blogContent: '',
  blogCoverImage: '',
  blogStatus: 1,
  enableComment: 0
})

const fetchCategories = async () => {
  try {
    const res = await getBlogEditData()
    const d = res.data || {}
    categories.value = d.categories || []
  } catch (e) {
    console.error(e)
  }
}

const fetchBlog = async () => {
  if (!route.params.id) return
  try {
    const res = await getBlogEditData(route.params.id)
    const d = res.data || {}
    const blog = d.blog || {}
    if (blog) Object.assign(form.value, blog)
  } catch (e) {
    console.error(e)
  }
}

/** 封面图上传 */
const handleUpload = async (file) => {
  try {
    const res = await uploadFile(file)
    form.value.blogCoverImage = res.data || res
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error('上传失败')
  }
  return false
}

const randomCover = () => {
  const idx = Math.floor(Math.random() * 40) + 1
  form.value.blogCoverImage = `/admin/dist/img/rand/${idx}.jpg`
}

/** Markdown 编辑器内图片上传（粘贴/拖拽） */
const handleEditorUpload = async (files, callback) => {
  const urls = []
  for (const file of files) {
    try {
      const res = await uploadFile(file)
      const url = res.data || res
      urls.push(url)
    } catch (e) {
      ElMessage.error(`图片 ${file.name} 上传失败`)
    }
  }
  callback(urls)
}

const handleSave = async () => {
  if (!form.value.blogTitle) {
    return ElMessage.warning('请输入文章标题')
  }
  saving.value = true
  try {
    await saveBlog(form.value)
    ElMessage.success('保存成功')
    router.push('/blog/list')
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchCategories()
  fetchBlog()
})
</script>

<style scoped>
.cover-upload {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.cover-placeholder {
  width: 200px;
  height: 120px;
  border-radius: 6px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 14px;
}
.cover-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
