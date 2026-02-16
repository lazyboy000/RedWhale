package ocean.RedWhale;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 設定画面を表示するためのフラグメントです。
 */
public class SettingsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private List<SettingsItem> settingsList;

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
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_settings);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 表示する設定項目の静的なリストを作成
        settingsList = new ArrayList<>();
        settingsList.add(new SettingsItem(android.R.drawable.ic_menu_save, "Storage", "Manage local storage"));
        settingsList.add(new SettingsItem(android.R.drawable.ic_lock_lock, "Security", "Encryption and privacy settings"));
        settingsList.add(new SettingsItem(android.R.drawable.ic_menu_help, "Help", "FAQ, contact support"));

        // アダプターを作成し、RecyclerViewに設定
        adapter = new SettingsAdapter(getContext(), settingsList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}