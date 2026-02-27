package ocean.RedWhale;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import android.util.Base64;
import android.util.Log;

/**
 * 通信メッセージをAES-GCMアルゴリズムで暗号化・復号化するクラスです。
 * これにより、データが盗み見られたり、改ざんされたりすることを防ぎます。
 * エンドツーエンド暗号化（E2EE）の基盤となります。
 */
public class EncryptionManager {
    private static final String TAG = "EncryptionManager";
    
    // 暗号化のアルゴリズムとモード（GCMは改ざん検知機能が含まれており安全です）
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    
    // 初期化ベクトル（IV）の長さ。GCMモードでは通常12バイトを使用します。
    private static final int GCM_IV_LENGTH = 12;
    
    // 認証タグ（改ざんチェック用データ）の長さ。
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * 指定されたAESキー（共通鍵）を使用して、平文（テキスト）を暗号化します。
     * 本来のアプリでは、RSA公開鍵やECDH（鍵交換）を使ってこの共通鍵を共有します。
     *
     * @param plaintext 暗号化したい元のメッセージ
     * @param keyBytes  暗号化に使用するキーのデータ（バイト配列）
     * @return Base64エンコードされた暗号化済み文字列（IVを含む）
     */
    public static String encrypt(String plaintext, byte[] keyBytes) {
        try {
            // 提供されたバイトデータから、AES暗号化用の秘密鍵（共通鍵）を作ります。
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            
            // 安全なランダムな値（IV: 初期化ベクトル）を生成します。
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 暗号化の準備（Cipherの初期化）を行います。
            Cipher cipher = Cipher.getInstance(AES_MODE);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            // 実際にメッセージを暗号化（バイトデータに変換）します。
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
            
            // 復号化するにはIVが必要なため、暗号化データの先頭にIVを結合します。
            // [IV (12 bytes)] + [暗号化データ] の形になります。
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            // 通信しやすいようにBase64文字列に変換して返します。
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "暗号化に失敗しました", e);
            return null;
        }
    }

    /**
     * 暗号化されたメッセージを受け取り、指定されたキー（共通鍵）で復号化して元のテキストに戻します。
     *
     * @param encodedCiphertext 暗号化されたBase64文字列（IVを含む）
     * @param keyBytes          復号化に使用するキーのデータ
     * @return 復号化された平文（元のメッセージ）、失敗時はnull
     */
    public static String decrypt(String encodedCiphertext, byte[] keyBytes) {
        try {
            // 受信したBase64文字列をバイトデータに戻します。
            byte[] combined = Base64.decode(encodedCiphertext, Base64.NO_WRAP);
            
            // 復号化に使うAESキーをセットします。
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // 先頭の12バイトからIV（初期化ベクトル）を抽出します。
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            // 復号化の準備（Cipherの初期化）を行います。
            Cipher cipher = Cipher.getInstance(AES_MODE);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // IVの分を飛ばして、残りの部分（暗号化された実データ）を復号化します。
            byte[] plaintextBytes = cipher.doFinal(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH);
            
            // 復号化されたバイトデータを文字列（テキスト）に戻して返します。
            return new String(plaintextBytes);
        } catch (Exception e) {
            Log.e(TAG, "復号化に失敗しました。キーが違うか、データが破損しています", e);
            return null;
        }
    }

    /**
     * セッション（一度の通信）ごとに使う、ランダムなAESキーを生成します。
     *
     * @return ランダムに生成された256ビット（32バイト）のキー
     */
    public static byte[] generateRandomKey() {
        try {
            // AESアルゴリズムを使って256ビットの強力なキーを生成します。
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey().getEncoded();
        } catch (Exception e) {
            // 万が一生成に失敗した場合の保険（推奨はされません）
            return new byte[32];
        }
    }

    /**
     * AESキーを相手のRSA公開鍵で暗号化します（キー交換用）。
     */
    public static byte[] encryptAESKeyWithRSA(byte[] aesKey, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_MODE);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(aesKey);
        } catch (Exception e) {
            Log.e(TAG, "RSAによるAESキーの暗号化に失敗しました", e);
            return null;
        }
    }

    /**
     * 暗号化されたAESキーを自分のRSA秘密鍵で復号化します。
     */
    public static byte[] decryptAESKeyWithRSA(byte[] encryptedAesKey, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_MODE);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(encryptedAesKey);
        } catch (Exception e) {
            Log.e(TAG, "RSAによるAESキーの復号化に失敗しました", e);
            return null;
        }
    }

    /**
     * 自分のRSA秘密鍵を使ってデータにデジタル署名を作成します（改ざん防止）。
     */
    public static byte[] signData(byte[] data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            Log.e(TAG, "デジタル署名の作成に失敗しました", e);
            return null;
        }
    }

    /**
     * 相手のRSA公開鍵を使って、データのデジタル署名を検証します。
     */
    public static boolean verifySignature(byte[] data, byte[] signatureBytes, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            Log.e(TAG, "デジタル署名の検証に失敗しました", e);
            return false;
        }
    }
}