package com.campus.service.service;

import com.campus.service.entity.Task;
import com.campus.service.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class TaskService {

    private final TaskMapper taskMapper;

    @Value("${platform.max-task-fee}")
    private int maxTaskFee;

    public TaskService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public Task publish(Task task) {
        task.setStatus(0);
        taskMapper.insert(task);
        return task;
    }

    public List<Task> getSquareList(int page) {
        return taskMapper.selectPendingList(page * 10, 10);
    }

    public List<Task> getPendingList() {
        return taskMapper.selectAllPending();
    }

    public List<Task> getAllTasks() {
        return taskMapper.selectList(null);
    }

    public List<Task> getByType(String taskType) {
        return taskMapper.selectByType(taskType);
    }

    public List<Task> search(String keyword) {
        return taskMapper.searchByKeyword(keyword);
    }

    public Task getDetail(Long taskId, Long userId) {
        return taskMapper.selectById(taskId);
    }

    public String takeTask(Long taskId, Long takerId) {
        // 检查任务状态
        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 0) {
            return "任务已被抢，再看看别的吧";
        }
        if (task.getPublisherId().equals(takerId)) {
            return "不能接自己发布的任务";
        }
        // 每天最多接3单（演示用）
        int todayCount = taskMapper.countTodayTakeByUser(takerId);
        if (todayCount >= 3) {
            return "今日已接3单，明天再来吧";
        }
        // 更新任务状态，乐观锁：update 时检查 status=0，避免并发
        int rows = taskMapper.takeTask(taskId, takerId);
        if (rows > 0) {
            return null;
        }
        return "任务已被抢，再看看别的吧";
    }

    public boolean completeTask(Long taskId, Long userId, Long takerId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 1) {
            return false;
        }
        // 发布者或接单者都可以确认完成
        boolean isPublisher = task.getPublisherId().equals(userId);
        boolean isTaker = task.getTakerId().equals(userId);
        if (!isPublisher && !isTaker) {
            return false;
        }
        // 发布者确认时需传入正确的接单者ID
        if (isPublisher && (takerId == null || !task.getTakerId().equals(takerId))) {
            return false;
        }
        task.setStatus(2);
        task.setCompleteTime(java.time.LocalDateTime.now());
        return taskMapper.updateById(task) > 0;
    }

    public boolean updateTask(Long taskId, Long userId, Task update) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getPublisherId().equals(userId) || task.getStatus() != 0) {
            return false;
        }
        if (update.getPickupLocation() != null) task.setPickupLocation(update.getPickupLocation());
        if (update.getPickupLat() != null) task.setPickupLat(update.getPickupLat());
        if (update.getPickupLng() != null) task.setPickupLng(update.getPickupLng());
        if (update.getDeliveryLocation() != null) task.setDeliveryLocation(update.getDeliveryLocation());
        if (update.getDeliveryLat() != null) task.setDeliveryLat(update.getDeliveryLat());
        if (update.getDeliveryLng() != null) task.setDeliveryLng(update.getDeliveryLng());
        if (update.getFee() != null) task.setFee(update.getFee());
        if (update.getDeadline() != null) task.setDeadline(update.getDeadline());
        if (update.getRemark() != null) task.setRemark(update.getRemark());
        if (update.getTaskType() != null) task.setTaskType(update.getTaskType());
        return taskMapper.updateById(task) > 0;
    }

    // 接单者放弃任务，任务退回待接单
    public String giveUpTask(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !task.getTakerId().equals(userId) || task.getStatus() != 1) {
            return "操作失败";
        }
        int rows = taskMapper.giveUpTask(taskId, userId);
        return rows > 0 ? null : "操作失败";
    }

    public boolean cancelTask(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);
        // 只有待接单状态可取消；已接单后不允许取消（双方需协商或等超时）
        if (task == null || !task.getPublisherId().equals(userId) || task.getStatus() != 0) {
            return false;
        }
        task.setStatus(3);
        return taskMapper.updateById(task) > 0;
    }

    public List<Task> getPublishedTasks(Long userId) {
        return taskMapper.selectByPublisher(userId);
    }

    public List<Task> getTakenTasks(Long userId) {
        return taskMapper.selectByTaker(userId);
    }

    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pendingTasks", taskMapper.countPendingTasks());
        stats.put("todayTasks", taskMapper.countTodayTasks());
        return stats;
    }

    // 管理员强制取消（无视状态限制）
    public boolean adminForceCancel(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() == 2 || task.getStatus() == 3) return false;
        task.setStatus(3);
        return taskMapper.updateById(task) > 0;
    }

    // 每小时检查：过期待接单→自动取消，过期已接单未确认→自动完成
    @Scheduled(fixedRate = 3600000)
    public void autoCancelExpired() {
        int cancelled = taskMapper.autoCancelExpired();
        int completed = taskMapper.autoCompleteExpired();
        if (cancelled > 0) {
            System.out.println("自动取消了 " + cancelled + " 个已过期的待接单跑腿任务");
        }
        if (completed > 0) {
            System.out.println("自动完成了 " + completed + " 个截止时间已过的进行中跑腿任务");
        }
    }
}
