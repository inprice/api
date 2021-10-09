package io.inprice.api.helpers;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.config.Props;

/**
 * Copied from
 * https://stackoverflow.com/questions/2860943/how-can-i-hash-a-password-in-java
 */
public class PasswordHelper {

  private static SecureRandom sr;
  private static SecretKeyFactory skf;

  /**
   * Computes a salted PBKDF2 hash of given plaintext password suitable for
   * storing in a database.
   */
  public static String getSaltedHash(String password) {
  	if (StringUtils.isBlank(password)) return null;

    try {
      if (sr == null) {
        sr = SecureRandom.getInstance("SHA1PRNG");
      }
      byte[] seed = sr.generateSeed(32);
      String hash = hash(password, seed);
      String salt = Base64.getEncoder().encodeToString(seed);
      return (hash+salt).replaceAll("=", "");
    } catch (Exception e) {
      System.err.println("Failed to generate salted hash for password!");
    }
    return null;
  }

  public static boolean isValid(String password, String saltedHash) {
    if (StringUtils.isBlank(password) || StringUtils.isBlank(saltedHash)) return false;

    String hash = saltedHash.substring(0, 43) + "=";
    String salt = saltedHash.substring(43) + "=";

    String hashOfInput = hash(password, Base64.getDecoder().decode(salt));
    return hashOfInput.equals(hash);
  }

  /**
   * using PBKDF2 from Sun
   */
  private static String hash(String password, byte[] salt) {
  	if (StringUtils.isBlank(password)) return null;

  	try {
      if (skf == null) {
        skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      }
      SecretKey key = skf.generateSecret(new PBEKeySpec(password.toCharArray(), salt, Props.getConfig().APP.SALT_ROUNDS * 1000, 256));
      return Base64.getEncoder().encodeToString(key.getEncoded());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public static void main(String[] args) {
		System.out.println(isValid("1234-AB", "eLgUOcQnH/Twai9hJF4Ing25yXoR2eGA0DseixPycjcTb//WqlbdEct3rykdJI7MAmoO2MBDBaAoVYGsV7LLuo"));
	}

}
