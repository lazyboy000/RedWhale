package ocean.RedWhale;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * 連絡先リスト（友達リスト）をRecyclerViewに表示するためのアダプターです。
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private final List<Friend> friendList;
    private final Context context;

    public ContactsAdapter(Context context, List<Friend> friendList) {
        this.context = context;
        this.friendList = friendList;
    }

    /**
     * 新しいViewHolderが作成されるときに呼び出されます。
     * @param parent 親ビューグループ
     * @param viewType ビュータイプ
     * @return 新しいContactViewHolder
     */
    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    /**
     * ViewHolderにデータをバインド（設定）するときに呼び出されます。
     * @param holder データを設定するViewHolder
     * @param position データリスト内の位置
     */
    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        holder.name.setText(friend.getName());
        holder.address.setText(friend.getAddress());

        // アイテムがクリックされたら、その連絡先とのチャット画面を開く
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("deviceAddress", friend.getAddress());
            context.startActivity(intent);
        });
    }

    /**
     * リスト内のアイテムの総数を返します。
     * @return アイテムの総数
     */
    @Override
    public int getItemCount() {
        return friendList.size();
    }

    /**
     * リストの各アイテムのビューを保持するためのViewHolderクラスです。
     */
    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView name, address;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contact_name);
            address = itemView.findViewById(R.id.contact_address);
        }
    }
}
