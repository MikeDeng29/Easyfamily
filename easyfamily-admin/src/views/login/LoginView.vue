<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <span>管理端登录</span>
      </template>
      <el-form label-width="90px" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import http from "../../api/http";
import { saveTokens } from "../../auth/storage";

const router = useRouter();
const username = ref("admin");
const password = ref("Trump@666");
const submitting = ref(false);

const handleLogin = async () => {
  if (!username.value || !password.value) {
    ElMessage.warning("请输入用户名和密码");
    return;
  }
  submitting.value = true;
  try {
    const resp = await http.post("/api/v1/auth/admin/login", {
      username: username.value,
      password: password.value
    });
    if (resp.data?.code !== "OK") {
      throw new Error(resp.data?.message || "登录失败");
    }
    const data = resp.data?.data ?? {};
    if (!data.accessToken || !data.refreshToken) {
      throw new Error("missing token");
    }
    saveTokens(data.accessToken, data.refreshToken);
    ElMessage.success("登录成功");
    await router.push("/report");
  } catch (e) {
    const message = e?.response?.data?.message || e?.message || "登录失败，请检查账号或密码";
    ElMessage.error(message);
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.login-card {
  width: 420px;
}
</style>
