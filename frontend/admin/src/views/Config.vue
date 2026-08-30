<template>
  <div class="config-page">
    <div class="page-header">
      <h3>系统配置</h3>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <!-- 站点信息 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span style="font-weight: 600;">站点信息</span>
          </template>
          <el-form :model="websiteForm" label-width="100px">
            <el-form-item label="站点名称">
              <el-input v-model="websiteForm.websiteName" placeholder="站点名称" />
            </el-form-item>
            <el-form-item label="站点描述">
              <el-input v-model="websiteForm.websiteDescription" placeholder="站点描述" />
            </el-form-item>
            <el-form-item label="站点Logo">
              <el-input v-model="websiteForm.websiteLogo" placeholder="Logo URL" />
            </el-form-item>
            <el-form-item label="Favicon">
              <el-input v-model="websiteForm.websiteIcon" placeholder="favicon.ico URL" />
            </el-form-item>
            <el-form-item label="站点主题">
              <el-select v-model="websiteForm.theme" style="width: 100%;">
                <el-option label="default" value="default" />
                <el-option label="amaze" value="amaze" />
                <el-option label="yummy-jekyll" value="yummy-jekyll" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveWebsite" :loading="savingWebsite">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 个人信息 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span style="font-weight: 600;">个人信息</span>
          </template>
          <el-form :model="userInfoForm" label-width="100px">
            <el-form-item label="个人头像">
              <div class="avatar-upload">
                <el-image
                  v-if="userInfoForm.yourAvatar"
                  :src="userInfoForm.yourAvatar"
                  style="width: 50px; height: 50px; border-radius: 50%;"
                  fit="cover"
                />
                <span v-else style="color: #c0c4cc; font-size: 13px;">暂无头像</span>
                <el-upload
                  :show-file-list="false"
                  :before-upload="handleAvatarUpload"
                  accept="image/*"
                  style="margin-left: 12px;"
                >
                  <el-button size="small">上传</el-button>
                </el-upload>
              </div>
              <el-input v-model="userInfoForm.yourAvatar" placeholder="头像 URL" style="margin-top: 8px;" />
            </el-form-item>
            <el-form-item label="个人名称">
              <el-input v-model="userInfoForm.yourName" placeholder="个人名称" />
            </el-form-item>
            <el-form-item label="个人邮箱">
              <el-input v-model="userInfoForm.yourEmail" placeholder="个人邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveUserInfo" :loading="savingUserInfo">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 底部设置 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span style="font-weight: 600;">底部设置</span>
          </template>
          <el-form :model="footerForm" label-width="120px">
            <el-form-item label="底部About">
              <el-input v-model="footerForm.footerAbout" placeholder="About" />
            </el-form-item>
            <el-form-item label="备案号">
              <el-input v-model="footerForm.footerICP" placeholder="备案号" />
            </el-form-item>
            <el-form-item label="Copyright">
              <el-input v-model="footerForm.footerCopyRight" placeholder="Copyright" />
            </el-form-item>
            <el-form-item label="Powered By">
              <el-input v-model="footerForm.footerPoweredBy" placeholder="Powered By" />
            </el-form-item>
            <el-form-item label="PB URL">
              <el-input v-model="footerForm.footerPoweredByURL" placeholder="Powered By URL" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveFooter" :loading="savingFooter">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- SEO 静态渲染 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span style="font-weight: 600;">SEO 静态渲染</span>
              <el-tag :type="rendering ? 'warning' : 'success'" size="small">
                {{ rendering ? '渲染中...' : '空闲' }}
              </el-tag>
            </div>
          </template>
          <div style="display: flex; align-items: center; gap: 16px;">
            <el-button type="primary" @click="handleRenderAll" :loading="rendering" :disabled="rendering">
              全量渲染
            </el-button>
            <span style="color: #909399; font-size: 13px;">
              渲染所有已发布博客为静态 HTML，提升 SEO 和首屏加载速度。容器启动时自动渲染，每天凌晨 2 点定时执行。
            </span>
          </div>
          <div v-if="renderOutput" style="margin-top: 16px;">
            <el-alert
              :title="renderSuccess ? '渲染成功' : '渲染失败'"
              :type="renderSuccess ? 'success' : 'error'"
              :description="renderMessage"
              show-icon
              :closable="true"
              @close="renderOutput = false"
            />
            <el-input
              v-if="renderLog"
              type="textarea"
              :model-value="renderLog"
              readonly
              :autosize="{ minRows: 3, maxRows: 15 }"
              style="margin-top: 12px; font-family: monospace;"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigList, saveWebsiteConfig, saveUserInfoConfig, saveFooterConfig, uploadFile, renderAllPages, getRenderStatus } from '../api/admin'

