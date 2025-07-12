<template>
  <div class="app-container">
    <div class="chat-card">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="chat-header-left">
          <div class="ai-avatar">
            <i class="bi bi-robot"></i>
          </div>
          <div class="chat-info">
            <h5 class="chat-name">AI 智能助手</h5>
            <span class="chat-status" :class="{ 'active': isConnected }">
              {{ isConnected ? '在线' : '连接中...' }}
            </span>
          </div>
        </div>
        <div class="chat-actions">
          <button class="action-btn" @click="clearMessages" title="清空对话">
            <i class="bi bi-trash"></i>
          </button>
          <button class="action-btn" @click="refreshConnection" title="刷新连接">
            <i class="bi bi-arrow-clockwise"></i>
          </button>
        </div>
      </div>
      
      <!-- 聊天内容区域 -->
      <div class="chat-container" ref="chatContainer">
        <transition-group name="fade" tag="div">
          <div class="welcome-section" v-if="messages.length === 1" key="welcome">
            <div class="welcome-header">
              <div class="welcome-icon">
                <i class="bi bi-stars"></i>
              </div>
              <h3>与AI助手对话</h3>
              <p>我可以回答问题、提供建议和解答疑问</p>
            </div>
            <div class="quick-prompts">
              <h6 class="prompt-title">可以尝试这些问题：</h6>
              <div class="prompt-list">
                <button 
                  v-for="(prompt, index) in suggestedPrompts" 
                  :key="index" 
                  class="prompt-btn"
                  @click="usePrompt(prompt)"
                >
                  {{ prompt }}
                </button>
              </div>
            </div>
          </div>
        
          <div 
            v-for="(message, index) in messages" 
            :key="'msg-' + index" 
            :class="['message-wrapper', message.isUser ? 'user-message' : 'ai-message']"
          >
            <div class="message-avatar">
              <div v-if="message.isUser" class="user-avatar">
                <i class="bi bi-person-fill"></i>
              </div>
              <div v-else class="ai-avatar">
                <i class="bi bi-robot"></i>
              </div>
            </div>
            <div class="message-bubble">
              <div class="message-content">
                <p v-html="formatMessageContent(message.content)"></p>
              </div>
              <div class="message-time">{{ formatTime(message.time || new Date()) }}</div>
            </div>
          </div>
        </transition-group>
        
        <div class="typing-indicator" v-show="isTyping">
          <div class="typing-bubble">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <div class="typing-text">AI正在思考...</div>
        </div>
      </div>
      
      <!-- 输入区域 -->
      <div class="input-container">
        <div class="input-wrapper">
          <textarea
            v-model="currentMessage"
            placeholder="输入您的问题或指令..."
            class="message-input"
            :disabled="isTyping"
            @keydown.enter.prevent="handleKeyDown"
            ref="inputField"
            rows="1"
          ></textarea>
          <button 
            class="send-button" 
            @click="sendMessage" 
            :disabled="!currentMessage.trim() || isTyping"
          >
            <i class="bi" :class="isTyping ? 'bi-hourglass-split' : 'bi-send-fill'"></i>
          </button>
        </div>
        <div class="input-footer">
          <span class="input-tip">按Enter键发送 • Shift+Enter换行</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'


// 状态
const chatContainer = ref(null)
const inputField = ref(null)
const messages = ref([
  {
    content: '您好！我是AI助手，很高兴为您服务。请问有什么可以帮您？',
    isUser: false,
    time: new Date()
  }
])
const currentMessage = ref('')
const isTyping = ref(false)
const isConnected = ref(true)

// 建议的问题提示
const suggestedPrompts = [
  "如何借用设备？",
  "这个系统有哪些功能？",
  "装备维护需要注意什么？",
  "如何处理借用过期的情况？"
]

// 自动调整文本域高度
const adjustTextareaHeight = () => {
  if (!inputField.value) return
  inputField.value.style.height = 'auto'
  inputField.value.style.height = `${inputField.value.scrollHeight}px`
}

watch(currentMessage, () => {
  nextTick(adjustTextareaHeight)
})

