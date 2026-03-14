package com.easyfamily.common.logging;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Aspect
@Component
public class ApiSummaryLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiSummaryLogAspect.class);
    private static final int MAX_LOG_VALUE_LENGTH = 300;
    private static final int MAX_LOG_TEXT_LENGTH = 1500;
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "pwd", "token", "smscode", "authorization", "secret", "appcode"
    );

    private final ObjectMapper objectMapper;

    public ApiSummaryLogAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("execution(* com.easyfamily..controller..*(..))")
    public Object logApiSummary(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNanos = System.nanoTime();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String httpMethod = "-";
        String uri = "-";
        String clientIp = "-";

        ServletRequestAttributes attrs = currentRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            httpMethod = request.getMethod();
            uri = request.getRequestURI();
            clientIp = resolveClientIp(request);
        }

        String requestSummary = toSummary(joinPoint.getArgs());
        try {
            Object response = joinPoint.proceed();
            long elapsedMs = elapsedMs(startNanos);
            String responseCode = resolveResponseCode(response);
            String responseSummary = toSummary(response);
            log.info(
                    "api_summary status=SUCCESS method={} httpMethod={} uri={} clientIp={} elapsedMs={} req={} respCode={} resp={}",
                    method,
                    httpMethod,
                    uri,
                    clientIp,
                    elapsedMs,
                    requestSummary,
                    responseCode,
                    responseSummary
            );
            return response;
        } catch (BusinessException ex) {
            long elapsedMs = elapsedMs(startNanos);
            log.warn(
                    "api_summary status=BIZ_FAIL method={} httpMethod={} uri={} clientIp={} elapsedMs={} req={} errorCode={} errorMsg={}",
                    method,
                    httpMethod,
                    uri,
                    clientIp,
                    elapsedMs,
                    requestSummary,
                    ex.getCode(),
                    truncate(ex.getMessage())
            );
            throw ex;
        } catch (Exception ex) {
            long elapsedMs = elapsedMs(startNanos);
            log.error(
                    "api_summary status=SYS_FAIL method={} httpMethod={} uri={} clientIp={} elapsedMs={} req={} errorType={} errorMsg={}",
                    method,
                    httpMethod,
                    uri,
                    clientIp,
                    elapsedMs,
                    requestSummary,
                    ex.getClass().getSimpleName(),
                    truncate(ex.getMessage())
            );
            throw ex;
        }
    }

    private ServletRequestAttributes currentRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes;
        }
        return null;
    }

    private String resolveResponseCode(Object response) {
        if (response instanceof ApiResponse<?> apiResponse) {
            return apiResponse.code();
        }
        return "UNKNOWN";
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String toSummary(Object value) {
        Object sanitized = sanitize(value);
        try {
            return truncate(objectMapper.writeValueAsString(sanitized));
        } catch (JsonProcessingException ex) {
            return truncate(String.valueOf(sanitized));
        }
    }

    private Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof HttpServletRequest || value instanceof HttpServletResponse) {
            return "[servlet-object]";
        }
        if (value instanceof byte[] bytes) {
            return "[bytes:" + bytes.length + "]";
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return sanitizeScalar(value);
        }
        if (value instanceof Object[] array) {
            List<Object> items = new ArrayList<>(array.length);
            for (Object item : array) {
                items.add(sanitize(item));
            }
            return items;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> items = new ArrayList<>(collection.size());
            for (Object item : collection) {
                items.add(sanitize(item));
            }
            return items;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveKey(key)) {
                    result.put(key, "***");
                } else {
                    result.put(key, sanitize(entry.getValue()));
                }
            }
            return result;
        }
        try {
            Object asObject = objectMapper.convertValue(value, Object.class);
            return sanitize(asObject);
        } catch (IllegalArgumentException ex) {
            return sanitizeScalar(value);
        }
    }

    private Object sanitizeScalar(Object value) {
        String text = String.valueOf(value);
        if (text.length() > MAX_LOG_VALUE_LENGTH) {
            return text.substring(0, MAX_LOG_VALUE_LENGTH) + "...";
        }
        return text;
    }

    private boolean isSensitiveKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        for (String token : SENSITIVE_KEYS) {
            if (lower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() <= MAX_LOG_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LOG_TEXT_LENGTH) + "...";
    }
}
