package ocean.RedWhale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * チャットメッセージのリストを表示するためのカスタムArrayAdapterです。
 * 送信メッセージと受信メッセージで異なるレイアウトを使用します。
 */
public class ChatAdapter extends ArrayAdapter<ChatMessage> {

    // 送信メッセージのビュータイプ
    private static final int VIEW_TYPE_SENT = 0;
    // 受信メッセージのビュータイプ
    private static final int VIEW_TYPE_RECEIVED = 1;

    public ChatAdapter(@NonNull Context context, List<ChatMessage> messages) {
        super(context, 0, messages);
    }

    /**
     * リストビューが使用するビューの種類の数を返します。
     * @return ビューの種類の数（送信/受信の2種類）
     */
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /**
     * 指定された位置のアイテムのビュータイプを返します。
     * @param position アイテムの位置
     * @return 送信メッセージならVIEW_TYPE_SENT、受信メッセージならVIEW_TYPE_RECEIVED
     */
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message != null && message.isSentByUser()) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    /**
     * データセット内の指定された位置にあるデータを表示するビューを取得します。
     * @param position ビューを生成するアイテムの位置
     * @param convertView 再利用する古いビュー（nullの場合もある）
     * @param parent このビューが最終的にアタッチされる親ビュー
     * @return データに対応するビュー
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ChatMessage message = getItem(position);
        if (message == null) {
            // メッセージがnullの場合は空のビューを返す
            return new View(getContext());
        }

        int viewType = getItemViewType(position);

        // convertViewがnullの場合のみ、新しいビューをインフレートする
        if (convertView == null) {
            if (viewType == VIEW_TYPE_SENT) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_message_sent, parent, false);
            } else {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_message_received, parent, false);
            }
        }

        // ビュータイプに基づいてメッセージテキストを設定
        if (viewType == VIEW_TYPE_SENT) {
            TextView tvMessage = convertView.findViewById(R.id.tv_message_sent);
            tvMessage.setText(message.getMessage());
        } else {
            TextView tvMessage = convertView.findViewById(R.id.tv_message_received);
            tvMessage.setText(message.getMessage());
        }

        return convertView;
    }
}