// 处理按键事件
const handleKeyDown = (e) => {
  if (e.shiftKey && e.key === 'Enter') {
    // 允许换行
    return
  } else if (e.key === 'Enter' && !isTyping.value) {
    sendMessage()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (chatContainer.value) {
    nextTick(() => {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    })
  }
}

// 发送消息
const sendMessage = async () => {
  const message = currentMessage.value.trim()
  if (!message || isTyping.value) return

  // 添加用户消息
  messages.value.push({
    content: message,
    isUser: true,
    time: new Date()
  })
  currentMessage.value = ''
  adjustTextareaHeight()
  scrollToBottom()

  // 显示输入指示器
  isTyping.value = true

  try {
    const response = await axios.post('/api/chat/send', { message }, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      timeout: 30000 // 30秒超时
    })
    
    // 添加AI响应
    if (response.data && response.data.message) {
      setTimeout(() => {
        messages.value.push({
          content: response.data.message,
          isUser: false,
          time: new Date()
        })
        isTyping.value = false
        scrollToBottom()
      }, 500) // 添加一个短暂延迟，让打字动画更自然
    } else {
      throw new Error('无效的响应格式')
    }
  } catch (error) {
    let errorMessage = '发送消息失败'
    if (error.response) {
      // 服务器响应错误
      errorMessage = error.response.data?.message || '服务器错误'
    } else if (error.request) {
      // 请求超时或网络错误
      errorMessage = '网络连接失败，请检查您的网络连接'
    }
    
    ElMessage.error(errorMessage)
    messages.value.push({
      content: `抱歉，发生了错误：${errorMessage}`,
      isUser: false,
      time: new Date()
    })
    isTyping.value = false
    scrollToBottom()
  }
}

// 使用预设问题
const usePrompt = (prompt) => {
  currentMessage.value = prompt
  nextTick(() => {
    if (inputField.value) inputField.value.focus()
  })
}

// 清空对话
const clearMessages = () => {
  messages.value = [
    {
      content: '您好！我是AI助手，很高兴为您服务。请问有什么可以帮您？',
      isUser: false,
      time: new Date()
    }
  ]
}

// 刷新连接
const refreshConnection = () => {
  isConnected.value = false
  setTimeout(() => {
    isConnected.value = true
    ElMessage.success('连接已刷新')
  }, 1000)
}

// 格式化消息内容，处理换行和链接
const formatMessageContent = (content) => {
  if (!content) return ''
  
  // 处理URL转为链接
  const urlRegex = /(https?:\/\/[^\s]+)/g
  const withLinks = content.replace(urlRegex, url => {
    return `<a href="${url}" target="_blank" class="message-link">${url}</a>`
  })
  
  // 处理换行
  return withLinks.replace(/\n/g, '<br>')
}

// 格式化时间
const formatTime = (date) => {
  if (!date) return ''
  
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

// 初始化
onMounted(() => {
  scrollToBottom()
  if (inputField.value) inputField.value.focus()
})
</script>

<style lang="scss" scoped>
// 导入变量
@import "../../styles/variables.scss";

.app-container {
  padding: 20px;
  height: 100%;
  display: flex;
  justify-content: center;
  background: linear-gradient(135deg, rgba(240, 244, 248, 0.8) 0%, rgba(237, 240, 249, 0.9) 100%);
}

.chat-card {
  height: calc(100vh - 120px);
  width: 100%;
  max-width: 900px;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s ease;
  animation: slideUp 0.5s ease-out forwards;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 聊天头部
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  background: linear-gradient(to right, rgba($--color-primary, 0.02) 0%, rgba($--color-primary, 0.05) 100%);
  border-bottom: 1px solid $--border-color-lighter;
  
  .chat-header-left {
    display: flex;
    align-items: center;
  }
  
  .chat-info {
    margin-left: 14px;
  }
  
  .chat-name {
    font-size: 18px;
    font-weight: 600;
    margin: 0;
    color: $--color-text-primary;
    letter-spacing: -0.3px;
  }
  
  .chat-status {
    font-size: 13px;
    color: $--color-text-secondary;
    
    &.active {
      color: $--color-success;
      
      &::before {
        content: "";
        display: inline-block;
        width: 8px;
        height: 8px;
        background-color: $--color-success;
        border-radius: 50%;
        margin-right: 6px;
        animation: pulse 2s infinite;
      }
    }
  }
  
  .chat-actions {
    display: flex;
    gap: 10px;
  }
  
  .action-btn {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: transparent;
    border: none;
    border-radius: 50%;
    color: $--color-text-secondary;
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover {
      background-color: rgba($--color-primary, 0.1);
      color: $--color-primary;
      transform: translateY(-2px);
    }
    
    i {
      font-size: 16px;
    }
  }
}

@keyframes pulse {
  0% { 
    box-shadow: 0 0 0 0 rgba($--color-success, 0.6);
    opacity: 0.8;
  }
  70% { 
    box-shadow: 0 0 0 6px rgba($--color-success, 0);
    opacity: 1;
  }
  100% { 
    box-shadow: 0 0 0 0 rgba($--color-success, 0);
    opacity: 0.8;
  }
}

// 聊天容器
.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23e2e8f0' fill-opacity='0.4'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.1);
    border-radius: 3px;
    transition: background-color 0.3s;
  }
  
  &::-webkit-scrollbar-thumb:hover {
    background-color: rgba(0, 0, 0, 0.2);
  }
  
  &::-webkit-scrollbar-track {
    background-color: transparent;
  }
}

