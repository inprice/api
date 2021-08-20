package io.inprice.api.utils;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import io.inprice.api.config.Props;

/**
 * 
 * https://howtodoinjava.com/java/java-security/java-aes-encryption-example/
 *
 */
public class AES {

	private static SecretKeySpec secretKey;
	private static byte[] key;

	static {
		final byte[] ENCRYPTION_KEY = Props.getConfig().KEYS.ENCRYPTION.getBytes();
		if (ENCRYPTION_KEY == null) {
			System.err.println("Encryption key is empty!");
			System.exit(-1);
		}

		MessageDigest sha = null;
		try {
			sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(ENCRYPTION_KEY);
			key = Arrays.copyOf(key, 16);
			secretKey = new SecretKeySpec(key, "AES");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String encrypt(String decrypted) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return Base64.getEncoder().encodeToString(cipher.doFinal(decrypted.getBytes("UTF-8")));
		} catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return null;
	}

	public static String decrypt(String encrypted) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
		} catch (Exception e) {
			System.out.println("Error while decrypting: " + e.toString());
		}
		return null;
	}

}