package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.service.entity.Question;
import com.campus.service.entity.QuestionRecord;
import com.campus.service.entity.QuestionWrong;
import com.campus.service.entity.Resource;
import com.campus.service.mapper.QuestionMapper;
import com.campus.service.mapper.QuestionRecordMapper;
import com.campus.service.mapper.QuestionWrongMapper;
import com.campus.service.mapper.ResourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 题库练习：客观题自动判分、主观题只存作答；错题本按知识点反向推荐资料
@Service
public class QuestionService {

    // 题型: 0单选 1多选 2判断 3填空 4主观
    private static final int TYPE_SUBJECTIVE = 4;

    private final QuestionMapper questionMapper;
    private final QuestionRecordMapper recordMapper;
    private final QuestionWrongMapper wrongMapper;
    private final ResourceMapper resourceMapper;

    public QuestionService(QuestionMapper questionMapper, QuestionRecordMapper recordMapper,
                           QuestionWrongMapper wrongMapper, ResourceMapper resourceMapper) {
        this.questionMapper = questionMapper;
        this.recordMapper = recordMapper;
        this.wrongMapper = wrongMapper;
        this.resourceMapper = resourceMapper;
    }

    // 管理员录题
    public Question add(Question q) {
        q.setQuestionId(null);
        questionMapper.insert(q);
        return q;
    }

    public List<String> getSubjects() {
        return questionMapper.selectSubjects();
    }

    // 取题：答案与解析不下发，避免直接看答案
    public List<Question> list(String subject, String chapter, int page) {
        QueryWrapper<Question> w = new QueryWrapper<>();
        if (ResourceService.notBlank(subject)) w.eq("subject", subject);
        if (ResourceService.notBlank(chapter)) w.eq("chapter", chapter);
        w.orderByAsc("question_id").last("LIMIT " + (page * 10) + ", 10");
        List<Question> list = questionMapper.selectList(w);
        for (Question q : list) {
            q.setAnswer(null);
            q.setAnalysis(null);
        }
        return list;
    }

    // 提交作答：客观题判分，主观题只存作答
    @Transactional
    public Map<String, Object> submit(Long userId, Long questionId, String userAnswer) {
        Map<String, Object> res = new HashMap<>();
        Question q = questionMapper.selectById(questionId);
        if (q == null) return res;

        boolean correct = judge(q, userAnswer);
        QuestionRecord rec = new QuestionRecord();
        rec.setUserId(userId);
        rec.setQuestionId(questionId);
        rec.setUserAnswer(userAnswer == null ? "" : userAnswer);
        rec.setIsCorrect(correct ? 1 : 0);
        recordMapper.insert(rec);

        // 答错（且不是主观题）进错题本
        if (!correct && !isSubjective(q)) {
            QuestionWrong exist = wrongMapper.selectOne(new QueryWrapper<QuestionWrong>()
                    .eq("user_id", userId).eq("question_id", questionId));
            if (exist == null) {
                QuestionWrong wq = new QuestionWrong();
                wq.setUserId(userId);
                wq.setQuestionId(questionId);
                wq.setWrongTimes(1);
                wrongMapper.insert(wq);
            } else {
                exist.setWrongTimes(exist.getWrongTimes() + 1);
                wrongMapper.updateById(exist);
            }
        }

        res.put("isCorrect", correct);
        res.put("answer", q.getAnswer());
        res.put("analysis", q.getAnalysis());
        res.put("subjective", isSubjective(q));
        return res;
    }

    private boolean isSubjective(Question q) {
        return q.getQType() != null && q.getQType() == TYPE_SUBJECTIVE;
    }

    // 判分规则：多选忽略顺序；判断归一化；单选/填空忽略空格与大小写；主观题不判
    private boolean judge(Question q, String userAnswer) {
        if (isSubjective(q) || userAnswer == null) return false;
        String a = q.getAnswer() == null ? "" : q.getAnswer().trim();
        String u = userAnswer.trim();
        if (a.isEmpty() || u.isEmpty()) return false;

        Integer t = q.getQType();
        if (t != null && t == 1) {
            return sortedUpper(a).equals(sortedUpper(u));
        }
        if (t != null && t == 2) {
            return normBool(a).equals(normBool(u));
        }
        return a.replaceAll("\\s+", "").equalsIgnoreCase(u.replaceAll("\\s+", ""));
    }

    private String sortedUpper(String s) {
        char[] cs = s.replaceAll("\\s+", "").toUpperCase().toCharArray();
        Arrays.sort(cs);
        return new String(cs);
    }

    // 判断题归一化：对/是/true/A/1 → T，错/否/false/B/0 → F
    private String normBool(String s) {
        String v = s.replaceAll("\\s+", "").toLowerCase();
        if (v.equals("对") || v.equals("是") || v.equals("true") || v.equals("a") || v.equals("1")) return "T";
        if (v.equals("错") || v.equals("否") || v.equals("false") || v.equals("b") || v.equals("0")) return "F";
        return v;
    }

    public List<QuestionRecord> getRecord(Long userId, int page) {
        return recordMapper.selectList(new QueryWrapper<QuestionRecord>()
                .eq("user_id", userId).orderByDesc("create_time").last("LIMIT " + (page * 20) + ", 20"));
    }

    public List<Question> getWrong(Long userId) {
        List<Question> list = questionMapper.selectWrongQuestions(userId);
        for (Question q : list) {
            q.setAnswer(null);
            q.setAnalysis(null);
        }
        return list;
    }

    public boolean removeWrong(Long userId, Long questionId) {
        return wrongMapper.delete(new QueryWrapper<QuestionWrong>()
                .eq("user_id", userId).eq("question_id", questionId)) > 0;
    }

    // 错题本 → 按薄弱知识点（课程 / 章节）反向推荐平台内的学习资料
    public List<Resource> recommendResources(Long userId) {
        List<Question> wrongs = questionMapper.selectWrongQuestions(userId);
        if (wrongs.isEmpty()) return new ArrayList<>();

        Set<String> subjects = new LinkedHashSet<>();
        Set<String> chapters = new LinkedHashSet<>();
        for (Question q : wrongs) {
            if (ResourceService.notBlank(q.getSubject())) subjects.add(q.getSubject());
            if (ResourceService.notBlank(q.getChapter())) chapters.add(q.getChapter());
        }
        if (subjects.isEmpty() && chapters.isEmpty()) return new ArrayList<>();

        QueryWrapper<Resource> w = new QueryWrapper<>();
        w.eq("status", 1);
        boolean[] first = {true};
        w.and(q -> {
            for (String s : subjects) {
                if (first[0]) { q.like("course", s); first[0] = false; }
                else q.or().like("course", s);
            }
            for (String c : chapters) {
                if (first[0]) { q.like("title", c); first[0] = false; }
                else q.or().like("title", c);
            }
        });
        w.orderByDesc("download_count").last("LIMIT 20");
        return resourceMapper.selectList(w);
    }
}
