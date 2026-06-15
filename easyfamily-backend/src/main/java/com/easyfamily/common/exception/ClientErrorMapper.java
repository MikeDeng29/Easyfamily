package com.easyfamily.common.exception;

import java.util.Map;

import static java.util.Map.entry;

public final class ClientErrorMapper {

    private static final ErrorView DEFAULT_ERROR =
            new ErrorView("BIZ_ERROR", "请求暂时无法处理，请稍后重试");

    private static final Map<String, ErrorView> BUSINESS_ERROR_MAPPING = Map.ofEntries(
            entry("INVALID_CAPTCHA_TOKEN", new ErrorView("AUTH_FAILED", "人机校验已失效，请重新验证")),
            entry("CAPTCHA_REQUIRED", new ErrorView("CAPTCHA_REQUIRED", "检测到频繁请求，请完成滑动验证后重试")),
            entry("CAPTCHA_CHALLENGE_NOT_FOUND", new ErrorView("AUTH_FAILED", "滑动验证码不存在，请刷新后重试")),
            entry("CAPTCHA_CHALLENGE_ALREADY_USED", new ErrorView("AUTH_FAILED", "滑动验证码已使用，请重新获取")),
            entry("CAPTCHA_CHALLENGE_EXPIRED", new ErrorView("AUTH_FAILED", "滑动验证码已过期，请重新获取")),
            entry("CAPTCHA_SLIDE_VERIFY_FAILED", new ErrorView("AUTH_FAILED", "滑动验证失败，请重试")),
            entry("CAPTCHA_RISK_DETECTED", new ErrorView("AUTH_FAILED", "验证行为异常，请重试")),
            entry("SMS_SEND_FAILED", new ErrorView("SERVICE_UNAVAILABLE", "短信发送失败，请稍后重试")),
            entry("SMS_SEND_ERROR", new ErrorView("SERVICE_UNAVAILABLE", "短信服务暂不可用，请稍后重试")),
            entry("INVALID_SMS_CODE", new ErrorView("AUTH_FAILED", "短信验证码错误或已过期")),
            entry("INVALID_ADMIN_CREDENTIALS", new ErrorView("AUTH_FAILED", "账号或密码错误")),
            entry("TOKEN_REVOKED", new ErrorView("AUTH_FAILED", "登录状态已失效，请重新登录")),
            entry("INVALID_REFRESH_TOKEN", new ErrorView("AUTH_FAILED", "登录状态已过期，请重新登录")),
            entry("UNAUTHORIZED", new ErrorView("UNAUTHORIZED", "请先登录")),
            entry("PHONE_ALREADY_BOUND", new ErrorView("PHONE_ERROR", "该手机号已绑定")),
            entry("PHONE_NOT_FOUND", new ErrorView("PHONE_ERROR", "手机号不存在")),
            entry("PRIMARY_PHONE_CANNOT_UNBIND", new ErrorView("PHONE_ERROR", "主手机号不可解绑")),
            entry("PHONE_NOT_BOUND_TO_USER", new ErrorView("PHONE_ERROR", "该手机号未绑定到当前账号")),
            entry("FAMILY_MEMBER_NOT_FOUND", new ErrorView("BIZ_ERROR", "家庭成员不存在")),
            entry("FAMILY_MEMBER_PHONE_EXISTS", new ErrorView("BIZ_ERROR", "该手机号已存在于家庭成员列表")),
            entry("QUOTA_EXCEEDED", new ErrorView("QUOTA_EXCEEDED", "查询额度已用完，请明日再试")),
            entry("INVALID_PROVIDER_KEY", new ErrorView("SERVICE_UNAVAILABLE", "查询服务配置异常，请稍后再试")),
            entry("PROVIDER_CIRCUIT_OPEN", new ErrorView("SERVICE_UNAVAILABLE", "查询服务繁忙，请稍后再试")),
            entry("PROVIDER_TIMEOUT", new ErrorView("SERVICE_UNAVAILABLE", "查询超时，请稍后重试")),
            entry("PROVIDER_UNAVAILABLE", new ErrorView("SERVICE_UNAVAILABLE", "查询服务暂不可用，请稍后再试")),
            entry("ALIYUN_MARKET_HTTP_ERROR", new ErrorView("SERVICE_UNAVAILABLE", "第三方查询服务异常，请稍后重试")),
            entry("ALIYUN_MARKET_CALL_ERROR", new ErrorView("SERVICE_UNAVAILABLE", "第三方查询服务调用失败，请稍后重试")),
            entry("ALIYUN_MARKET_CONFIG_MISSING", new ErrorView("SERVICE_UNAVAILABLE", "查询服务配置不完整，请联系管理员")),
            entry("BILL_NOT_FOUND", new ErrorView("BIZ_ERROR", "账单不存在或无权操作")),
            entry("BILL_INVALID_DATE", new ErrorView("INVALID_PARAM", "日期格式错误，请使用 yyyy-MM-dd")),
            entry("BILL_INVALID_MONTH", new ErrorView("INVALID_PARAM", "月份格式错误，请使用 yyyy-MM")),
            entry("MEMORY_NOT_FOUND", new ErrorView("BIZ_ERROR", "记忆不存在或无权操作"))
    );

    private ClientErrorMapper() {
    }

    public static ErrorView fromBusinessCode(String internalCode) {
        return BUSINESS_ERROR_MAPPING.getOrDefault(internalCode, DEFAULT_ERROR);
    }

    public record ErrorView(String code, String message) {
    }
}
