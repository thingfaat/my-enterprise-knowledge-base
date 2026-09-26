# Day 01 验收报告

结论：本课代码与运行验收通过，可以进入 Day 02。学习记录有一个 Java 版本概念需要纠正，已说明，不是代码缺陷；不据此声称所有概念已完全掌握。

## 验收范围与基线

- 日期：2026-09-26；本地提交 `3844a0e24157bf3492fb5e7d0e4ca0eaab0b6f4f`。
- 验收对象：本地 `day-01-spring-boot-hello/`，未修改用户业务源码。
- GitHub main 曾只读核对为同一提交；构建、源码审阅和请求验收均在本地进行。
- 工具实测：Java 17.0.17，IDEA 自带 Maven 3.9.11，Maven 使用 Java 17。
- 构建：当天模块内执行 Maven `-B clean package`，退出码 0，BUILD SUCCESS，生成可执行 JAR。没有测试源码，不能把这项写成单元测试通过。
- 运行：用上述 JAR 启动，保留配置端口 18082，仅将监听地址限制为 127.0.0.1；测试完成后仅停止本次启动的进程。

## 源码审阅

- POM、两个 Java 文件和 application.yml 全部在当天独立模块内，符合目录规则。
- 启动类 `Day01SpringBootHelloApplication` 名称与文件一致，扫描覆盖 controller 子包；与教案命名不同不构成问题。
- Java 17、Boot 3.2.0、Web Starter 和 Boot 插件配置有效。
- GET 映射和纯文本返回正确，已将固定问候语改为自己的内容，并保留解释性注释。
- 无必须修复才能进入下一课的代码问题。

## 实際运行结果

| 请求/操作 | 实际结果 | 判定 |
|---|---|---|
| GET /api/hello | 200；Hello, day-01-spring-boot-hello；Content-Type 为 text/plain | 通过 |
| GET /api/not-exist | 404，错误正文 status=404 | 通过 |
| POST /api/hello | 405，错误正文 status=405 | 通过 |
| 停止本次应用后请求 | 连接失败 | 通过 |

错误响应原始正文（时间戳只用于本次证据）：

### GET /api/hello

```text
HTTP 200
Hello, day-01-spring-boot-hello
```

### GET /api/not-exist

```text
HTTP 404
{"timestamp":"2026-09-26T14:39:47.263+00:00","status":404,"error":"Not Found","path":"/api/not-exist"}
```

### POST /api/hello

```text
HTTP 405
{"timestamp":"2026-09-26T14:39:47.288+00:00","status":405,"error":"Method Not Allowed","path":"/api/hello"}
```

## 学习记录反馈

1. “Maven 用于组织、编译、构建项目”方向正确，还可补充依赖解析与管理。
2. 构建与启动的理解基本正确；构建也包含资源处理等步骤，运行还可以通过 IDE/main 或 Boot 插件，不限于 java -jar。
3. POM 的 java.version 在本课配置下影响编译目标，不会自动改变 Maven 实际使用的 JDK；实际运行 JDK 由环境/IDE 等决定，以 `mvn -version` 为证据。该概念已写入 Day 02 开头，建议本人复述后再把“掌握”填为完成。
4. 改问候语已由提交源码及本次 200 响应证实；记录里的 404/405 与本次复验一致。

用户原答案保留，评语单独记录，不伪造用户已修正的回答。业务实现完成、运行验收通过与概念掌握程度分开记录。
