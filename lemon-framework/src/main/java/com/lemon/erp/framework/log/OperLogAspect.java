package com.lemon.erp.framework.log;

import com.lemon.erp.common.util.ServletUtils;
import com.lemon.erp.framework.security.SecurityUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

/**
 * 拦截 @OperLog，收集操作信息后发布 OperLogEvent，由业务模块异步落库
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private static final int MAX_LENGTH = 2000;
    /**
     * 完全隐藏：密码、第三方应用密钥、群机器人地址和加签密钥、大模型 API Key，电商店铺 / 广告 / 1688 / 海外仓 / 物流商的授权
     * （另外名称以 secret、token、password、credentials、apikey 等结尾的字段也按凭证处理，见 {@link #isSecret}）
     */
    private static final Set<String> SECRET_FIELDS = Set.of("password", "oldPassword", "newPassword", "adminPassword", "appSecret",
            "robotWebhook", "robotSecret", "apiKey", "secret", "secretKey", "clientSecret", "partnerKey", "accessToken",
            "refreshToken", "adminAccessToken", "credentials", "adsCredentials", "carrierCredentials", "auth", "carrierAuth",
            "authorization", "cookie");
    /** 字段名（小写）以这些结尾的按凭证完全隐藏 */
    private static final String[] SECRET_SUFFIXES = {"secret", "token", "password", "credentials", "apikey", "api_key",
            "privatekey", "private_key", "partnerkey", "partner_key", "accesskey", "access_key", "secretkey", "secret_key"};
    /** 部分隐藏（保留首尾）：证件号、银行卡号、手机号 */
    private static final Set<String> PARTIAL_FIELDS = Set.of("idCard", "bankAccount", "phone", "mobile", "emergencyPhone");
    /** 薪资类金额：隐藏数值 */
    private static final Set<String> SALARY_FIELDS = Set.of("baseSalary", "salary", "postAllowance", "otherAllowance",
            "perfBonusBase", "socialBase", "housingBase", "grossPay", "netPay", "fromSalary", "toSalary");
    private static final String MASK = "******";

    private final ApplicationEventPublisher publisher;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        // 在执行前取用户名：注销等操作执行后上下文可能已失效
        String operator = SecurityUtils.getUsernameOrNull();
        Object result = null;
        Throwable error = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                publish(point, operLog, operator, result, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("记录操作日志失败", e);
            }
        }
    }

    private void publish(ProceedingJoinPoint point, OperLog operLog, String operator, Object result, Throwable error,
                         long cost) {
        HttpServletRequest request = ServletUtils.getRequest();
        MethodSignature signature = (MethodSignature) point.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName() + "()";
        publisher.publishEvent(new OperLogEvent(
                operLog.title(),
                operLog.type().getCode(),
                method,
                request == null ? null : request.getMethod(),
                request == null ? null : request.getRequestURI(),
                ServletUtils.getClientIp(request),
                operator,
                operLog.saveParams() ? toJson(filterArgs(point.getArgs())) : null,
                operLog.saveResult() && result != null ? toJson(result) : null,
                error == null ? 1 : 0,
                error == null ? null : truncate(error.getMessage()),
                cost,
                LocalDateTime.now()));
    }

    private Object[] filterArgs(Object[] args) {
        return Arrays.stream(args)
                .filter(arg -> !(arg instanceof ServletRequest || arg instanceof ServletResponse
                        || arg instanceof MultipartFile || arg instanceof BindingResult))
                .toArray();
    }

    private String toJson(Object value) {
        try {
            JsonNode node = objectMapper.valueToTree(value);
            mask(node);
            return truncate(objectMapper.writeValueAsString(node));
        } catch (Exception e) {
            return truncate(String.valueOf(value));
        }
    }

    /** 递归脱敏：密码、薪资完全隐藏，证件号、银行卡号、手机号保留首尾 */
    private void mask(JsonNode node) {
        if (node instanceof ObjectNode obj) {
            for (String name : obj.propertyNames()) {
                JsonNode v = obj.get(name);
                if (isSecret(name) && v != null && !v.isNull() && !v.isBoolean()
                        || SALARY_FIELDS.contains(name) && v != null && !v.isNull()) {
                    obj.put(name, MASK);
                } else if (PARTIAL_FIELDS.contains(name) && v != null && v.isString()) {
                    obj.put(name, partial(v.asString()));
                } else if (v != null && v.isString()) {
                    String nested = maskNestedJson(v.asString());
                    if (nested != null) {
                        obj.put(name, nested);
                    }
                } else {
                    mask(v);
                }
            }
        } else if (node instanceof ArrayNode arr) {
            for (int i = 0; i < arr.size(); i++) {
                JsonNode v = arr.get(i);
                String nested = v.isString() ? maskNestedJson(v.asString()) : null;
                if (nested != null) {
                    arr.set(i, arr.stringNode(nested));
                } else {
                    mask(v);
                }
            }
        }
    }

    static boolean isSecret(String name) {
        if (SECRET_FIELDS.contains(name)) {
            return true;
        }
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        for (String suffix : SECRET_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /** 字符串本身是 JSON 对象 / 数组（如以 JSON 文本提交的授权）时脱敏后重新序列化；不是 JSON 或无需修改时返回 null */
    private String maskNestedJson(String text) {
        String t = text.trim();
        if (t.length() < 2 || !(t.startsWith("{") && t.endsWith("}") || t.startsWith("[") && t.endsWith("]"))) {
            return null;
        }
        try {
            JsonNode inner = objectMapper.readTree(t);
            if (!(inner instanceof ObjectNode || inner instanceof ArrayNode)) {
                return null;
            }
            String before = objectMapper.writeValueAsString(inner);
            mask(inner);
            String after = objectMapper.writeValueAsString(inner);
            return before.equals(after) ? null : after;
        } catch (Exception e) {
            return null;
        }
    }

    /** 保留前 3 位、后 4 位，过短的全部隐藏 */
    static String partial(String s) {
        if (s.length() <= 7) {
            return MASK;
        }
        return s.substring(0, 3) + "*".repeat(s.length() - 7) + s.substring(s.length() - 4);
    }

    private static String truncate(String s) {
        return s != null && s.length() > MAX_LENGTH ? s.substring(0, MAX_LENGTH) : s;
    }
}
