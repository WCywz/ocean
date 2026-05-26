# 生产环境部署

## 服务器配置

- **云平台：** 阿里云轻量服务器
- **配置：** 4C8G，70 GB 磁盘
- **公网 IP：** （见 deploy/ 目录下的服务器配置）

## 部署架构

```
Nginx (80端口)
├── /api/* → Spring Boot (localhost:8080)
├── / → Vue 前端静态文件 (/www/wwwroot/ocean/dist/)
└── Python ML 推理脚本（按需调用）
```

## 组件

| 组件 | 说明 |
|------|------|
| Nginx | 反向代理 + 静态文件服务 |
| Spring Boot | Java 后端（`ocean-server-1.0.0.jar`） |
| MySQL | 数据库（`ocean_forecast` 库，含约 4100 万行观测数据） |
| Redis | 缓存（localhost:6379） |
| Python | PyTorch 模型推理 |

## 部署脚本

`deploy/update-20260524-v2.sh` — 当前使用的更新脚本：
- 解压更新包 → 停止旧服务 → 备份 → 复制新文件 → 启动服务
- 使用 `sudo` 执行关键操作
- 包含完整定时任务说明

## 定时任务（生产环境）

```
00:05 — 数据摄入
01:00 — 预报生成
02:00 — 健康评估
02:30 — 告警检查
```

## 更新归档

`deploy/update-20260524-v3.tar.gz` — 当前最新更新包（约 49 MB），包含：
- 前端构建产物（`dist/`）
- Spring Boot JAR
- Python 摄入脚本（`ingest_daily.py`、`ingest_raw_daily.py`）
- 预报运行脚本（`run_forecast.py`、`prepare_forecast_input.py`）

## 待完成项

- [ ] Python/PyTorch 环境配置（模型推理依赖）
- [ ] 系统服务自动启动（systemd）
- [ ] HTTPS 配置
- [ ] 默认密码修改

## 关键文件

- `deploy/application.yml` — 生产环境配置
- `deploy/ocean-server-1.0.0.jar` — 后端 JAR
- `deploy/dist/` — 前端构建产物
- `deploy/best_model_*.pt` — 模型权重文件
- `deploy/update-20260524-v2.sh` — 更新脚本
- `deploy/update-20260524-v3.tar.gz` — 更新归档
