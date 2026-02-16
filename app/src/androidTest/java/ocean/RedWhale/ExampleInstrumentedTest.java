package ocean.RedWhale;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * インストルメンテーションテスト（Androidデバイス上で実行されるテスト）。
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // テスト対象アプリのコンテキストを取得
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // パッケージ名が期待通りであるかを確認
        assertEquals("ocean.RedWhale", appContext.getPackageName());
    }
}
