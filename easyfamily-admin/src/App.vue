<template>
  <el-container style="min-height: 100vh">
    <el-header>
      <el-menu mode="horizontal" :default-active="activePath" router>
        <el-menu-item index="/report">报表</el-menu-item>
        <el-menu-item index="/quota">查询限额配置</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const activePath = computed(() => route.path);
</script>
