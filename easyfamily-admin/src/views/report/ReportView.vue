<template>
  <el-card>
    <template #header>
      <span>运营报表（MVP）</span>
    </template>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-table :data="dauData" style="width: 100%">
          <el-table-column prop="date" label="日期" />
          <el-table-column prop="dau" label="DAU" />
        </el-table>
      </el-col>
      <el-col :span="12">
        <el-table :data="featureData" style="width: 100%">
          <el-table-column prop="feature" label="功能点" />
          <el-table-column prop="pv" label="访问量" />
        </el-table>
      </el-col>
    </el-row>
    <el-divider />
    <el-descriptions title="查询概览" :column="2" border>
      <el-descriptions-item label="总查询次数">{{ overview.totalQueryCount }}</el-descriptions-item>
      <el-descriptions-item label="缓存命中次数">{{ overview.cacheHitCount }}</el-descriptions-item>
      <el-descriptions-item label="缓存命中率">{{ overview.cacheHitRate }}</el-descriptions-item>
      <el-descriptions-item label="当前日限额">{{ overview.dailyQuotaPerUser }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import http from "../../api/http";

const dauData = ref([]);
const featureData = ref([]);
const overview = ref({
  totalQueryCount: 0,
  cacheHitCount: 0,
  cacheHitRate: "0%",
  dailyQuotaPerUser: 0
});

onMounted(async () => {
  try {
    const [dauResp, hotResp, overviewResp] = await Promise.all([
      http.get("/api/v1/admin/reports/dau"),
      http.get("/api/v1/admin/reports/feature-hot"),
      http.get("/api/v1/admin/reports/query-overview")
    ]);
    dauData.value = dauResp.data.data ?? [];
    featureData.value = hotResp.data.data ?? [];
    const overviewRaw = overviewResp.data.data ?? {};
    overview.value = {
      totalQueryCount: overviewRaw.totalQueryCount ?? 0,
      cacheHitCount: overviewRaw.cacheHitCount ?? 0,
      cacheHitRate: `${Math.round((overviewRaw.cacheHitRate ?? 0) * 100)}%`,
      dailyQuotaPerUser: overviewRaw.dailyQuotaPerUser ?? 0
    };
  } catch (e) {
    ElMessage.error("报表加载失败");
  }
});
</script>