// 欢迎部分
.welcome-section {
  margin-bottom: 30px;
  text-align: center;
  padding: 30px 20px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.2);
  animation: fadeIn 0.8s ease-out;
  
  .welcome-header {
    margin-bottom: 20px;
  }
  
  .welcome-icon {
    width: 70px;
    height: 70px;
    margin: 0 auto 20px;
    background: linear-gradient(135deg, rgba($--color-primary, 0.2) 0%, rgba($--color-primary-light, 0.2) 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    z-index: 1;
    box-shadow: 0 6px 16px -8px rgba($--color-primary, 0.4);
    
    &::before {
      content: '';
      position: absolute;
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: linear-gradient(135deg, rgba($--color-primary, 0.1) 0%, rgba($--color-primary-light, 0.1) 100%);
      animation: pulse-ring 2s infinite;
      z-index: -1;
    }
    
    i {
      font-size: 28px;
      color: $--color-primary;
    }
  }
  
  h3 {
    font-size: 22px;
    font-weight: 700;
    margin-bottom: 10px;
    background: linear-gradient(135deg, $--color-primary 0%, $--color-primary-light 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    text-fill-color: transparent;
  }
  
  p {
    color: $--color-text-secondary;
    margin-bottom: 24px;
    font-size: 15px;
  }
  
  .prompt-title {
    font-size: 15px;
    font-weight: 500;
    color: $--color-text-secondary;
    margin-bottom: 14px;
  }
  
  .prompt-list {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 12px;
  }
  
  .prompt-btn {
    background-color: #fff;
    border: 1px solid $--border-color-base;
    border-radius: 24px;
    color: $--color-text-regular;
    padding: 10px 18px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s;
    max-width: 320px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
    
    &:hover {
      border-color: $--color-primary;
      color: $--color-primary;
      background-color: rgba($--color-primary, 0.05);
      transform: translateY(-3px);
      box-shadow: 0 6px 12px rgba(0, 0, 0, 0.06);
    }
    
    &:active {
      transform: translateY(-1px);
    }
  }
}

@keyframes pulse-ring {
  0% {
    transform: scale(0.8);
    opacity: 0.8;
  }
  70% {
    transform: scale(1.1);
    opacity: 0.2;
  }
  100% {
    transform: scale(0.8);
    opacity: 0.8;
  }
}

// 消息样式
.message-wrapper {
  display: flex;
  margin-bottom: 22px;
  animation: fadeIn 0.5s ease-out;
  
  &.user-message {
    flex-direction: row-reverse;
    
    .message-bubble {
      margin-left: 0;
      margin-right: 14px;
    }
    
    .message-content {
      background: linear-gradient(135deg, $--color-primary 0%, $--color-primary-dark 100%);
      color: #fff;
      border-radius: 18px 4px 18px 18px;
      box-shadow: 0 4px 12px rgba($--color-primary, 0.2);
      
      a.message-link {
        color: #fff;
        text-decoration: underline;
      }
    }
    
    .message-time {
      text-align: right;
    }
  }
  
  &.ai-message {
    .message-content {
      background: #fff;
      color: $--color-text-primary;
      border-radius: 4px 18px 18px 18px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }
  }
}

// 头像
.message-avatar {
  flex-shrink: 0;
  
  .user-avatar,
  .ai-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    transition: transform 0.3s;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  }
  
  .user-avatar {
    background: linear-gradient(135deg, $--color-primary-light 0%, $--color-primary-dark 100%);
    
    i {
      font-size: 18px;
    }
  }
  
  .ai-avatar {
    background: linear-gradient(135deg, $--color-info 0%, $--color-secondary 100%);
    
    i {
      font-size: 18px;
    }
  }
}

