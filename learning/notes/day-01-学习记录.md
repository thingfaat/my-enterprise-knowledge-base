# Day 01 学习记录

日期：2026-09-26
课程：[Maven 与第一个 Spring Boot 接口](../lessons/day-01-Maven与第一个SpringBoot接口.md)
当天模块：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello`（待用户创建，无跨日模块依赖）。
目标：在当天独立模块亲手完成并运行最小 HTTP 接口，解释构建和启动的区别。
状态：教学已开始；教案已编写；业务代码待本人实现，尚未验收。

## 已检查的事实

- 当前工程无 POM/Java 源码，尚未代写业务代码。
- 当前终端 Java：17.0.17；用户明确选择本地 JDK 17；本课 POM 编译目标为 17。
- `mvn` 未在 PATH 中；IDEA 自带 Maven 3.9.11 已验证可执行，当前使用 Java 17。
- 参考父 POM、文档模块 POM、DocumentApplication 和 DocumentController 已读取；版本与方法见教案。

## 我的记录（待本人填写）

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

终端 JDK 17 与 Maven 已确认可用，阅读教案原理，在 day-01-spring-boot-hello 中自己创建四个文件；再提供构建与请求结果进行验收。不要将预期输出直接填写为实际结果。
