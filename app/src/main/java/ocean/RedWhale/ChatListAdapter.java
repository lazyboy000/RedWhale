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
 * チャットの会話リスト（RecyclerView）を表示するためのアダプターです。
 * 相手のアイコン、名前、最後のメッセージ、時間を1行ずつ並べて表示します。
 */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final List<ChatListItem> chatItems;
    private final Context context;

    public ChatListAdapter(Context context, List<ChatListItem> chatItems) {
        this.context = context;
        this.chatItems = chatItems;
    }

    /**
     * 新しい1行分の見た目（ビュー）を作る必要があるときに呼ばれます。
     * @param parent 親のレイアウト
     * @param viewType 見た目の種類
     * @return 新しく作ったViewHolder（画面部品の入れ物）
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // list_item_chat.xml というデザインファイルを読み込んでビューを作ります
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    /**
     * 作ったビューに、実際のチャットデータをセットします。
     * @param holder データをセットする入れ物
     * @param position 何番目のデータか
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatListItem item = chatItems.get(position);
        holder.name.setText(item.getName());
        holder.lastMessage.setText(item.getLastMessage());
        holder.timestamp.setText(item.getTimestamp());
        holder.avatar.setImageResource(item.getAvatarResId());

        // その行がタップされた時の動作です
        holder.itemView.setOnClickListener(v -> {
            // タップされたらチャット画面（MainActivity）を開きます
            Intent intent = new Intent(context, MainActivity.class);
            // 相手の名前を次の画面に渡します
            intent.putExtra("CHAT_NAME", item.getName());
            context.startActivity(intent);
        });
    }

    /**
     * リストに表示するデータが全部でいくつあるかを返します。
     */
    @Override
    public int getItemCount() {
        return chatItems.size();
    }

    /**
     * 1行分の画面部品（アイコンやテキスト）をキャッシュしておくためのクラスです。
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