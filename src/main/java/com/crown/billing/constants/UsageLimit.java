package com.crown.billing.constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드에 이 어노테이션을 붙이면 호출 전 플랜별 사용량 한도를 체크합니다.
 * 한도 초과 시 UsageLimitExceededException 발생.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsageLimit {
    UsageType type();
}
