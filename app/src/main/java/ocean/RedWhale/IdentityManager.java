package ocean.RedWhale;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAKeyGenParameterSpec;

import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

import java.security.cert.Certificate;

/**
 * ユーザーの暗号化ID（公開鍵と秘密鍵）を管理するクラスです。
 * Android Keystoreを使用して、端末内の安全な領域に鍵を保存します。
 */
public class IdentityManager {
    private static final String TAG = "IdentityManager";
    private static final String KEY_ALIAS = "RedWhaleIdentityKey";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    private final Context context;

    public IdentityManager(Context context) {
        this.context = context;
    }

    /**
     * 暗号化IDが既に作成されているか確認します。
     */
    public boolean exists() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            return keyStore.containsAlias(KEY_ALIAS) && keyStore.getCertificate(KEY_ALIAS) != null;
        } catch (Exception e) {
            Log.e(TAG, "IDの確認に失敗", e);
            return false;
        }
    }

    /**
     * IDが存在しない場合のみ、新しく生成します。
     */
    public void ensureIdentityExists() {
        if (!exists()) {
            generateNewIdentity();
        }
    }

    /**
     * RSA 2048bitの鍵ペアを生成します（時間がかかる処理です）。
     */
    public void generateNewIdentity() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE);

            kpg.initialize(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY |
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setKeySize(2048)
                    .build());

            kpg.generateKeyPair();
            Log.i(TAG, "新しい暗号化IDを生成しました");
        } catch (Exception e) {
            Log.e(TAG, "IDの生成に失敗", e);
        }
    }

    /**
     * 自分の公開鍵（Node IDとして使用）をBase64形式で取得します。
     */
    public String getIdentityAddress() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            
            Certificate cert = keyStore.getCertificate(KEY_ALIAS);
            if (cert == null) return null;
            
            PublicKey publicKey = cert.getPublicKey();
            if (publicKey == null) return null;
            
            return Base64.encodeToString(publicKey.getEncoded(), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Node IDの取得に失敗", e);
            return null;
        }
    }

    /**
     * 文字列形式の公開鍵をオブジェクトに変換します。
     */
    public static PublicKey getPublicKeyFromAddress(String address) {
        try {
            byte[] keyBytes = Base64.decode(address, Base64.NO_WRAP);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            Log.e(TAG, "公開鍵の変換に失敗", e);
            return null;
        }
    }

    /**
     * 保存されている秘密鍵を取得します。
     */
    public PrivateKey getPrivateKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            return (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
        } catch (Exception e) {
            Log.e(TAG, "秘密鍵の取得に失敗", e);
            return null;
        }
    }

    /**
     * 公開鍵から一意のハッシュ値を生成します。
     */
    public static byte[] getAddressHash(String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(address.getBytes());
        } catch (Exception e) {
            return new byte[32];
        }
    }
}