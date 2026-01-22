# IDEA 2025.3 项目快速修复指南

## 问题描述
在IDEA 2025.3中导入项目后出现"找不到类"等报错。

## 快速修复步骤(3分钟)

### 1. 配置JDK 17
```
File -> Project Structure -> Project
- SDK: 选择或添加 JDK 17
- Language level: 17
```

### 2. 刷新Maven
```
右侧 Maven 窗口 -> 点击刷新图标 (Reload All Maven Projects)
```
如果Maven窗口为空,右键点击 `pom.xml` -> `Add as Maven Project`

### 3. 安装并启用Lombok插件
```
File -> Settings -> Plugins -> 搜索 "Lombok" -> Install
重启IDEA
File -> Settings -> Build, Execution, Deployment ->
  Compiler -> Annotation Processors -> 勾选 "Enable annotation processing"
```

### 4. 清理并重建项目
```bash
# Windows系统,双击运行
fix-idea-errors.bat

# 或手动执行:
mvnw.cmd clean
mvnw.cmd compile
mvnw.cmd package -DskipTests
```

### 5. 在IDEA中重建
```
Build -> Rebuild Project
File -> Invalidate Caches -> Invalidate and Restart
```

---

## 详细配置说明
请查看 `IDEA_CONFIG_GUIDE.md` 获取完整的配置说明和问题排查步骤。

---

## 关键信息

### 项目技术栈
- Spring Boot: 3.4.3
- Java: 17 (必须!)
- Maven: 3.x
- 主要依赖:
  - Spring Boot Web, JPA, Thymeleaf
  - MySQL Connector
  - Lombok
  - MapStruct
  - FastJSON

### 常见报错及解决方案

| 报错类型 | 可能原因 | 解决方案 |
|---------|---------|---------|
| 找不到Spring Boot类 | Maven依赖未下载 | 刷新Maven |
| 找不到Lombok注解生成的代码 | Lombok插件未安装/未启用 | 安装插件+启用注解处理 |
| 找不到MapStruct生成的代码 | 注解处理器未配置 | pom.xml已优化,刷新Maven |
| JDK版本错误 | 使用了错误的JDK版本 | 配置JDK 17 |
| 依赖下载失败 | Maven仓库连接问题 | 配置阿里云镜像 |

---

## 配置Maven镜像(可选)

如果依赖下载缓慢,配置阿里云镜像:

**Windows**:
```bash
# 复制 settings.xml.example 到 Maven 配置目录
# 通常位于: C:\Users\你的用户名\.m2\settings.xml
```

或手动编辑 `~/.m2/settings.xml`:
```xml
<mirrors>
  <mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

---

## 验证修复成功

### 1. 检查依赖
在IDEA中打开 `External Libraries`,应该看到:
- spring-boot-starter-web-3.4.3.jar
- spring-boot-starter-data-jpa-3.4.3.jar
- spring-boot-starter-thymeleaf-3.4.3.jar
- mysql-connector-j-x.x.x.jar
- lombok-1.18.30.jar
- mapstruct-1.5.3.Final.jar
- fastjson-2.0.34.jar
- httpclient-4.5.13.jar

### 2. 运行测试
```bash
mvnw.cmd test
```

### 3. 启动应用
在IDEA中运行 `StockApplication.java` 的 `main` 方法

### 4. 检查错误提示
编辑器中应该没有红色错误提示

---

## 快速命令参考

```bash
# 清理
mvnw.cmd clean

# 编译
mvnw.cmd compile

# 测试
mvnw.cmd test

# 打包(跳过测试)
mvnw.cmd package -DskipTests

# 运行应用
mvnw.cmd spring-boot:run

# 查看依赖树
mvnw.cmd dependency:tree
```

---

## 文件说明

- `IDEA_CONFIG_GUIDE.md` - 详细配置指南
- `IDEA_QUICK_FIX.md` - 本文件,快速修复指南
- `settings.xml.example` - Maven镜像配置示例
- `fix-idea-errors.bat` - Windows自动修复脚本
- `pom.xml` - Maven配置文件(已优化)

---

## 修改说明

本次修复对 `pom.xml` 进行了以下优化:

1. 在 `maven-compiler-plugin` 中添加了 `annotationProcessorPaths` 配置
2. 明确指定了Lombok和MapStruct的注解处理器路径
3. 添加了MapStruct的Spring组件模型配置

这些优化确保了Lombok和MapStruct注解处理器能正确工作,避免"找不到生成的代码"的问题。

---

## 联系与支持

如果按以上步骤操作后仍有问题:

1. 检查IDEA Event Log(View -> Tool Windows -> Event Log)
2. 检查Maven构建日志
3. 确认IDEA版本为2025.3
4. 确认JDK版本为17
5. 查看详细的 `IDEA_CONFIG_GUIDE.md`
