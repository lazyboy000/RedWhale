package ocean.RedWhale;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import android.util.Base64;

/**
 * 暗号化と復号を処理するセキュリティクラスです。
 * 注意：このクラスはサンプルのためのものであり、セキュアではありません。
 */
public class Security {

    private static final String ALGORITHM = "AES";
    // 重要：これは非常に危険な実装です。実際のアプリケーションでは、このようにキーをハードコーディングしないでください。
    // このキーはアプリケーションから簡単に抽出可能であり、暗号化が無意味になります。
    // This should be a securely exchanged key in a real application.
    private static final String SECRET_KEY = "YourSecretKey123";

    /**
     * 文字列をAESアルゴリズムで暗号化します。
     *
     * @param valueToEnc 暗号化する文字列
     * @return Base64でエンコードされた暗号化後の文字列
     * @throws Exception 暗号化に失敗した場合eit
     */
    public static String encrypt(String valueToEnc) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encValue = c.doFinal(valueToEnc.getBytes());
        return Base64.encodeToString(encValue, Base64.DEFAULT);
    }

    /**
     * AESで暗号化された文字列を復号します。
     *
     * @param encryptedValue Base64でエンコードされた暗号化文字列
     * @return 復号された元の文字列
     * @throws Exception 復号に失敗した場合
     */
    public static String decrypt(String encryptedValue) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = Base64.decode(encryptedValue, Base64.DEFAULT);
        byte[] decValue = c.doFinal(decordedValue);
        return new String(decValue);
    }

    /**
     * ハードコードされたSECRET_KEY文字列から秘密鍵を生成します。
     *
     * @return 生成された秘密鍵
     */
    private static Key generateKey() {
        return new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
    }
}
