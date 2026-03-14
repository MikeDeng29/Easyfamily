package com.easyfamily.common.exception;

import com.easyfamily.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        log.warn("Business exception occurred, internalCode={}, detail={}", ex.getCode(), ex.getMessage());
        ClientErrorMapper.ErrorView errorView = ClientErrorMapper.fromBusinessCode(ex.getCode());
        return ApiResponse.fail(errorView.code(), errorView.message());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ApiResponse<Void> handleValidation(Exception ex) {
        log.warn("Validation exception occurred: {}", ex.getMessage());
        return ApiResponse.fail("BAD_REQUEST", "请求参数不合法，请检查后重试");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGeneric(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        return ApiResponse.fail("INTERNAL_ERROR", "系统繁忙，请稍后再试");
    }
}
