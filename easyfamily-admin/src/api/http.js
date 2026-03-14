import axios from "axios";
import { getAccessToken, clearTokens } from "../auth/storage";
import router from "../router";

const http = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 8000
});

http.interceptors.request.use((config) => {
  const accessToken = getAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      clearTokens();
      if (router.currentRoute.value.path !== "/login") {
        router.push("/login");
      }
    }
    return Promise.reject(error);
  }
);

export default http;
