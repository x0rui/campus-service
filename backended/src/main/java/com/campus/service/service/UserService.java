package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.service.entity.User;
import com.campus.service.mapper.UserMapper;
import com.campus.service.util.JwtUtil;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    // 登录
    public Map<String, Object> login(String openid, String nickName, String avatarUrl) {
        // 1. 查微信 openid 有没有在数据库里
        User user = userMapper.selectByOpenid(openid);

        if (user == null) {
            // 2. 没查到，创建新用户进行注册
            user = new User();
            user.setOpenid(openid);
            user.setNickName(nickName);
            user.setAvatarUrl(avatarUrl);
            user.setRole(0);        // 默认普通学生
            user.setStatus(0);      // 默认正常
            userMapper.insert(user);// 插入数据库
        } else if (user.getStatus() == 1) {
            return null; // 账号被禁用了 → 返回 null
        }

        // 3. 生成 JWT token
        String token = jwtUtil.generateToken(user.getUserId(), user.getOpenid(), user.getRole());
        // 4. 返回 token 和用户信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    public boolean updateUser(User user) {
        return userMapper.updateById(user) > 0;
    }

    public boolean bindInfo(Long userId, String realName, String studentId, String phone,
                           String college, String major, String className,
                           Integer age, String gender) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        user.setRealName(realName);
        user.setStudentId(studentId);
        user.setPhone(phone);
        user.setCollege(college);
        user.setMajor(major);
        user.setClassName(className);
        user.setAge(age);
        user.setGender(gender);
        return userMapper.updateById(user) > 0;
    }

    public boolean updateRole(Long userId, Integer role) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        user.setRole(role);
        return userMapper.updateById(user) > 0;
    }

    public List<User> listAll() {
        return userMapper.selectList(null);
    }

    public List<User> listAllByUserId(String userId) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        try {
            qw.eq(User::getUserId, Long.valueOf(userId));
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
        return userMapper.selectList(qw);
    }

    // 统计
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalStudents", userMapper.countStudents());
        stats.put("todayNewUsers", userMapper.countTodayNewUsers());
        return stats;
    }

    public List<User> getRecommendedUsers(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getHobbies() == null || user.getHobbies().isEmpty()) {
            return new ArrayList<>();
        }
        String[] hobbies = user.getHobbies().split(",");
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.ne(User::getUserId, userId);
        qw.isNotNull(User::getHobbies).ne(User::getHobbies, "");
        qw.and(w -> {
            boolean first = true;
            for (String h : hobbies) {
                String kw = h.trim();
                if (kw.isEmpty()) continue;
                if (first) { w.like(User::getHobbies, kw); first = false; }
                else { w.or().like(User::getHobbies, kw); }
            }
        });
        qw.last("LIMIT 20");
        return userMapper.selectList(qw);
    }
}
