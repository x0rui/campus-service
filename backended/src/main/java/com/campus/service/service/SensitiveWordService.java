package com.campus.service.service;

import com.campus.service.mapper.SensitiveWordMapper;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 数据库存敏感词 → 启动加载到 HashSet → 发布时遍历检测 → 大量敏感词用 Trie 优化 → 新增敏感词用定时刷新
 */
@Service
public class SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;
    private Set<String> sensitiveWords = new HashSet<>();

    public SensitiveWordService(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @PostConstruct // 项目启动时自动执行
    public void loadSensitiveWords() {
        List<String> words = sensitiveWordMapper.selectAllWords(); // 从数据库查敏感词
        sensitiveWords = new HashSet<>(words); // 创建敏感词集合，存到内存（HashSet）
    }

    /**
     * 检测文本是否包含敏感词，返回命中的敏感词列表
     */
    public List<String> check(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        List<String> hits = new ArrayList<>();
        for (String word : sensitiveWords) { // 遍历所有敏感词
            if (text.contains(word)) { // 检测文本是否包含敏感词
                hits.add(word);        // 有就记下来
            }
        }
        return hits;
    }

    public void refresh() {
        loadSensitiveWords();
    }
}
