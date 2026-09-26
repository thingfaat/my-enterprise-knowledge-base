# Day 01 学习记录

日期：2026-09-26
课程：[Maven 与第一个 Spring Boot 接口](../lessons/day-01-Maven与第一个SpringBoot接口.md)
当天模块：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello`（已实现，无跨日模块依赖）。
目标：在当天独立模块亲手完成并运行最小 HTTP 接口，解释构建和启动的区别。
状态：用户已实现；本地代码与运行验收通过，Java 版本概念补充见下方评语。

## 已检查的事实

- 备课时尚无业务代码；用户随后已在提交 3844a0e 实现 Day 01 独立模块。
- 当前终端 Java：17.0.17；用户明确选择本地 JDK 17；本课 POM 编译目标为 17。
- `mvn` 未在 PATH 中；IDEA 自带 Maven 3.9.11 已验证可执行，当前使用 Java 17。
- 参考父 POM、文档模块 POM、DocumentApplication 和 DocumentController 已读取；版本与方法见教案。

## 我的记录（保留本人原答案）

1. 为什么有了 Java 还要 Maven：maven用于组织、编译、构建项目
2. 我认为构建与启动的区别：构建是将原生的java组织成可运行的class组合成的jar包，启动是运行这个jar包
3. JDK 17 与 Maven 使用的 Java 版本：可以在maven的组织文件pom.xml文件中指定java的版本
4. 我创建的文件与遇到的问题：没有问题
5. 构建与启动的实际结果：启动之后观察控制台应用运行在18082端口，访问`http://localhost:18082/api/hello`返回`Hello, day-01-spring-boot-hello`
6. 三条 HTTP 请求的状态码和正文：

```bash
curl -i http://localhost:18082/api/hello 返回结果是：Hello, day-01-spring-boot-hello
curl -i http://localhost:18082/api/not-exist 返回结果是：{"timestamp":"2026-09-26T14:29:52.633+00:00","status":404,"error":"Not Found","path":"/api/not-exist"}
curl -i -X POST http://localhost:18082/api/hello 返回结果是：{"timestamp":"2026-09-26T14:29:11.284+00:00","status":405,"error":"Method Not Allowed","path":"/api/hello"}
```

7. 我独立修改问候语后的结果：返回正常
8. 面试追问中仍不确定的题目：无

## 下一步

进入 Day 02，在独立 day-02-spring-di 模块学习构造器注入；保留 Day 01 源码不变。

## 验收评语（助手补充，保留上方本人原答案）

本地提交 3844a0e 的构建、实际 200/404/405 与停止后连接验证通过，见 [Day 01 验收报告](../reviews/day-01-验收报告.md)。

第 3 题补充：POM 的 java.version 配置编译目标，不会自动切换 Maven 实际运行的 JDK；实际版本应检查 Maven 的版本输出以及环境/IDE 配置。请在 Day 02 用自己的话复述区别，再记录掌握情况。
