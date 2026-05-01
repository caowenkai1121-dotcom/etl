package com.etl.engine.transform.rules;

import com.etl.common.enums.TransformRuleType;
import com.etl.engine.transform.TransformRuleFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 加密解密规则
 */
@Slf4j
@Component
public class EncryptRule implements TransformRuleFactory.TransformRule {

    @PostConstruct
    public void init() {
        TransformRuleFactory.register(getType(), this);
    }

    @Override
    public TransformRuleType getType() {
        return TransformRuleType.ENCRYPT;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> record, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>(record);
        String field = (String) config.get("field");
        String algorithm = (String) config.getOrDefault("algorithm", "MD5");
        String targetField = (String) config.getOrDefault("targetField", field);

        if (field != null && result.containsKey(field)) {
            Object value = result.get(field);
            if (value != null) {
                String encrypted = encrypt(value.toString(), algorithm);
                result.put(targetField, encrypted);
            }
        }
        return result;
    }

    private String encrypt(String input, String algorithm) {
        try {
            switch (algorithm.toUpperCase()) {
                case "MD5":
                    return hash(input, "MD5");
                case "SHA256":
                    return hash(input, "SHA-256");
                case "BASE64":
                    return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
                default:
                    return hash(input, "MD5");
            }
        } catch (Exception e) {
            log.warn("加密失败，使用原始值", e);
            return input;
        }
    }

    private String hash(String input, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
