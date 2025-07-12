const fs = require('fs');
const path = require('path');

// 要处理的目录
const targetDir = path.join(__dirname, 'ui', 'src');

// 匹配console.log的正则表达式
const consoleLogRegex = /^\s*console\.log\(.+?\);?\s*$/gm;

// 递归处理目录
function processDirectory(directory) {
  const files = fs.readdirSync(directory);
  
  files.forEach(file => {
    const filePath = path.join(directory, file);
    const stats = fs.statSync(filePath);
    
    if (stats.isDirectory()) {
      processDirectory(filePath);
    } else if (stats.isFile() && (file.endsWith('.js') || file.endsWith('.vue'))) {
      processFile(filePath);
    }
  });
}

// 处理单个文件
function processFile(filePath) {
  try {
    let content = fs.readFileSync(filePath, 'utf8');
    const originalSize = content.length;
    
    // 删除console.log语句
    content = content.replace(consoleLogRegex, '');
    
    // 如果文件内容有变化，则写回文件
    if (content.length !== originalSize) {
      fs.writeFileSync(filePath, content, 'utf8');
      console.log(`已清理 ${filePath}`);
    }
  } catch (error) {
    console.error(`处理文件 ${filePath} 时出错:`, error);
  }
}

console.log('开始清理console.log语句...');
processDirectory(targetDir);
console.log('清理完成!'); 