<template>
  <el-card>
    <template #header>
      <span>反馈管理</span>
    </template>

    <!-- 筛选栏 -->
    <el-row :gutter="12" style="margin-bottom: 16px">
      <el-col :span="6">
        <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 100%" @change="handleFilterChange">
          <el-option label="全部" value="" />
          <el-option label="待处理" value="pending" />
          <el-option label="已回复" value="replied" />
        </el-select>
      </el-col>
      <el-col :span="6">
        <el-select v-model="pageSize" placeholder="每页条数" style="width: 100%" @change="handlePageSizeChange">
          <el-option label="10 条/页" :value="10" />
          <el-option label="20 条/页" :value="20" />
          <el-option label="50 条/页" :value="50" />
        </el-select>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="feedbackList"
      style="width: 100%"
      row-key="id"
    >
      <!-- 展开行：显示详细描述和回复内容 -->
      <el-table-column type="expand">
        <template #default="{ row }">
          <div style="padding: 12px 20px">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="问题描述">
                {{ row.description }}
              </el-descriptions-item>
              <el-descriptions-item label="联系邮箱">
                {{ row.email || '未填写' }}
              </el-descriptions-item>
              <el-descriptions-item v-if="row.reply" label="管理员回复">
                {{ row.reply }}
              </el-descriptions-item>
              <el-descriptions-item v-if="row.repliedAt" label="回复时间">
                {{ row.repliedAt }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column prop="phone" label="用户手机" width="150" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'pending' ? 'warning' : 'success'">
            {{ row.status === 'pending' ? '待处理' : '已回复' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="联系邮箱" min-width="180">
        <template #default="{ row }">
          <span v-if="row.email">{{ row.email }}</span>
          <el-tag v-else type="info">未填写</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'pending'"
            type="primary"
            size="small"
            @click="openReplyDialog(row)"
          >
            回复
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 错误提示 -->
    <el-alert
      v-if="loadError"
      title="加载失败，请刷新页面重试"
      type="error"
      show-icon
      style="margin-top: 16px"
    />

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 16px; justify-content: flex-end; display: flex"
      @current-change="fetchFeedback"
      @size-change="handlePageSizeChange"
    />

    <!-- 回复对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="回复用户反馈"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetDialog"
    >
      <el-descriptions :column="1" border style="margin-bottom: 16px">
        <el-descriptions-item label="用户手机">{{ activeRow?.phone }}</el-descriptions-item>
        <el-descriptions-item label="反馈标题">{{ activeRow?.title }}</el-descriptions-item>
        <el-descriptions-item label="问题描述">{{ activeRow?.description }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-bottom: 10px">
        <el-text v-if="activeRow?.email" type="info" size="small">回复将发送到：{{ activeRow.email }}</el-text>
        <el-text v-else type="warning" size="small">用户未填写邮箱，回复不会发送邮件通知</el-text>
      </div>

      <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-width="0">
        <el-form-item prop="reply">
          <el-input
            v-model="replyForm.reply"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请输入回复内容，回复后将通过短信通知用户"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReply">确认回复</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import http from "../../api/http";

const feedbackList = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const filterStatus = ref("");
const loading = ref(false);
const loadError = ref(false);

const dialogVisible = ref(false);
const submitting = ref(false);
const activeRow = ref(null);
const replyFormRef = ref(null);
const replyForm = ref({ reply: "" });

const replyRules = {
  reply: [{ required: true, message: "回复内容不能为空", trigger: "blur" }]
};

const fetchFeedback = async () => {
  loading.value = true;
  loadError.value = false;
  try {
    const resp = await http.get("/api/v1/admin/feedback", {
      params: {
        status: filterStatus.value || undefined,
        page: currentPage.value - 1,
        size: pageSize.value
      }
    });
    const data = resp.data?.data ?? {};
    feedbackList.value = data.items ?? [];
    total.value = data.total ?? 0;
  } catch {
    loadError.value = true;
    ElMessage.error("反馈列表加载失败");
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => {
  currentPage.value = 1;
  fetchFeedback();
};

const handlePageSizeChange = () => {
  currentPage.value = 1;
  fetchFeedback();
};

const openReplyDialog = (row) => {
  activeRow.value = row;
  replyForm.value.reply = "";
  dialogVisible.value = true;
};

const resetDialog = () => {
  activeRow.value = null;
  replyForm.value.reply = "";
};

const submitReply = async () => {
  const valid = await replyFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    await http.post(`/api/v1/admin/feedback/${activeRow.value.id}/reply`, {
      reply: replyForm.value.reply
    });
    ElMessage.success("回复成功，已通过短信通知用户");
    dialogVisible.value = false;
    fetchFeedback();
  } catch {
    ElMessage.error("回复发送失败，请重试");
  } finally {
    submitting.value = false;
  }
};

fetchFeedback();
</script>
