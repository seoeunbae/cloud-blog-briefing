package gcp.cloudblog_mailing.util;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class EmailEncoder {
    private static final String SECRET_KEY = "YourSecretKeyHere"; // Use a strong, secure key

    public static String encryptEmail(String email) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // Use only first 16 bytes

            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytes = cipher.doFinal(email.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder()
                    .encodeToString(encryptedBytes)
                    .replace("=", ""); // Remove padding for URL safety
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting email", e);
        }
    }

    public static String decryptEmail(String encryptedEmail) {
        try {
            String padded = encryptedEmail + "===".substring(0, (4 - encryptedEmail.length() % 4) % 4);

            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);

            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decryptedBytes = cipher.doFinal(Base64.getUrlDecoder().decode(padded));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting email", e);
        }
    }

}