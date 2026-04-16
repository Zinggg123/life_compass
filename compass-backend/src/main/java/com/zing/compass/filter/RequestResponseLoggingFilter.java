package com.zing.compass.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_LEN = 2000;
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "token", "authorization", "secret", "accessToken", "refreshToken"
    ));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        Exception error = null;
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (IOException | ServletException ex) {
            error = ex;
            throw ex;
        } catch (RuntimeException ex) {
            error = ex;
            throw ex;
        } finally {
            String requestBody = resolveRequestPayload(requestWrapper);
            String responseBody = resolveResponsePayload(responseWrapper, error);
            log.info("HTTP {} {} requestBody={} responseBody={} status={}",
                    requestWrapper.getMethod(),
                    requestWrapper.getRequestURI(),
                    requestBody,
                    responseBody,
                    responseWrapper.getStatus());
            responseWrapper.copyBodyToResponse();
        }
    }

    private String resolveRequestPayload(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            return sanitizePayload(new String(content, resolveCharset(request.getCharacterEncoding())));
        }

        String queryString = request.getQueryString();
        if (StringUtils.hasText(queryString)) {
            return truncate(queryString);
        }

        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            return truncate(params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + Arrays.toString(entry.getValue()))
                    .collect(Collectors.joining("&")));
        }

        return "";
    }

    private String resolveResponsePayload(ContentCachingResponseWrapper response, Throwable error) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            return sanitizePayload(new String(content, resolveCharset(response.getCharacterEncoding())));
        }

        if (error != null) {
            return truncate(error.getClass().getSimpleName() + ": " + error.getMessage());
        }

        return "";
    }

    private String sanitizePayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return "";
        }
        String trimmed = payload.trim();
        if (!((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]")))) {
            return truncate(payload);
        }

        try {
            Object json = JSON.parse(trimmed);
            maskSensitiveFields(json);
            return truncate(JSON.toJSONString(json));
        } catch (Exception e) {
            return truncate(payload);
        }
    }

    private void maskSensitiveFields(Object node) {
        if (node instanceof JSONObject object) {
            for (String key : object.keySet()) {
                Object value = object.get(key);
                if (SENSITIVE_KEYS.contains(key)) {
                    object.put(key, maskValue(value));
                } else {
                    maskSensitiveFields(value);
                }
            }
        } else if (node instanceof JSONArray array) {
            for (Object item : array) {
                maskSensitiveFields(item);
            }
        }
    }

    private Object maskValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.length() <= 8) {
            return "***";
        }
        return text.substring(0, 3) + "***" + text.substring(text.length() - 2);
    }

    private Charset resolveCharset(String encoding) {
        if (!StringUtils.hasText(encoding)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= MAX_LEN) {
            return value;
        }
        return value.substring(0, MAX_LEN) + "...(truncated)";
    }
}



