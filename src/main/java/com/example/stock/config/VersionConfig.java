package com.example.stock.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
@Component
public class VersionConfig {

    private String commitCount = "0";
    private String commitId = "unknown";

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("git.properties");
            if (resource.exists()) {
                Properties props = new Properties();
                props.load(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
                commitCount = props.getProperty("git.total.commit.count", "0");
                commitId = props.getProperty("git.commit.id.abbrev", "unknown");
                log.info("版本号加载成功: v{} ({})", commitCount, commitId);
            } else {
                log.warn("git.properties 文件未找到, 使用默认版本号");
                fallbackToGitCommand();
            }
        } catch (Exception e) {
            log.warn("读取git.properties失败: {}, 尝试git命令回退", e.getMessage());
            fallbackToGitCommand();
        }
    }

    private void fallbackToGitCommand() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-list", "--count", "HEAD");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    commitCount = line.trim();
                    log.info("通过git命令获取版本号: v{}", commitCount);
                }
            }
            process.waitFor();
        } catch (Exception e2) {
            log.warn("git命令也失败, 使用默认版本号 v0");
        }
    }

    public String getVersion() {
        return "v" + commitCount;
    }

    public String getCommitId() {
        return commitId;
    }
}
