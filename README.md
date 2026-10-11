# 数字图像处理 AI 助教系统（Digital Image Processing AI Tutor）

面向《数字图像处理》课程的智能助教系统，支持教师与学生双角色，覆盖课前预习、课中交互、课后作业、AI 答疑、互评答辩、学情分析等完整教学闭环。

## 技术栈

- **后端**：Spring Boot 3.2 + MyBatis-Plus + MySQL 8 + Redis + Sa-Token + WebSocket
- **前端**：Vue 3 + Vite + Element Plus + ECharts + Pinia
- **AI**：DeepSeek（OpenAI 兼容协议），支持 RAG 知识库问答、图片/文件分析、代码排错、函数查询

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 课次包 | 24 次课 + 4 个实验，支持教案/重难点/知识点/PPT/交互课件/作业 |
| 交互课件 | 直方图、均衡化、滤波、频域、形态学、分割、边缘等可视化实验室 |
| AI 助教 | RAG 问答、图片/文件分析、代码排错、函数查询（自动识别类型） |
| 作业系统 | 选择/填空/代码题，客观题自动判分，教师可打回重做、查看每题正确率与答案分布 |
| 学情看板 | 实时概览、按课次分析、词云、困惑点、易错点、异常提醒、课后报告 |
| 互评答辩 | 评价量规、互评活动、打分评论 |
| 埋点记录 | 登录/退出、材料浏览、PPT 页码、课件调参、AI 问答、作业提交等全量行为追踪 |

## 目录结构

```
DIP/
├── backend/            # Spring Boot 后端
│   ├── src/            # 源码
│   ├── sql/            # schema.sql 建表 + data.sql 初始数据 + 迁移脚本
│   └── .env.example    # 环境变量模板（复制为 .env 后填写）
├── frontpage/          # Vue3 前端
├── 启动系统.bat        # Windows 一键启动（Redis + 后端 + 前端）
└── 停止系统.bat        # 一键停止
```

## 快速开始

### 1. 环境准备

- JDK 17+
- Node.js 18+
- MySQL 8.0
- Redis 5.0+
- **LibreOffice**（用于 PPT/PPTX 在线预览，需单独安装，见下方说明）

### 2. 初始化数据库

```bash
mysql -uroot -p
CREATE DATABASE ai_tutor DEFAULT CHARACTER SET utf8mb4;
# 执行建表与初始数据
mysql -uroot -p ai_tutor < backend/sql/schema.sql
mysql -uroot -p ai_tutor < backend/sql/data.sql
```

### 3. 配置后端

```bash
cd backend
cp .env.example .env
# 编辑 .env，填入数据库密码、AI API Key 等
```

### 4. 启动

**Windows（一键）**：双击 `启动系统.bat`

**手动启动**：

```bash
# 后端
cd backend
mvn clean package
java -jar target/ai-tutor-backend-1.0.0.jar

# 前端
cd frontpage
npm install
npm run dev
```

访问 `http://localhost:5173`

## 默认账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 教师 | teacher | teacher123 |
| 学生 | 学号（如 20240001） | 学号 |

## 说明

- AI 相关功能需在「系统配置」中填入真实 DeepSeek API Key（.env 中为占位符）。
- 代码题作业因系统边界不自动判分，由教师评阅。
- **LibreOffice 依赖**：PPT/PPTX 在线预览依赖 LibreOffice（非 Maven/npm 依赖），
  需在部署机器上单独安装。后端通过 `soffice --headless` 命令将 PPT 转 PDF 再逐页渲染 PNG，
  安装路径通过 `.env` 的 `SOFFICE_PATH` 配置。
  - Windows：从 https://www.libreoffice.org 下载安装，或在 .env 配置完整路径（如 `C:\Program Files\LibreOffice\program\soffice.exe`）
  - Linux：`apt install libreoffice-impress` 或 `yum install libreoffice-impress`（默认路径 `soffice`）
  - 未安装时 PPT/PPTX 预览会失败，前端会引导用户下载原文件。
