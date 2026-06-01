package com.example.criptoprojecte;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class CryptoUtils {

    private static final String PREFS_NAME = "CryptoPrefs";
    private static final String KEY_AES = "aes_key";
    private static final String KEY_RSA_PUBLIC = "rsa_public";
    private static final String KEY_RSA_PRIVATE = "rsa_private";
    private static final String KEY_IV = "iv";


    public static KeyPair generateAndSaveKeys(Context context) throws Exception {
        // Genera clave AES
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();

        // Genera IV
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);

        // Genera clave PAR RSA
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // Guarda las llaves en SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_AES, Base64.encodeToString(aesKey.getEncoded(), Base64.DEFAULT));
        editor.putString(KEY_RSA_PUBLIC, Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT));
        editor.putString(KEY_RSA_PRIVATE, Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT));
        editor.putString(KEY_IV, Base64.encodeToString(iv, Base64.DEFAULT));

        editor.apply();

        return keyPair;
    }

    public static SecretKey getSymmetricKey(Context context) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String keyBase64 = prefs.getString(KEY_AES, null);

        if (keyBase64 == null) {
            throw new Exception("Clave AES no encontrada");
        }

        byte[] keyBytes = Base64.decode(keyBase64, Base64.DEFAULT);
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static KeyPair getKeyPair(Context context) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String publicKeyBase64 = prefs.getString(KEY_RSA_PUBLIC, null);
        String privateKeyBase64 = prefs.getString(KEY_RSA_PRIVATE, null);

        if (publicKeyBase64 == null || privateKeyBase64 == null) {
            throw new Exception("Clave RSA no encontrada");
        }

        // Convertir la llave publica
        byte[] publicKeyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        // Convertir la llave privada
        byte[] privateKeyBytes = Base64.decode(privateKeyBase64, Base64.DEFAULT);
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        return new KeyPair(publicKey, privateKey);
    }

    public static byte[] getIV(Context context) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String ivBase64 = prefs.getString(KEY_IV, null);

        if (ivBase64 == null) {
            throw new Exception("IV no encontrado");
        }

        return Base64.decode(ivBase64, Base64.DEFAULT);
    }
}