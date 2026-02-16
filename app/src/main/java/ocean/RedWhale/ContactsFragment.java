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

import java.util.List;

/**
 * 保存されている連絡先（友達）のリストを表示するためのフラグメントです。
 */
public class ContactsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private List<Friend> friendList;
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
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_contacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // データベースヘルパーを初期化し、すべての友達を取得
        dbHelper = new DatabaseHelper(getContext());
        friendList = dbHelper.getAllFriends();

        // アダプターを作成し、RecyclerViewに設定
        adapter = new ContactsAdapter(getContext(), friendList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}