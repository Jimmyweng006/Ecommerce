package com.jimmyweng.ecommerce.logging;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class LogExecutionAspect {

    @Around("@annotation(com.jimmyweng.ecommerce.logging.LogExecution)")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        Logger logger = LoggerFactory.getLogger(pjp.getTarget().getClass());
        String method = pjp.getSignature().toShortString();
        String args = Arrays.stream(pjp.getArgs())
                .map(arg -> arg == null ? "null" : arg.toString())
                .collect(Collectors.joining(", "));

        String correlationId = MDC.get(RequestCorrelationFilter.CORRELATION_ID_KEY);
        if (logger.isInfoEnabled()) {
            logger.info(
                    "{} - entering with args: {} (corrId={})",
                    method,
                    args,
                    correlationId);
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Object result = pjp.proceed();
            stopWatch.stop();
            if (logger.isInfoEnabled()) {
                logger.info(
                        "{} - completed in {} ms (corrId={})",
                        method,
                        stopWatch.getTotalTimeMillis(),
                        correlationId);
            }
            return result;
        } catch (Throwable ex) {
            stopWatch.stop();
            logger.error(
                    "{} - failed after {} ms (corrId={})",
                    method,
                    stopWatch.getTotalTimeMillis(),
                    correlationId,
                    ex);
            throw ex;
        }
    }
}
