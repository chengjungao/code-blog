<template>
  <div class="profile-page">
    <div class="page-header">
      <h3>个人中心</h3>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span style="font-weight: 600;">修改密码</span>
          </template>
          <el-form :model="pwdForm" label-width="100px">
            <el-form-item label="原密码" required>
              <el-input v-model="pwdForm.originalPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" required>
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handlePwdSave" :loading="pwdSaving">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span style="font-weight: 600;">修改信息</span>
          </template>
          <el-form :model="nameForm" label-width="100px">
            <el-form-item label="登录名">
              <el-input v-model="nameForm.loginUserName" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="nameForm.nickName" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleNameSave" :loading="nameSaving">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProfile, updatePassword, updateName } from '../api/admin'

const router = useRouter()
const pwdSaving = ref(false)
const nameSaving = ref(false)

const pwdForm = reactive({
  originalPassword: '',
  newPassword: ''
})

const nameForm = reactive({
  loginUserName: '',
  nickName: ''
})

/** 从后端回显当前用户信息 */
const fetchProfile = async () => {
  try {
    const res = await getProfile()
    const d = res.data || {}
    nameForm.loginUserName = d.loginUserName || ''
    nameForm.nickName = d.nickName || ''
  } catch (e) {
    console.error(e)
  }
}

const handlePwdSave = async () => {
  if (!pwdForm.originalPassword || !pwdForm.newPassword) {
    return ElMessage.warning('请填写密码')
  }
  pwdSaving.value = true
  try {
    await updatePassword(pwdForm)
    ElMessage.success('密码修改成功，请重新登录')
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    router.push('/login')
  } catch (e) {
    console.error(e)
  } finally {
    pwdSaving.value = false
  }
}

const handleNameSave = async () => {
  if (!nameForm.loginUserName || !nameForm.nickName) {
    return ElMessage.warning('请填写完整信息')
  }
  nameSaving.value = true
  try {
    await updateName(nameForm)
    ElMessage.success('修改成功')
    localStorage.setItem('username', nameForm.nickName)
  } catch (e) {
    console.error(e)
  } finally {
    nameSaving.value = false
  }
}

onMounted(() => fetchProfile())
</script>
