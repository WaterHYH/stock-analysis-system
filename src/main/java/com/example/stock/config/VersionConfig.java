package com.example.stock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Configuration
public class VersionConfig {

    private String commitCount = "0";
    private String commitId = "unknown";
    private String buildTime = "unknown";

    public VersionConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("git.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                commitCount = props.getProperty("git.total.commit.count", "0");
                commitId = props.getProperty("git.commit.id.abbrev", "unknown");
                buildTime = props.getProperty("git.build.time", "unknown");
            }
        } catch (Exception e) {
            log.warn("无法读取git.properties, 使用默认版本号: {}", e.getMessage());
        }
    }

    public String getVersion() {
        return "v" + commitCount;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getBuildTime() {
        return buildTime;
    }
}
