package com.jimmyweng.ecommerce.datasource.aspect;

import com.jimmyweng.ecommerce.datasource.ReplicaRoutingContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ReadFromPrimaryAspect {

    @Around("@annotation(com.jimmyweng.ecommerce.datasource.annotation.ReadFromPrimary)"
            + " || @within(com.jimmyweng.ecommerce.datasource.annotation.ReadFromPrimary)")
    public Object forcePrimary(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            ReplicaRoutingContext.forcePrimary();
            return joinPoint.proceed();
        } finally {
            ReplicaRoutingContext.clear();
        }
    }
}
