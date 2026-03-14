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
      <el-descriptions-item label="今日登录用户数">{{ overview.loginUserCount }}</el-descriptions-item>
      <el-descriptions-item label="当前日限额">{{ overview.dailyQuotaPerUser }}</el-descriptions-item>
    </el-descriptions>
    <el-divider />
    <el-card shadow="never">
      <template #header>
        <span>近7天登录用户数</span>
      </template>
      <div ref="loginChartRef" class="login-chart" />
    </el-card>
  </el-card>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import * as echarts from "echarts";
import http from "../../api/http";

const dauData = ref([]);
const featureData = ref([]);
const overview = ref({
  totalQueryCount: 0,
  cacheHitCount: 0,
  cacheHitRate: "0%",
  loginUserCount: 0,
  dailyQuotaPerUser: 0
});
const loginChartRef = ref(null);
let loginChartInstance = null;

const renderLoginChart = async (rows) => {
  await nextTick();
  if (!loginChartRef.value) {
    return;
  }
  if (!loginChartInstance) {
    loginChartInstance = echarts.init(loginChartRef.value);
  }
  loginChartInstance.setOption({
    tooltip: {
      trigger: "axis"
    },
    xAxis: {
      type: "category",
      data: rows.map((item) => item.date)
    },
    yAxis: {
      type: "value",
      minInterval: 1
    },
    series: [
      {
        name: "登录用户数",
        type: "bar",
        data: rows.map((item) => item.loginUserCount),
        barMaxWidth: 40
      }
    ]
  });
};

onMounted(async () => {
  try {
    const [dauResp, hotResp, overviewResp, loginWeeklyResp] = await Promise.all([
      http.get("/api/v1/admin/reports/dau"),
      http.get("/api/v1/admin/reports/feature-hot"),
      http.get("/api/v1/admin/reports/query-overview"),
      http.get("/api/v1/admin/reports/login-users-weekly")
    ]);
    dauData.value = dauResp.data.data ?? [];
    featureData.value = hotResp.data.data ?? [];
    const overviewRaw = overviewResp.data.data ?? {};
    overview.value = {
      totalQueryCount: overviewRaw.totalQueryCount ?? 0,
      cacheHitCount: overviewRaw.cacheHitCount ?? 0,
      cacheHitRate: `${Math.round((overviewRaw.cacheHitRate ?? 0) * 100)}%`,
      loginUserCount: overviewRaw.loginUserCount ?? 0,
      dailyQuotaPerUser: overviewRaw.dailyQuotaPerUser ?? 0
    };
    await renderLoginChart(loginWeeklyResp.data.data ?? []);
  } catch (e) {
    ElMessage.error("报表加载失败");
  }
});

onBeforeUnmount(() => {
  if (loginChartInstance) {
    loginChartInstance.dispose();
    loginChartInstance = null;
  }
});
</script>

<style scoped>
.login-chart {
  width: 100%;
  height: 320px;
}
</style>
