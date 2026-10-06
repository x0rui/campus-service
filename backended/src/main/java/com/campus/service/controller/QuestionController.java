package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.Question;
import com.campus.service.entity.QuestionRecord;
import com.campus.service.entity.Resource;
import com.campus.service.service.QuestionService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

// 题库与练习模块
@RestController
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    // 1. 管理员录题 POST /add
    @OpLog("录入题目")
    @PostMapping("/add")
    public Result<?> add(HttpServletRequest request, @RequestBody Question question) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) return Result.fail(403, "无权限");
        if (question.getContent() == null || question.getContent().trim().isEmpty()) return Result.fail("题干不能为空");
        if (question.getAnswer() == null || question.getAnswer().trim().isEmpty()) return Result.fail("参考答案不能为空");
        if (question.getSubject() == null || question.getSubject().trim().isEmpty()) return Result.fail("科目不能为空");
        return Result.ok(questionService.add(question));
    }

    // 2. 科目列表 GET /subjects
    @GetMapping("/subjects")
    public Result<List<String>> subjects() {
        return Result.ok(questionService.getSubjects());
    }

    // 3. 取题 GET /list?subject=&chapter=&page=（不含答案与解析）
    @GetMapping("/list")
    public Result<List<Question>> list(@RequestParam(required = false) String subject,
                                       @RequestParam(required = false) String chapter,
                                       @RequestParam(defaultValue = "0") int page) {
        return Result.ok(questionService.list(subject, chapter, page));
    }

    // 4. 提交作答 POST /submit
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        Object qid = body.get("questionId");
        if (qid == null) return Result.fail("缺少题目ID");
        String answer = body.get("answer") == null ? "" : body.get("answer").toString();
        Map<String, Object> res = questionService.submit(userId, Long.valueOf(qid.toString()), answer);
        return res.isEmpty() ? Result.fail("题目不存在") : Result.ok(res);
    }

    // 5. 我的答题记录 GET /record?page=
    @GetMapping("/record")
    public Result<List<QuestionRecord>> record(HttpServletRequest request,
                                               @RequestParam(defaultValue = "0") int page) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(questionService.getRecord(userId, page));
    }

    // 6. 错题本 GET /wrong
    @GetMapping("/wrong")
    public Result<List<Question>> wrong(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(questionService.getWrong(userId));
    }

    // 7. 移出错题本 DELETE /wrong/{questionId}
    @DeleteMapping("/wrong/{questionId}")
    public Result<?> removeWrong(HttpServletRequest request, @PathVariable Long questionId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return questionService.removeWrong(userId, questionId) ? Result.ok() : Result.fail("错题本中没有该题");
    }

    // 8. 错题本按知识点反向推荐资料 GET /wrong/recommend
    @GetMapping("/wrong/recommend")
    public Result<List<Resource>> recommend(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(questionService.recommendResources(userId));
    }
}
