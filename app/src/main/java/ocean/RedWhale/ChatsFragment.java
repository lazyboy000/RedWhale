package ocean.RedWhale;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 最近のチャットリストを表示するためのフラグメントです。
 */
public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecentChatsAdapter adapter;
    private List<RecentChat> recentChats;
    private DatabaseHelper dbHelper;
    private View emptyState;

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
        // 対応するレイアウトファイルをインフレート
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize with empty list and attach adapter immediately to prevent "No adapter attached" error
        recentChats = new ArrayList<>();
        adapter = new RecentChatsAdapter(getContext(), recentChats);
        recyclerView.setAdapter(adapter);

        emptyState = view.findViewById(R.id.tv_empty_chats);

        dbHelper = new DatabaseHelper(getContext());

        FloatingActionButton fab = view.findViewById(R.id.fab_new_chat);
        // フローティングアクションボタン（FAB）のクリックリスナー
        fab.setOnClickListener(v -> {
            // FABがクリックされたら、新しいチャットを開始するためにDeviceListActivityに移動
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.msg_select_device_new_chat, Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(getActivity(), DeviceListActivity.class);
            // DeviceListActivityからの結果は、親のHomeActivityのonActivityResultで処理される
            getActivity().startActivityForResult(intent, 102);
        });

        return view;
    }

    /**
     * フラグメントがユーザーに表示されるたびに呼び出されます。
     */
    @Override
    public void onResume() {
        super.onResume();
        // 最近のチャットリストを更新
        loadRecentChats();
    }

    /**
     * データベースから最近のチャットを取得し、RecyclerViewに表示します。
     */
    private void loadRecentChats() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) return;

        @SuppressLint("MissingPermission") String currentUserAddress = bluetoothAdapter.getAddress();
        
        // データベース操作をバックグラウンドスレッドで実行
        new Thread(() -> {
            List<RecentChat> loadedChats = dbHelper.getRecentChats(currentUserAddress);
            
            // UIの更新はメインスレッドで行う
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    recentChats.clear();
                    if (loadedChats != null) {
                        recentChats.addAll(loadedChats);
                    }

                    if (recentChats.isEmpty()) {
                        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        if (emptyState != null) emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }
}