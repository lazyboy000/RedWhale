package ocean.RedWhale;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 「通話」画面のフラグメントです。
 * 現在はプレースホルダーとして機能します。
 */
public class CallsFragment extends Fragment {

    /**
     * フラグメントのUIを生成するために呼び出されます。
     * @param inflater レイアウトをインフレートするためのLayoutInflater
     * @param container 親ビューグループ
     * @param savedInstanceState 保存された状態
     * @return フラグメントのルートビュー
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 対応するレイアウトファイルをインフレートして返す
        return inflater.inflate(R.layout.fragment_calls, container, false);
    }
}