import { createRouter, createWebHistory } from "vue-router";
import ReportView from "../views/report/ReportView.vue";
import QuotaView from "../views/quota/QuotaView.vue";
import LoginView from "../views/login/LoginView.vue";
import FeedbackView from "../views/feedback/FeedbackView.vue";
import { isLoggedIn } from "../auth/storage";

const routes = [
  { path: "/", redirect: "/report" },
  { path: "/login", component: LoginView },
  { path: "/report", component: ReportView, meta: { requiresAuth: true } },
  { path: "/quota", component: QuotaView, meta: { requiresAuth: true } },
  { path: "/feedback", component: FeedbackView, meta: { requiresAuth: true } }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  if (to.path === "/login" && isLoggedIn()) {
    return "/report";
  }
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return "/login";
  }
  return true;
});

export default router;