const loading = ref(false)
const savingWebsite = ref(false)
const savingUserInfo = ref(false)
const savingFooter = ref(false)

// SEO 渲染状态
const rendering = ref(false)
const renderOutput = ref(false)
const renderSuccess = ref(false)
const renderMessage = ref('')
const renderLog = ref('')

// 原始配置 Map（key-value）
const configMap = ref({})

const websiteForm = reactive({
  websiteName: '',
  websiteDescription: '',
  websiteLogo: '',
  websiteIcon: '',
  theme: 'default'
})

const userInfoForm = reactive({
  yourAvatar: '',
  yourName: '',
  yourEmail: ''
})

const footerForm = reactive({
  footerAbout: '',
  footerICP: '',
  footerCopyRight: '',
  footerPoweredBy: '',
  footerPoweredByURL: ''
})

/** 从 configMap 回填表单 */
const fillForms = () => {
  const m = configMap.value
  websiteForm.websiteName = m['websiteName'] || ''
  websiteForm.websiteDescription = m['websiteDescription'] || ''
  websiteForm.websiteLogo = m['websiteLogo'] || ''
  websiteForm.websiteIcon = m['websiteIcon'] || ''
  websiteForm.theme = m['theme'] || 'default'

  userInfoForm.yourAvatar = m['yourAvatar'] || ''
  userInfoForm.yourName = m['yourName'] || ''
  userInfoForm.yourEmail = m['yourEmail'] || ''

  footerForm.footerAbout = m['footerAbout'] || ''
  footerForm.footerICP = m['footerICP'] || ''
  footerForm.footerCopyRight = m['footerCopyRight'] || ''
  footerForm.footerPoweredBy = m['footerPoweredBy'] || ''
  footerForm.footerPoweredByURL = m['footerPoweredByURL'] || ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getConfigList()
    const data = res.data || {}
    // getAllConfigs 返回 Map<String, String>
    configMap.value = data
    fillForms()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const saveWebsite = async () => {
  savingWebsite.value = true
  try {
    await saveWebsiteConfig({ ...websiteForm })
    ElMessage.success('站点信息保存成功')
  } catch (e) { console.error(e) }
  finally { savingWebsite.value = false }
}

const saveUserInfo = async () => {
  savingUserInfo.value = true
  try {
    await saveUserInfoConfig({ ...userInfoForm })
    ElMessage.success('个人信息保存成功')
  } catch (e) { console.error(e) }
  finally { savingUserInfo.value = false }
}

const saveFooter = async () => {
  savingFooter.value = true
  try {
    await saveFooterConfig({ ...footerForm })
    ElMessage.success('底部设置保存成功')
  } catch (e) { console.error(e) }
  finally { savingFooter.value = false }
}

const handleAvatarUpload = async (file) => {
  try {
    const res = await uploadFile(file)
    userInfoForm.yourAvatar = res.data || res
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error('上传失败')
  }
  return false
}

onMounted(() => {
  fetchData()
  checkRenderStatus()
})

const checkRenderStatus = async () => {
  try {
    const res = await getRenderStatus()
    rendering.value = res.data?.rendering || false
  } catch (e) { /* ignore */ }
}

const handleRenderAll = async () => {
  rendering.value = true
  renderOutput.value = false
  try {
    const res = await renderAllPages()
    renderSuccess.value = res.resultCode === 200
    renderMessage.value = res.message || (renderSuccess.value ? '渲染完成' : '渲染失败')
    renderLog.value = res.data?.output || ''
    renderOutput.value = true
  } catch (e) {
    renderSuccess.value = false
    renderMessage.value = '渲染请求失败'
    renderOutput.value = true
  } finally {
    rendering.value = false
  }
}
</script>

<style scoped>
.avatar-upload {
  display: flex;
  align-items: center;
}
</style>
