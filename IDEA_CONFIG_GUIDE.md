# IDEA 2025.3 项目配置指南

## 问题说明
在IDEA 2025.3中导入项目后出现"找不到类"的报错,主要原因是Maven依赖未正确加载、JDK版本配置不正确或注解处理器未启用。

---

## 解决步骤

### 第一步: 配置JDK

1. **确保已安装JDK 17**
   - 下载地址: https://www.oracle.com/java/technologies/downloads/#java17
   - 或使用OpenJDK: https://adoptium.net/

2. **在IDEA中配置JDK**
   - 打开 `File` -> `Project Structure` (Ctrl+Alt+Shift+S)
   - 选择 `Project`
   - 在 `SDK` 下拉框中选择或添加 JDK 17
   - 设置 `Language level` 为 `17 - Sealed types, always-strict floating-point semantics`
   - 点击 `Apply` 和 `OK`

3. **检查模块JDK设置**
   - 在 `Project Structure` 中选择 `Modules`
   - 选择 `stock` 模块
   - 在 `Sources` 和 `Dependencies` 标签中确认 `Language level` 为 17
   - 点击 `Apply` 和 `OK`

---

### 第二步: 启用Maven并刷新项目

1. **打开Maven工具窗口**
   - 点击右侧的 `Maven` 标签,或通过 `View` -> `Tool Windows` -> `Maven`

2. **刷新Maven项目**
   - 点击Maven工具窗口中的 `Reload All Maven Projects` 按钮(刷新图标)
   - 或按快捷键 `Ctrl+Shift+O`

3. **如果Maven工具窗口为空,手动添加pom.xml**
   - 右键点击 `pom.xml` 文件
   - 选择 `Add as Maven Project`
   - 等待IDEA索引和依赖下载完成

---

### 第三步: 安装Lombok插件

1. **打开插件设置**
   - `File` -> `Settings` (Ctrl+Alt+S)
   - 选择 `Plugins`

2. **搜索并安装Lombok**
   - 在搜索框中输入 `Lombok`
   - 如果未安装,点击 `Install` 按钮安装
   - 安装完成后点击 `Apply` 和 `OK`
   - **重启IDEA**

3. **启用注解处理**
   - 重启后,打开 `File` -> `Settings` -> `Build, Execution, Deployment` -> `Compiler` -> `Annotation Processors`
   - 勾选 `Enable annotation processing`
   - 点击 `Apply` 和 `OK`

---

### 第四步: 配置MapStruct注解处理器

确保pom.xml中的MapStruct配置正确(当前配置已正确):

```xml
<!-- MapStruct 核心依赖 -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.3.Final</version>
</dependency>

<!-- MapStruct 注解处理器 -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.3.Final</version>
    <scope>provided</scope>
</dependency>
```

**IDEA设置**:
1. 打开 `File` -> `Settings` -> `Build, Execution, Deployment` -> `Compiler` -> `Annotation Processors`
2. 确认勾选了 `Enable annotation processing`
3. 在 `Processor path` 中应该能看到MapStruct处理器

---

### 第五步: 清理并重新构建项目

1. **清理Maven缓存**
   - 打开终端(Terminal)
   - 执行命令:
   ```bash
   ./mvnw clean
   ```

2. **重新编译项目**
   ```bash
   ./mvnw compile
   ```
   或在IDEA中: `Build` -> `Rebuild Project`

3. **下载所有依赖**
   ```bash
   ./mvnw dependency:resolve
   ```

---

### 第六步: 检查Maven仓库设置

1. **打开Maven设置**
   - `File` -> `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Maven`

2. **检查Maven home directory**
   - 选择 `Bundled (Maven 3)` 或自定义的Maven安装路径

3. **检查User settings file**
   - 通常使用默认配置 `~/.m2/settings.xml`
   - 如果使用国内镜像,确保配置正确

4. **检查Local repository**
   - 确认本地仓库路径正确(通常 `~/.m2/repository`)

---

### 第七步: 重新导入项目(如果以上步骤无效)

1. **关闭当前项目**
   - `File` -> `Close Project`

2. **删除IDEA配置文件(可选)**
   - 删除项目根目录下的 `.idea` 文件夹(如果有)

3. **重新打开项目**
   - 在欢迎界面选择 `Open`
   - 选择项目根目录 `c:/workspace/IntelliJ-IDEA/stock-system`
   - 选择 `Open as Project`

4. **等待IDEA索引完成**
   - IDEA会自动识别Maven项目并下载依赖
   - 右下角进度条完成后即可

---

## 常见问题排查

### 问题1: 依赖下载失败

**原因**: Maven仓库连接问题或网络问题

**解决方案**:
1. 配置阿里云Maven镜像(在 `~/.m2/settings.xml` 中):
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

2. 或使用VPN连接后重新下载

### 问题2: Lombok注解不生效

**解决方案**:
1. 确认Lombok插件已安装
2. 确认注解处理已启用
3. 执行 `File` -> `Invalidate Caches` -> `Invalidate and Restart`
4. 重新编译项目

### 问题3: Spring Boot类找不到

**解决方案**:
1. 确认pom.xml中的Spring Boot parent版本正确(3.4.3)
2. 执行Maven重新加载
3. 检查是否有网络问题导致依赖下载失败

### 问题4: MapStruct生成的代码找不到

**解决方案**:
1. 确认注解处理已启用
2. 执行 `Build` -> `Rebuild Project`
3. 检查target/generated-sources/annotations目录是否有生成的代码
4. 如果有但IDEA不识别,手动添加该目录为源代码目录:
   - `Project Structure` -> `Modules` -> `Sources`
   - 右键点击 `target/generated-sources/annotations` -> `Mark as` -> `Sources`

---

## 验证步骤

完成以上配置后,验证是否解决:

1. **检查依赖是否加载**
   - 在 `External Libraries` 中查看是否包含所有Maven依赖
   - 应该看到: spring-boot-starter-*, mysql-connector-j, lombok, mapstruct等

2. **运行测试**
   ```bash
   ./mvnw test
   ```
   或在IDEA中运行测试类

3. **启动应用程序**
   - 运行 `StockApplication.java` 中的 `main` 方法
   - 确认应用能正常启动

4. **检查错误提示**
   - 查看编辑器中是否还有红色错误提示
   - 如有,按上述步骤排查

---

## 快速命令参考

```bash
# 清理项目
./mvnw clean

# 编译项目
./mvnw compile

# 运行测试
./mvnw test

# 打包项目
./mvnw package

# 运行应用
./mvnw spring-boot:run

# 查看依赖树
./mvnw dependency:tree

# 跳过测试打包
./mvnw package -DskipTests
```

---

## 技术栈说明

- **Spring Boot**: 3.4.3
- **Java**: 17
- **构建工具**: Maven
- **数据库**: MySQL
- **模板引擎**: Thymeleaf
- **JSON处理**: FastJSON
- **代码简化**: Lombok
- **对象映射**: MapStruct

---

## 联系与支持

如果按照以上步骤仍然存在问题,请:
1. 检查IDEA版本是否为2025.3
2. 查看IDEA的Event Log(`View` -> `Tool Windows` -> `Event Log`)中的错误信息
3. 检查Maven构建日志是否有错误
