package ocean.RedWhale;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * 最近のチャットリストをRecyclerViewに表示するためのアダプターです。
 */
public class RecentChatsAdapter extends RecyclerView.Adapter<RecentChatsAdapter.RecentChatViewHolder> {

    private final List<RecentChat> recentChats;
    private final Context context;

    public RecentChatsAdapter(Context context, List<RecentChat> recentChats) {
        this.context = context;
        this.recentChats = recentChats;
    }

    /**
     * 新しいViewHolderが作成されるときに呼び出されます。
     * @param parent 親ビューグループ
     * @param viewType ビュータイプ
     * @return 新しいRecentChatViewHolder
     */
    @NonNull
    @Override
    public RecentChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // list_item_chatレイアウトを共有して使用
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_chat, parent, false);
        return new RecentChatViewHolder(view);
    }

    /**
     * ViewHolderにデータをバインド（設定）するときに呼び出されます。
     * @param holder データを設定するViewHolder
     * @param position データリスト内の位置
     */
    @Override
    public void onBindViewHolder(@NonNull RecentChatViewHolder holder, int position) {
        RecentChat chat = recentChats.get(position);
        holder.name.setText(chat.getFriendName());
        holder.lastMessage.setText(chat.getLastMessage());
        // タイムスタンプを「x分前」のような相対的な時間で表示
        holder.timestamp.setText(DateUtils.getRelativeTimeSpanString(chat.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

        // アイテムクリックでチャット画面に遷移
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("deviceAddress", chat.getFriendAddress());
            context.startActivity(intent);
        });
    }

    /**
     * リスト内のアイテムの総数を返します。
     * @return アイテムの総数
     */
    @Override
    public int getItemCount() {
        return recentChats.size();
    }

    /**
     * 最近のチャットリストの各アイテムのビューを保持するためのViewHolderクラスです。
     */
    public static class RecentChatViewHolder extends RecyclerView.ViewHolder {
        TextView name, lastMessage, timestamp;

        public RecentChatViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.chat_name);
            lastMessage = itemView.findViewById(R.id.chat_last_message);
            timestamp = itemView.findViewById(R.id.chat_timestamp);
        }
    }
}
