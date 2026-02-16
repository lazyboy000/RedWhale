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

import java.util.List;

/**
 * 最近のチャットリストを表示するためのフラグメントです。
 */
public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecentChatsAdapter adapter;
    private List<RecentChat> recentChats;
    private DatabaseHelper dbHelper;

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

        dbHelper = new DatabaseHelper(getContext());

        FloatingActionButton fab = view.findViewById(R.id.fab_new_chat);
        // フローティングアクションボタン（FAB）のクリックリスナー
        fab.setOnClickListener(v -> {
            // FABがクリックされたら、新しいチャットを開始するためにDeviceListActivityに移動
            Toast.makeText(getContext(), "Select a device to start a new chat", Toast.LENGTH_SHORT).show();
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
        if (bluetoothAdapter != null) {
            // 現在のユーザーのBluetoothアドレスを取得
            @SuppressLint("MissingPermission") String currentUserAddress = bluetoothAdapter.getAddress();
            // データベースヘルパーを使って最近のチャットを取得
            recentChats = dbHelper.getRecentChats(currentUserAddress);
            // アダプターを作成してRecyclerViewに設定
            adapter = new RecentChatsAdapter(getContext(), recentChats);
            recyclerView.setAdapter(adapter);
        }
    }
}