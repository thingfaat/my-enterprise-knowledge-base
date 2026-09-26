# Day 04 学习记录

课程：[HTTP 参数绑定与 Spring MVC](../lessons/day-04-HTTP参数绑定与SpringMVC.md)
模块：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc`
状态：用户报告完成，主体源码已核对；发现下述待修/待补事项，本次未运行验收。

## 本人填写

- 实际 Java/Maven 版本和构建结果：
- 三种参数注解的区别：
- 创建请求的状态、Location、JSON：
- 详情与搜索的实际结果：
- 空条件、无匹配、缺正文的实际结果：
- 400/404/405/415 的请求与结果：
- 断点：哪种 400 进入了 Controller，哪种没有：
- 重启前后的数据差异：
- 独立增加 contentKeyword 的文件、规则与三条验证：
- 创建与详情的数据流图：
- 未解决问题：

## 下次进入

读取计划与进度，核对本课代码和实际结果，验收后继续 Day 05 配置与日志。

## 2026-09-27 源码核对反馈

- DocumentController.create：`URI.create("/api/documents" + document.id())` 缺少分隔斜杠；应为 `URI.create("/api/documents/" + document.id())`。请修正后创建文档，并按 Location 访问验证 200。
- DocumentService.create：正文使用 strip，会移除首尾空白，与教案保留正文排版不同。请明确策略；若遵循原要求，保留原 content，并用带空白正文验证。
- contentKeyword 独立练习未在本地代码中出现；上述本人填写区域仍为空，保留原内容等待实际证据。
- 本次没有修改业务源码、没有构建/运行，不把源码存在视为全部验收通过。
