package com.campus.service.service;

import com.campus.service.entity.Task;
import com.campus.service.mapper.TaskMapper;
import com.campus.service.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final NotificationService notificationService;
    private final TencentMapService mapService;
    private final StringRedisTemplate redis;

    @Value("${platform.max-task-fee}")
    private int maxTaskFee;

    public TaskService(TaskMapper taskMapper, NotificationService notificationService,
                       TencentMapService mapService, StringRedisTemplate redis) {
        this.taskMapper = taskMapper;
        this.notificationService = notificationService;
        this.mapService = mapService;
        this.redis = redis;
    }

    public Task publish(Task task) {
        task.setStatus(0);
        taskMapper.insert(task);
        return task;
    }

    public List<Task> getSquareList(int page) {
        return taskMapper.selectPendingList(page * 10, 10);
    }

    // 任务广场"附近优先"：按当前定位到取件点的球面距离升序，再按任务类型筛
    // 距离在本地算（Haversine），不比直线更精确就不调外部接口，省额度也少一个故障点
    public List<Task> getNearbyList(Double lat, Double lng, String taskType, int page) {
        List<Task> all = (taskType == null || taskType.isEmpty())
                ? taskMapper.selectAllPending()
                : taskMapper.selectByType(taskType);

        if (lat != null && lng != null) {
            for (Task t : all) {
                t.setDistanceMeters(haversine(lat, lng, t.getPickupLat(), t.getPickupLng()));
            }
            all.sort(Comparator.comparingDouble(t ->
                    t.getDistanceMeters() == null ? Double.MAX_VALUE : t.getDistanceMeters()));
        }
        int from = Math.min(page * 10, all.size());
        int to = Math.min(from + 10, all.size());
        return all.subList(from, to);
    }

    private static Double haversine(double lat1, double lng1, java.math.BigDecimal lat2, java.math.BigDecimal lng2) {
        if (lat2 == null || lng2 == null) return null;
        double R = 6371000;
        double p2 = lat2.doubleValue();
        double dLat = Math.toRadians(p2 - lat1);
        double dLng = Math.toRadians(lng2.doubleValue() - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(p2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // 经纬度 → 文字地址（地图选点后回填用）
    public String reverseGeocode(double lat, double lng) {
        return mapService.reverseGeocode(lat, lng);
    }

    // 接单者上报实时位置：存 Redis（5 分钟过期）+ 经 WebSocket 推给发布者
    public String reportLocation(Long taskId, Long userId, double lat, double lng) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) return "任务不存在";
        if (t.getStatus() == null || t.getStatus() != 1) return "任务不在进行中";
        if (!userId.equals(t.getTakerId())) return "只有接单者可以上报位置";

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "location");
        payload.put("taskId", taskId);
        payload.put("lat", lat);
        payload.put("lng", lng);
        payload.put("time", LocalDateTime.now().toString());
        try {
            redis.opsForValue().set("task:loc:" + taskId,
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload),
                    5, TimeUnit.MINUTES);
        } catch (Exception ignored) {}
        ChatWebSocketHandler.pushRaw(t.getPublisherId(), payload);
        return null;
    }

    // 发布者取最新位置（WebSocket 不在线时的轮询兜底）
    public String getLocation(Long taskId) {
        try {
            return redis.opsForValue().get("task:loc:" + taskId);
        } catch (Exception e) {
            return null;
        }
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
            try {
                notificationService.send(task.getPublisherId(), 1, "任务已被接单",
                        "你的跑腿任务「" + (task.getPickupLocation() + "→" + task.getDeliveryLocation()) + "」已被接单", taskId);
            } catch (Exception ignored) {}
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
        boolean ok = taskMapper.updateById(task) > 0;
        if (ok) {
            try {
                Long otherId = isPublisher ? task.getTakerId() : task.getPublisherId();
                notificationService.send(otherId, 1, "任务已完成",
                        "跑腿任务「" + (task.getPickupLocation() + "→" + task.getDeliveryLocation()) + "」已完成", taskId);
            } catch (Exception ignored) {}
        }
        return ok;
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
        if (rows > 0) {
            try {
                notificationService.send(task.getPublisherId(), 1, "接单者放弃任务",
                        "跑腿任务「" + (task.getPickupLocation() + "→" + task.getDeliveryLocation()) + "」的接单者已放弃", taskId);
            } catch (Exception ignored) {}
            return null;
        }
        return "操作失败";
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

    public int countCompletedByTaker(Long userId) {
        return taskMapper.countCompletedByTaker(userId);
    }

    public int countTakenByTaker(Long userId) {
        return taskMapper.countTakenByTaker(userId);
    }
}
