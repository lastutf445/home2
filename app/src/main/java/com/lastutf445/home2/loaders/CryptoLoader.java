package com.lastutf445.home2.loaders;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoLoader {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    @Nullable
    private volatile static BigInteger modulus = null, pubExp = null;
    @Nullable
    private volatile static SecretKeySpec AESKey = null;
    private volatile static SecureRandom secureRandom;
    private final static int IV_LENGTH = 16;

    public static void init() {
        String encoded_key = UserLoader.getAESKey();
        AESKey = null;

        if (encoded_key != null) {
            setAESKey(encoded_key);
        }

        if (secureRandom == null) {
            try {
                secureRandom = SecureRandom.getInstance("SHA1PRNG");

            } catch (NoSuchAlgorithmException e) {
                secureRandom = new SecureRandom();
            }

            secureRandom.setSeed(
                    Calendar.getInstance().getTimeInMillis()
            );
        }
    }

    public static void clearRSA() {
        Log.d("LOGTAG", "clearRSA");
        modulus = null;
        pubExp = null;
    }

    public static void setAESKey(@NonNull String key) {
        byte[] raw_key = Base64.decode(key, Base64.NO_WRAP);

        if (raw_key != null) {
            AESKey = new SecretKeySpec(raw_key, "AES");
        }
    }

    public static String createAESKey() {
        Log.d("LOGTAG", "create aes key: " + DataLoader.getInt("AESBytes", 16));
        // it will work, i promise, just not now

        int bits = DataLoader.getInt("AESBytes", 16);
        byte[] bytes = new byte[bits];
        secureRandom.nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static boolean hasAESKey() {
        return AESKey != null;
    }

    public static void setPublicKey(@NonNull String modulus, @NonNull String pubExp) throws NumberFormatException {
        setPublicKey(new BigInteger(modulus, 16), new BigInteger(pubExp, 16));
    }

    public static void setPublicKey(@NonNull BigInteger modulus, @NonNull BigInteger pubExp) {
        CryptoLoader.modulus = modulus;
        CryptoLoader.pubExp = pubExp;
    }

    public static boolean isPublicKeyValid() {
        return isPublicKeyValid(modulus, pubExp);
    }

    public static boolean isPublicKeyValid(@Nullable BigInteger modulus, @Nullable BigInteger pubExp) {
        if (modulus == null || pubExp == null) return false;

        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, pubExp);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            kf.generatePublic(keySpec);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isPublicKeyValid(@Nullable String raw_modulus, @Nullable String raw_pubExp) {
        if (raw_modulus == null || raw_pubExp == null) return false;

        try {
            BigInteger modulus = new BigInteger(raw_modulus, 16);
            BigInteger pubExp = new BigInteger(raw_pubExp, 16);
            return isPublicKeyValid(modulus, pubExp);

        } catch (NumberFormatException e) {
            //e.printStackTrace();
            return false;
        }
    }

    public static String RSAEncrypt(@NonNull String msg) {
        if (modulus == null || pubExp == null) return null;

        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, pubExp);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA-1AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] bytes = cipher.doFinal(msg.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(bytes, Base64.NO_WRAP);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();

        } catch (InvalidKeySpecException e) {
            e.printStackTrace();

        } catch (NoSuchPaddingException e) {
            e.printStackTrace();

        } catch (InvalidKeyException e) {
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Nullable
    public static String AESEncrypt(@NonNull String msg) {
        try {
            byte[] ivBytes = new byte[IV_LENGTH / 2];
            secureRandom.nextBytes(ivBytes);
            String iv = bytesToHex(ivBytes);
            ivBytes = iv.getBytes(StandardCharsets.UTF_8);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, AESKey, ivParameterSpec);

            byte[] encrypted = cipher.doFinal(msg.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(ivBytes.length + encrypted.length);
            byteBuffer.put(ivBytes);
            byteBuffer.put(encrypted);

            String result = Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public static String AESDecrypt(String msg) {
        try {
            byte[] encrypted = Base64.decode(msg, Base64.NO_WRAP);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(encrypted, 0, IV_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, AESKey, ivParameterSpec);

            String result = new String(cipher.doFinal(encrypted, IV_LENGTH, encrypted.length - IV_LENGTH));
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; ++i) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }
}
