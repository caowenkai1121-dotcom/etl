package com.etl.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

/**
 * 加密工具类
 */
public class EncryptionUtil {

    private static final String DEFAULT_KEY = "ETLSYNC2026KEY!!";

    private static final AES AES_CIPHER = SecureUtil.aes(DEFAULT_KEY.getBytes(StandardCharsets.UTF_8));

    private EncryptionUtil() {}

    /**
     * 加密
     */
    public static String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return plainText;
        }
        return AES_CIPHER.encryptHex(plainText);
    }

    /**
     * 解密
     */
    public static String decrypt(String cipherText) {
        if (StrUtil.isBlank(cipherText)) {
            return cipherText;
        }
        try {
            return AES_CIPHER.decryptStr(cipherText);
        } catch (Exception e) {
            // 如果解密失败，可能是明文，直接返回
            return cipherText;
        }
    }

    /**
     * 判断是否已加密
     */
    public static boolean isEncrypted(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        try {
            AES_CIPHER.decryptStr(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
