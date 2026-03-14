<template>
  <el-container style="min-height: 100vh">
    <el-header v-if="showLayoutHeader">
      <div class="header-row">
        <el-menu mode="horizontal" :default-active="activePath" :ellipsis="false" router>
          <el-menu-item index="/report">报表</el-menu-item>
          <el-menu-item index="/quota">查询限额配置</el-menu-item>
        </el-menu>
        <el-button type="danger" plain @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { clearTokens } from "./auth/storage";

const route = useRoute();
const router = useRouter();
const activePath = computed(() => route.path);
const showLayoutHeader = computed(() => route.path !== "/login");

const logout = async () => {
  clearTokens();
  await router.push("/login");
};
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-row :deep(.el-menu) {
  flex: 1;
  min-width: 0;
}

.header-row :deep(.el-menu-item) {
  white-space: nowrap;
}
</style>
