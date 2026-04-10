package com.crown.common.exception;

import com.crown.billing.service.InsufficientTokenException;
import com.crown.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final JdbcTemplate jdbcTemplate;

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.fail(e.getMessage());
    }

    @ExceptionHandler(InsufficientTokenException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInsufficientToken(InsufficientTokenException e) {
        Map<String, Object> data = Map.of(
                "code", "INSUFFICIENT_TOKENS",
                "required", e.getRequired(),
                "balance", e.getBalance()
        );
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ApiResponse<>(false, e.getMessage(), data));
    }

    @ExceptionHandler(PlanFeatureBlockedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlePlanFeatureBlocked(PlanFeatureBlockedException e) {
        Map<String, Object> data = Map.of(
                "code", "PLAN_UPGRADE_REQUIRED",
                "currentPlan", e.getCurrentPlan(),
                "requiredPlan", e.getRequiredPlan()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, e.getMessage(), data));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        String path = request.getMethod() + " " + request.getRequestURI();
        log.error("[GlobalExceptionHandler] {} {}: {}", path, e.getClass().getSimpleName(), e.getMessage(), e);
        saveErrorLog(path, e);
        return ApiResponse.fail("서버 오류가 발생했습니다.");
    }

    private void saveErrorLog(String path, Exception e) {
        try {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stack = sw.toString();
            String message = e.getClass().getSimpleName() + ": " + e.getMessage();
            jdbcTemplate.update(
                "INSERT INTO error_log (source, level, path, message, stack_trace) VALUES (?,?,?,?,?)",
                "crown_api", "ERROR",
                path != null ? path.substring(0, Math.min(path.length(), 200)) : null,
                message.substring(0, Math.min(message.length(), 2000)),
                stack.substring(0, Math.min(stack.length(), 4000))
            );
        } catch (Exception dbEx) {
            log.warn("[GlobalExceptionHandler] 에러 로그 DB 저장 실패: {}", dbEx.getMessage());
        }
    }
}
