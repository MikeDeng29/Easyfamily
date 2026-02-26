<template>
  <el-card>
    <template #header>
      <span>查询策略配置</span>
    </template>
    <el-form label-width="170px">
      <el-form-item label="dailyQuotaPerUser">
        <el-input-number v-model="dailyQuotaPerUser" :min="1" :max="1000" />
      </el-form-item>
      <el-form-item label="dailyQuotaPerPhone">
        <el-input-number v-model="dailyQuotaPerPhone" :min="1" :max="10000" />
      </el-form-item>
      <el-form-item label="dailyQuotaPerIp">
        <el-input-number v-model="dailyQuotaPerIp" :min="1" :max="50000" />
      </el-form-item>
      <el-form-item label="preferRedisCache">
        <el-switch v-model="preferRedisCache" />
      </el-form-item>
      <el-form-item label="providerKey">
        <el-select v-model="providerKey" style="width: 180px">
          <el-option label="aliyun-market" value="aliyun-market" />
          <el-option label="simulated" value="simulated" />
        </el-select>
      </el-form-item>
      <el-form-item label="providerTimeoutMs">
        <el-input-number v-model="providerTimeoutMs" :min="100" :max="10000" />
      </el-form-item>
      <el-form-item label="providerRetryTimes">
        <el-input-number v-model="providerRetryTimes" :min="0" :max="5" />
      </el-form-item>
      <el-form-item label="providerCircuitFailureThreshold">
        <el-input-number v-model="providerCircuitFailureThreshold" :min="1" :max="20" />
      </el-form-item>
      <el-form-item label="providerCircuitOpenSeconds">
        <el-input-number v-model="providerCircuitOpenSeconds" :min="1" :max="300" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="saveQuota">保存</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import http from "../../api/http";

const dailyQuotaPerUser = ref(10);
const dailyQuotaPerPhone = ref(30);
const dailyQuotaPerIp = ref(100);
const preferRedisCache = ref(true);
const providerKey = ref("aliyun-market");
const providerTimeoutMs = ref(1500);
const providerRetryTimes = ref(1);
const providerCircuitFailureThreshold = ref(5);
const providerCircuitOpenSeconds = ref(30);

const loadSettings = async () => {
  try {
    const resp = await http.get("/api/v1/admin/query-settings");
    const data = resp.data?.data ?? {};
    dailyQuotaPerUser.value = data.dailyQuotaPerUser ?? dailyQuotaPerUser.value;
    dailyQuotaPerPhone.value = data.dailyQuotaPerPhone ?? dailyQuotaPerPhone.value;
    dailyQuotaPerIp.value = data.dailyQuotaPerIp ?? dailyQuotaPerIp.value;
    preferRedisCache.value = data.preferRedisCache ?? preferRedisCache.value;
    providerKey.value = data.providerKey ?? providerKey.value;
    providerTimeoutMs.value = data.providerTimeoutMs ?? providerTimeoutMs.value;
    providerRetryTimes.value = data.providerRetryTimes ?? providerRetryTimes.value;
    providerCircuitFailureThreshold.value =
      data.providerCircuitFailureThreshold ?? providerCircuitFailureThreshold.value;
    providerCircuitOpenSeconds.value =
      data.providerCircuitOpenSeconds ?? providerCircuitOpenSeconds.value;
  } catch (e) {
    ElMessage.error("配置读取失败");
  }
};

const saveQuota = async () => {
  try {
    const resp = await http.patch("/api/v1/admin/quota", null, {
      params: {
        dailyQuotaPerUser: dailyQuotaPerUser.value,
        dailyQuotaPerPhone: dailyQuotaPerPhone.value,
        dailyQuotaPerIp: dailyQuotaPerIp.value,
        preferRedisCache: preferRedisCache.value,
        providerKey: providerKey.value,
        providerTimeoutMs: providerTimeoutMs.value,
        providerRetryTimes: providerRetryTimes.value,
        providerCircuitFailureThreshold: providerCircuitFailureThreshold.value,
        providerCircuitOpenSeconds: providerCircuitOpenSeconds.value
      }
    });
    const data = resp.data?.data ?? {};
    dailyQuotaPerUser.value = data.dailyQuotaPerUser ?? dailyQuotaPerUser.value;
    dailyQuotaPerPhone.value = data.dailyQuotaPerPhone ?? dailyQuotaPerPhone.value;
    dailyQuotaPerIp.value = data.dailyQuotaPerIp ?? dailyQuotaPerIp.value;
    preferRedisCache.value = data.preferRedisCache ?? preferRedisCache.value;
    providerKey.value = data.providerKey ?? providerKey.value;
    providerTimeoutMs.value = data.providerTimeoutMs ?? providerTimeoutMs.value;
    providerRetryTimes.value = data.providerRetryTimes ?? providerRetryTimes.value;
    providerCircuitFailureThreshold.value =
      data.providerCircuitFailureThreshold ?? providerCircuitFailureThreshold.value;
    providerCircuitOpenSeconds.value =
      data.providerCircuitOpenSeconds ?? providerCircuitOpenSeconds.value;
    ElMessage.success("配置已更新");
  } catch (e) {
    ElMessage.error("配置更新失败");
  }
};

loadSettings();
</script>
