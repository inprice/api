package io.inprice.scrapper.api.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class Cryptor {

    private static final Logger log = LoggerFactory.getLogger(Cryptor.class);

    private static final String METHOD = "Blowfish";

    public static byte[] encrypt(String clearText) {
        try {
            SecretKeySpec key = new SecretKeySpec(Consts.Auth.DATA_SECRET_KEY.getBytes(StandardCharsets.UTF_8), METHOD);
            Cipher cipher = Cipher.getInstance(METHOD);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(clearText.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
        }

        return null;
    }

    public static String decrypt(byte[] encryptedData) {
        try {
            SecretKeySpec key = new SecretKeySpec(Consts.Auth.DATA_SECRET_KEY.getBytes(StandardCharsets.UTF_8), METHOD);
            Cipher cipher = Cipher.getInstance(METHOD);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(encryptedData);
            return new String(decrypted);
        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
        }

        return null;
    }

}
