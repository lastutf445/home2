package com.lastutf445.home2.loaders;

import android.icu.util.ULocale;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
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

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoLoader {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final int INIT_VECTOR_LENGTH = 16;
    private SecretKeySpec AESKey;


    public void init() {
        String raw_key = UserLoader.getKey();

        if (raw_key != null) {
            AESKey = new SecretKeySpec(raw_key.getBytes(StandardCharsets.UTF_8), "AES");
        }
    }

    public String RSAEncode(String msg, BigInteger modulus, BigInteger pubExp) {
        try {
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, pubExp);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] bytes = cipher.doFinal(msg.getBytes(StandardCharsets.UTF_8));
            return new String(bytes);

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

    public String AESEncode(String msg) {
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] ivBytes = new byte[INIT_VECTOR_LENGTH / 2];

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

            String result = Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String AESDecode(String msg) {
        try {
            byte[] encrypted = Base64.decode(msg, Base64.DEFAULT);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(encrypted, 0, INIT_VECTOR_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, AESKey, ivParameterSpec);

            String result = new String(cipher.doFinal(encrypted, INIT_VECTOR_LENGTH, encrypted.length - INIT_VECTOR_LENGTH));
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
