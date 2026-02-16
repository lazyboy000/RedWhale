package ocean.RedWhale;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * チャットセッションのリストをRecyclerViewに表示するためのアダプターです。
 * 各項目にはアバター、名前、最終メッセージ、タイムスタンプが含まれます。
 */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final List<ChatListItem> chatItems;
    private final Context context;

    public ChatListAdapter(Context context, List<ChatListItem> chatItems) {
        this.context = context;
        this.chatItems = chatItems;
    }

    /**
     * RecyclerViewによって呼び出され、新しいViewHolderを作成します。
     * @param parent ViewHolderが属するViewGroup
     * @param viewType ビューのタイプ
     * @return 新しいChatViewHolderインスタンス
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // list_item_chatレイアウトをインフレートしてビューを作成
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    /**
     * RecyclerViewによって呼び出され、指定された位置のデータをViewHolderにバインドします。
     * @param holder 更新するViewHolder
     * @param position データセット内のアイテムの位置
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatListItem item = chatItems.get(position);
        holder.name.setText(item.getName());
        holder.lastMessage.setText(item.getLastMessage());
        holder.timestamp.setText(item.getTimestamp());
        holder.avatar.setImageResource(item.getAvatarResId());

        // アイテムがクリックされたときの処理
        holder.itemView.setOnClickListener(v -> {
            // チャットがクリックされたらMainActivityを開く
            Intent intent = new Intent(context, MainActivity.class);
            // オプション：MainActivityにデータを渡す
            intent.putExtra("CHAT_NAME", item.getName());
            context.startActivity(intent);
        });
    }

    /**
     * データセット内のアイテムの総数を返します。
     * @return アイテムの総数
     */
    @Override
    public int getItemCount() {
        return chatItems.size();
    }

    /**
     * リストの各アイテムのビュー（UI要素）を保持するViewHolderクラスです。
     */
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name, lastMessage, timestamp;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.chat_avatar);
            name = itemView.findViewById(R.id.chat_name);
            lastMessage = itemView.findViewById(R.id.chat_last_message);
            timestamp = itemView.findViewById(R.id.chat_timestamp);
        }
    }
}