.message-bubble {
  max-width: 70%;
  margin-left: 14px;
  transition: transform 0.2s;
  
  &:hover {
    transform: translateY(-2px);
  }
}

.message-content {
  padding: 14px 18px;
  border-radius: 18px;
  font-size: 15px;
  line-height: 1.6;
  transition: box-shadow 0.3s;
  
  &:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  }
  
  p {
    margin: 0;
    word-wrap: break-word;
  }
}

.message-time {
  font-size: 11px;
  color: $--color-text-secondary;
  margin-top: 6px;
  opacity: 0.8;
  padding: 0 8px;
}

// 输入组件
.input-container {
  padding: 20px;
  background-color: #fff;
  border-top: 1px solid $--border-color-lighter;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  background-color: $--background-color-base;
  border-radius: 24px;
  padding: 10px 10px 10px 20px;
  transition: all 0.3s;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  
  &:focus-within {
    box-shadow: 0 0 0 2px rgba($--color-primary, 0.2), 0 6px 16px rgba(0, 0, 0, 0.06);
    background-color: #fff;
  }
}

.message-input {
  flex: 1;
  resize: none;
  border: none;
  background: transparent;
  padding: 10px 0;
  font-size: 15px;
  max-height: 120px;
  outline: none;
  color: $--color-text-primary;
  font-family: inherit;
  line-height: 1.5;
  
  &::placeholder {
    color: $--color-text-placeholder;
  }
}

.send-button {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, $--color-primary 0%, $--color-primary-dark 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
  margin-left: 10px;
  box-shadow: 0 4px 12px rgba($--color-primary, 0.3);
  
  &:hover {
    background: linear-gradient(135deg, $--color-primary-dark 0%, darken($--color-primary-dark, 5%) 100%);
    transform: translateY(-3px);
    box-shadow: 0 6px 16px rgba($--color-primary, 0.4);
  }
  
  &:disabled {
    background: linear-gradient(135deg, #ccc 0%, #aaa 100%);
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
  }
  
  i {
    font-size: 18px;
  }
}

.input-footer {
  margin-top: 10px;
  padding: 0 14px;
  
  .input-tip {
    font-size: 12px;
    color: $--color-text-secondary;
  }
}

// 输入指示器
.typing-indicator {
  display: flex;
  align-items: center;
  margin: 12px 0 12px 54px;
  animation: fadeIn 0.5s;
}

.typing-bubble {
  display: flex;
  align-items: center;
  background-color: #fff;
  padding: 10px 14px;
  border-radius: 18px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-right: 8px;
  
  span {
    width: 8px;
    height: 8px;
    margin: 0 2px;
    background: linear-gradient(135deg, $--color-primary 0%, $--color-primary-light 100%);
    border-radius: 50%;
    display: inline-block;
    animation: typing 1s infinite ease-in-out;
    
    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

.typing-text {
  font-size: 13px;
  color: $--color-text-secondary;
}

@keyframes typing {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s, transform 0.5s;
}
.fade-enter, .fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@media (max-width: 768px) {
  .app-container {
    padding: 10px;
  }
  
  .chat-card {
    height: calc(100vh - 80px);
    border-radius: 12px;
  }
  
  .message-bubble {
    max-width: 85%;
  }
  
  .welcome-section {
    padding: 20px 10px;
  }
  
  .chat-header {
    padding: 14px 16px;
    
    .chat-name {
      font-size: 16px;
    }
  }
  
  .chat-container {
    padding: 16px;
  }
  
  .input-container {
    padding: 14px;
  }
  
  .welcome-icon {
    width: 60px;
    height: 60px;
    
    i {
      font-size: 24px;
    }
  }
  
  .prompt-btn {
    padding: 8px 14px;
    font-size: 13px;
  }
}
</style> 