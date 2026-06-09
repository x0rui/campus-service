package com.campus.service.aspect;

import com.campus.service.annotation.OpLog;
import com.campus.service.entity.OperationLog;
import com.campus.service.mapper.OperationLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OpLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OpLogAspect.class);
    private final OperationLogMapper operationLogMapper;

    public OpLogAspect(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint point, OpLog opLog) throws Throwable {
        String operation = opLog.value();
        long start = System.currentTimeMillis();
        String methodName = point.getSignature().toShortString();
        try {
            Object result = point.proceed();
            long time = System.currentTimeMillis() - start;
            log.info("操作: {} | 耗时: {}ms | 成功", operation, time);

            // 保存到数据库
            OperationLog op = new OperationLog();
            op.setOperation(operation);
            op.setMethod(methodName);
            op.setCreateTime(java.time.LocalDateTime.now());
            try { operationLogMapper.insert(op); } catch (Exception ignored) {}

            return result;
        } catch (Exception e) {
            long time = System.currentTimeMillis() - start;
            log.error("操作: {} | 耗时: {}ms | 失败: {}", operation, time, e.getMessage());

            OperationLog op = new OperationLog();
            op.setOperation(operation + " [失败]");
            op.setMethod(methodName);
            op.setCreateTime(java.time.LocalDateTime.now());
            try { operationLogMapper.insert(op); } catch (Exception ignored) {}

            throw e;
        }
    }
}