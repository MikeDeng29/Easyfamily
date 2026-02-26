import { createRouter, createWebHistory } from "vue-router";
import ReportView from "../views/report/ReportView.vue";
import QuotaView from "../views/quota/QuotaView.vue";

const routes = [
  { path: "/", redirect: "/report" },
  { path: "/report", component: ReportView },
  { path: "/quota", component: QuotaView }
];

export default createRouter({
  history: createWebHistory(),
  routes
});
