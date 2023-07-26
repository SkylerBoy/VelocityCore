package es.virtualplanet.velocitycore.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Random;

public class Utils {

    public static String sha256(final String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    public static String generateCode(int length, boolean numbers, boolean letters) {
        String chars = "";

        if (numbers) chars += "0123456789";
        if (letters) chars += "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(chars.length());
            sb.append(chars.charAt(randomIndex));
        }

        return sb.toString();
    }

    public static void replaceValue(Map<String, Boolean> map, String key, Boolean value) {
        if (map.containsKey(key)) map.replace(key, value);
        else map.put(key, value);
    }

    public static String getDateFormatted(Instant instant) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date.from(instant));
    }
}
