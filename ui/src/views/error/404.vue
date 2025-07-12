<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = () => {
  router.push('/');
};

const goBack = () => {
  router.go(-1);
};
</script>

<template>
  <div class="error-page">
    <div class="container">
      <div class="error-content text-center">
        <h1 class="error-code">404</h1>
        <h2 class="error-title">页面未找到</h2>
        <p class="error-message">
          您访问的页面不存在或已被移动到其他位置。
        </p>
        <div class="error-actions mt-4">
          <button class="btn btn-primary me-3" @click="goHome">
            <i class="bi bi-house-door me-2"></i>返回首页
          </button>
          <button class="btn btn-outline-secondary" @click="goBack">
            <i class="bi bi-arrow-left me-2"></i>返回上一页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
}

.error-content {
  padding: 2rem;
  max-width: 500px;
  margin: 0 auto;
}

.error-code {
  font-size: 8rem;
  font-weight: 700;
  color: #0d6efd;
  line-height: 1;
  margin-bottom: 1rem;
  text-shadow: 4px 4px 0 rgba(13, 110, 253, 0.1);
}

.error-title {
  font-size: 2rem;
  font-weight: 600;
  margin-bottom: 1rem;
}

.error-message {
  color: #6c757d;
  font-size: 1.1rem;
  margin-bottom: 2rem;
}

.error-actions .btn {
  padding: 0.6rem 1.5rem;
  font-weight: 500;
}
</style> 