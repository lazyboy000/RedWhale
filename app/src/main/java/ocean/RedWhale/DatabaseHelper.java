
package ocean.RedWhale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 友達リストとチャットメッセージのローカルストレージを管理するためのデータベースヘルパークラスです。
 * SQLiteを使用してデータを永続化します。
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // データベース情報
    private static final String DATABASE_NAME = "RedWhale.db";
    private static final int DATABASE_VERSION = 1;

    // テーブル名
    private static final String TABLE_FRIENDS = "friends";
    private static final String TABLE_MESSAGES = "messages";

    // 'friends' テーブルのカラム
    private static final String KEY_FRIEND_ID = "id";
    private static final String KEY_FRIEND_NAME = "name";
    private static final String KEY_FRIEND_ADDRESS = "address";

    // 'messages' テーブルのカラム
    private static final String KEY_MESSAGE_ID = "id";
    private static final String KEY_MESSAGE_FROM_ADDRESS = "from_address";
    private static final String KEY_MESSAGE_TO_ADDRESS = "to_address";
    private static final String KEY_MESSAGE_CONTENT = "content";
    private static final String KEY_MESSAGE_TIMESTAMP = "timestamp";
    private static final String KEY_MESSAGE_IS_SENT = "is_sent";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * データベースが初めて作成されるときに呼び出されます。
     * ここでテーブルの作成と初期データの投入が行われます。
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_FRIENDS_TABLE = "CREATE TABLE " + TABLE_FRIENDS +
                "(" +
                KEY_FRIEND_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_FRIEND_NAME + " TEXT," +
                KEY_FRIEND_ADDRESS + " TEXT UNIQUE" + // アドレスはユニーク
                ")";

        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES +
                "(" +
                KEY_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_MESSAGE_FROM_ADDRESS + " TEXT," +
                KEY_MESSAGE_TO_ADDRESS + " TEXT," +
                KEY_MESSAGE_CONTENT + " TEXT," +
                KEY_MESSAGE_TIMESTAMP + " INTEGER," +
                KEY_MESSAGE_IS_SENT + " INTEGER" + // 1なら送信、0なら受信
                ")";

        db.execSQL(CREATE_FRIENDS_TABLE);
        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    /**
     * データベースがアップグレードされる必要があるときに呼び出されます。
     * @param db The database.
     * @param oldVersion 古いデータベースのバージョン
     * @param newVersion 新しいデータベースのバージョン
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // シンプルなアップグレード戦略：古いテーブルを削除して再作成
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FRIENDS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
            onCreate(db);
        }
    }

    /**
     * データベースに友達を追加します。同じアドレスの友達が既に存在する場合は更新します。
     * @param friend 追加する友達オブジェクト
     * @return 新しく挿入された行のID、またはエラーの場合は-1
     */
    public long addFriend(Friend friend) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_FRIEND_NAME, friend.getName());
        values.put(KEY_FRIEND_ADDRESS, friend.getAddress());

        // CONFLICT_REPLACEにより、同じアドレスが存在すればUPDATEされる
        long id = db.insertWithOnConflict(TABLE_FRIENDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return id;
    }

    /**
     * データベースからすべての友達のリストを取得します。
     * @return すべての友達のリスト
     */
    public List<Friend> getAllFriends() {
        List<Friend> friends = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_FRIENDS;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(KEY_FRIEND_ID);
                int nameIndex = cursor.getColumnIndex(KEY_FRIEND_NAME);
                int addressIndex = cursor.getColumnIndex(KEY_FRIEND_ADDRESS);

                if (idIndex != -1 && nameIndex != -1 && addressIndex != -1) {
                    Friend friend = new Friend(
                            cursor.getInt(idIndex),
                            cursor.getString(nameIndex),
                            cursor.getString(addressIndex)
                    );
                    friends.add(friend);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return friends;
    }

    /**
     * チャットメッセージをデータベースに追加します。
     * @param chatMessage 追加するメッセージオブジェクト
     * @param fromAddress 送信者のアドレス
     * @param toAddress 受信者のアドレス
     */
    public void addMessage(ChatMessage chatMessage, String fromAddress, String toAddress) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MESSAGE_FROM_ADDRESS, fromAddress);
        values.put(KEY_MESSAGE_TO_ADDRESS, toAddress);
        values.put(KEY_MESSAGE_CONTENT, chatMessage.getMessage());
        values.put(KEY_MESSAGE_TIMESTAMP, System.currentTimeMillis());
        values.put(KEY_MESSAGE_IS_SENT, chatMessage.isSentByUser() ? 1 : 0);

        db.insert(TABLE_MESSAGES, null, values);
        db.close();
    }

    /**
     * 指定された2つのデバイス間のすべてのメッセージを取得します。
     * @param address1 最初のデバイスのアドレス
     * @param address2 2番目のデバイスのアドレス
     * @return メッセージのリスト
     */
    public List<ChatMessage> getMessages(String address1, String address2) {
        List<ChatMessage> messages = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " +
                "(" + KEY_MESSAGE_FROM_ADDRESS + " = ? AND " + KEY_MESSAGE_TO_ADDRESS + " = ?) OR " +
                "(" + KEY_MESSAGE_FROM_ADDRESS + " = ? AND " + KEY_MESSAGE_TO_ADDRESS + " = ?) " +
                "ORDER BY " + KEY_MESSAGE_TIMESTAMP + " ASC";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{address1, address2, address2, address1});

        if (cursor.moveToFirst()) {
            do {
                int contentIndex = cursor.getColumnIndex(KEY_MESSAGE_CONTENT);
                int isSentIndex = cursor.getColumnIndex(KEY_MESSAGE_IS_SENT);

                if (contentIndex != -1 && isSentIndex != -1) {
                    ChatMessage message = new ChatMessage(
                            cursor.getString(contentIndex),
                            cursor.getInt(isSentIndex) == 1
                    );
                    messages.add(message);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return messages;
    }

    /**
     * 現在のユーザーの最近のチャットリストを取得します。
     * @param currentUserAddress 現在のユーザーのBluetoothアドレス
     * @return 最近のチャットのリスト
     */
    public List<RecentChat> getRecentChats(String currentUserAddress) {
        List<RecentChat> recentChats = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // ユーザーがチャットしたすべてのユニークな友達のアドレスを取得
        String subQuery = "SELECT " + KEY_MESSAGE_FROM_ADDRESS + " FROM " + TABLE_MESSAGES + " WHERE " + KEY_MESSAGE_TO_ADDRESS + " = '" + currentUserAddress + "' " +
                "UNION SELECT " + KEY_MESSAGE_TO_ADDRESS + " FROM " + TABLE_MESSAGES + " WHERE " + KEY_MESSAGE_FROM_ADDRESS + " = '" + currentUserAddress + "'";
        Cursor friendAddressCursor = db.rawQuery(subQuery, null);

        if (friendAddressCursor.moveToFirst()){
            do {
                int addressIndex = friendAddressCursor.getColumnIndex(KEY_MESSAGE_FROM_ADDRESS);
                if(addressIndex != -1) {
                    String friendAddress = friendAddressCursor.getString(addressIndex);

                    // 各友達との最後のメッセージを取得
                    String lastMessageQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " +
                            "(" + KEY_MESSAGE_FROM_ADDRESS + " = ? AND " + KEY_MESSAGE_TO_ADDRESS + " = ?) OR " +
                            "(" + KEY_MESSAGE_FROM_ADDRESS + " = ? AND " + KEY_MESSAGE_TO_ADDRESS + " = ?) " +
                            "ORDER BY " + KEY_MESSAGE_TIMESTAMP + " DESC LIMIT 1";
                    Cursor lastMessageCursor = db.rawQuery(lastMessageQuery, new String[]{currentUserAddress, friendAddress, friendAddress, currentUserAddress});

                    if (lastMessageCursor.moveToFirst()) {
                        int contentIndex = lastMessageCursor.getColumnIndex(KEY_MESSAGE_CONTENT);
                        int timestampIndex = lastMessageCursor.getColumnIndex(KEY_MESSAGE_TIMESTAMP);

                        if(contentIndex != -1 && timestampIndex != -1) {
                            String lastMessage = lastMessageCursor.getString(contentIndex);
                            long timestamp = lastMessageCursor.getLong(timestampIndex);

                            // 友達の名前を取得
                            String friendNameQuery = "SELECT " + KEY_FRIEND_NAME + " FROM " + TABLE_FRIENDS + " WHERE " + KEY_FRIEND_ADDRESS + " = ?";
                            Cursor friendNameCursor = db.rawQuery(friendNameQuery, new String[]{friendAddress});
                            String friendName = "Unknown"; // デフォルト名
                            if (friendNameCursor.moveToFirst()) {
                                int nameIndex = friendNameCursor.getColumnIndex(KEY_FRIEND_NAME);
                                if (nameIndex != -1) {
                                    friendName = friendNameCursor.getString(nameIndex);
                                }
                            }
                            friendNameCursor.close();

                            recentChats.add(new RecentChat(friendName, lastMessage, timestamp, friendAddress));
                        }
                    }
                    lastMessageCursor.close();
                }
            } while (friendAddressCursor.moveToNext());
        }
        friendAddressCursor.close();
        db.close();

        return recentChats;
    }
}
