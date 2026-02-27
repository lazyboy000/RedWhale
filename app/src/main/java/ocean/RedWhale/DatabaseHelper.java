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
 * チャット履歴や連絡先（友達リスト）を端末の内部ストレージに安全に保存・管理するための
 * ローカルデータベースのヘルパークラスです。
 * SQLite（Android標準の軽量データベース）を使用しています。
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // ---------------------------------------------------------
    // データベースとテーブルの設定
    // ---------------------------------------------------------
    private static final String DATABASE_NAME = "RedWhale.db"; // データベースのファイル名
    private static final int DATABASE_VERSION = 1;             // バージョン。仕様が変わった時に上げます。

    // テーブル（データを保存する表）の名前
    private static final String TABLE_FRIENDS = "friends";   // 連絡先用テーブル
    private static final String TABLE_MESSAGES = "messages"; // メッセージ履歴用テーブル

    // 'friends' テーブルの列名（カラム）
    private static final String KEY_FRIEND_ID = "id";                     // 管理用の連番
    private static final String KEY_FRIEND_NAME = "name";                 // 表示名
    private static final String KEY_FRIEND_ADDRESS = "address";           // デバイスのMACアドレス（一意）
    private static final String KEY_FRIEND_IDENTITY = "identity_address"; // 公開鍵（アイデンティティ）

    // 'messages' テーブルの列名（カラム）
    private static final String KEY_MESSAGE_ID = "id";                   // 管理用の連番
    private static final String KEY_MESSAGE_FROM_ADDRESS = "from_address";// 送信元のアドレス
    private static final String KEY_MESSAGE_TO_ADDRESS = "to_address";    // 送信先のアドレス
    private static final String KEY_MESSAGE_CONTENT = "content";         // メッセージ内容（平文または暗号文）
    private static final String KEY_MESSAGE_TIMESTAMP = "timestamp";     // 送受信した時間
    private static final String KEY_MESSAGE_IS_SENT = "is_sent";         // 自分が送信したか（1:送信, 0:受信）

    /**
     * コンストラクタ
     */
    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * アプリが初めてこのデータベースを使うときに呼ばれ、テーブル（表）を作成します。
     * @param db 操作対象のSQLiteデータベース
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 友達テーブルを作るSQL（データベースへの命令）
        String CREATE_FRIENDS_TABLE = "CREATE TABLE " + TABLE_FRIENDS +
                "(" +
                KEY_FRIEND_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + // IDは自動で増える連番
                KEY_FRIEND_NAME + " TEXT," +
                KEY_FRIEND_ADDRESS + " TEXT UNIQUE," +                  // アドレスは重複を許さない（UNIQUE）
                KEY_FRIEND_IDENTITY + " TEXT" +
                ")";

        // メッセージ履歴テーブルを作るSQL
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES +
                "(" +
                KEY_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_MESSAGE_FROM_ADDRESS + " TEXT," +
                KEY_MESSAGE_TO_ADDRESS + " TEXT," +
                KEY_MESSAGE_CONTENT + " TEXT," +
                KEY_MESSAGE_TIMESTAMP + " INTEGER," +
                KEY_MESSAGE_IS_SENT + " INTEGER" + // booleanの代わりに1と0を使います
                ")";

        // 上記のSQLを実行してテーブルを作ります。
        db.execSQL(CREATE_FRIENDS_TABLE);
        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    /**
     * アプリのバージョンアップなどで、データベースの構造（列など）が変わったときに呼ばれます。
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // 現在は古いテーブルを一旦削除して、新しい設計で作り直すシンプルな処理にしています。
            // ※本来はデータを消さずにALTER TABLEで列を追加したりします。
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FRIENDS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
            onCreate(db);
        }
    }

    // ---------------------------------------------------------
    // 友達（連絡先）の操作
    // ---------------------------------------------------------
    
    /**
     * 新しい友達をデータベースに保存します。
     * すでに同じアドレス（デバイス）が登録されている場合は、名前などの情報を上書き更新します。
     *
     * @param friend 保存する友達のデータオブジェクト
     * @return 新しく追加・更新された行のID（失敗した場合は-1）
     */
    public long addFriend(Friend friend) {
        SQLiteDatabase db = getWritableDatabase(); // 書き込み用のDBを開く
        
        ContentValues values = new ContentValues(); // 保存するデータを入れる箱
        values.put(KEY_FRIEND_NAME, friend.getName());
        values.put(KEY_FRIEND_ADDRESS, friend.getAddress());
        values.put(KEY_FRIEND_IDENTITY, friend.getIdentityAddress());

        // CONFLICT_REPLACEを使うことで、アドレスが重複した時は「エラー」ではなく「上書き」にします。
        long id = db.insertWithOnConflict(TABLE_FRIENDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close(); // DBを閉じる
        return id;
    }

    /**
     * データベースに保存されているすべての友達（連絡先）を取得します。
     *
     * @return 友達のリスト（ArrayList）
     */
    public List<Friend> getAllFriends() {
        List<Friend> friends = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_FRIENDS; // 全て取得するSQL
        
        SQLiteDatabase db = getReadableDatabase(); // 読み取り用のDBを開く
        Cursor cursor = db.rawQuery(selectQuery, null); // SQLを実行し、結果をカーソル（イテレータ）で受け取る

        // 最初の行があれば、順番にデータを読み込んでいきます
        if (cursor.moveToFirst()) {
            do {
                // 列が何番目にあるかを調べます
                int idIndex = cursor.getColumnIndex(KEY_FRIEND_ID);
                int nameIndex = cursor.getColumnIndex(KEY_FRIEND_NAME);
                int addressIndex = cursor.getColumnIndex(KEY_FRIEND_ADDRESS);
                int identityIndex = cursor.getColumnIndex(KEY_FRIEND_IDENTITY);

                // データがちゃんとあれば、Friendオブジェクトを作ってリストに追加します
                if (idIndex != -1 && nameIndex != -1 && addressIndex != -1 && identityIndex != -1) {
                    Friend friend = new Friend(
                            cursor.getInt(idIndex),
                            cursor.getString(nameIndex),
                            cursor.getString(addressIndex),
                            cursor.getString(identityIndex)
                    );
                    friends.add(friend);
                }
            } while (cursor.moveToNext()); // 次の行がある限り繰り返す
        }
        cursor.close();
        db.close();
        return friends;
    }

    // ---------------------------------------------------------
    // メッセージの操作
    // ---------------------------------------------------------

    /**
     * 新しいチャットメッセージを履歴としてデータベースに保存します。
     *
     * @param chatMessage 保存するメッセージの内容（自分の送信か相手の受信かを含む）
     * @param fromAddress 送信元のアドレス
     * @param toAddress   送信先のアドレス
     */
    public void addMessage(ChatMessage chatMessage, String fromAddress, String toAddress) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(KEY_MESSAGE_FROM_ADDRESS, fromAddress);
        values.put(KEY_MESSAGE_TO_ADDRESS, toAddress);
        values.put(KEY_MESSAGE_CONTENT, chatMessage.getMessage());
        values.put(KEY_MESSAGE_TIMESTAMP, System.currentTimeMillis()); // 現在の時間をミリ秒で保存
        values.put(KEY_MESSAGE_IS_SENT, chatMessage.isSentByUser() ? 1 : 0); // 自分が送ったなら1

        db.insert(TABLE_MESSAGES, null, values); // データを追加
        db.close();
    }

    /**
     * 自分と特定の相手との間のチャット履歴を、古い順にすべて取得します。
     *
     * @param address1 自分または相手のアドレス
     * @param address2 自分または相手のアドレス
     * @return チャットメッセージのリスト
     */
    public List<ChatMessage> getMessages(String address1, String address2) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 「Aさんが送りBさんが受け取った」または「Bさんが送りAさんが受け取った」メッセージを
        // 時系列順（ASC: 昇順）で取得するSQL文です。
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " +
                "(" + KEY_MESSAGE_FROM_ADDRESS + " = ? AND " + KEY_MESSAGE_TO_ADDRESS + " = ?) OR " +
                "(" + KEY_MESSAGE_FROM_ADDRESS + " = ? AND " + KEY_MESSAGE_TO_ADDRESS + " = ?) " +
                "ORDER BY " + KEY_MESSAGE_TIMESTAMP + " ASC";

        SQLiteDatabase db = getReadableDatabase();
        
        // ?の部分にアドレスを入れてSQLを実行します
        Cursor cursor = db.rawQuery(selectQuery, new String[]{address1, address2, address2, address1});

        if (cursor.moveToFirst()) {
            do {
                int contentIndex = cursor.getColumnIndex(KEY_MESSAGE_CONTENT);
                int isSentIndex = cursor.getColumnIndex(KEY_MESSAGE_IS_SENT);

                if (contentIndex != -1 && isSentIndex != -1) {
                    // データベースから読み取った情報でChatMessageオブジェクトを作ります
                    ChatMessage message = new ChatMessage(
                            cursor.getString(contentIndex),
                            cursor.getInt(isSentIndex) == 1 // 1なら自分が送信（true）
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
     * ホーム画面の「チャット一覧」に表示するための、各友達ごとの【最新のメッセージ】と
     * 【友達情報】をまとめたリストを取得します。
     *
     * @param currentUserAddress 自分のアドレス
     * @return 最近やり取りしたチャットのリスト
     */
    public List<RecentChat> getRecentChats(String currentUserAddress) {
        List<RecentChat> recentChats = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // 1. まず、自分がメッセージを送受信したことがある「すべての相手のアドレス（重複なし）」を取得します。
        // UNIONを使って、送信先（TO）と送信元（FROM）の両方から自分のアドレス以外のものを集めます。
        String subQuery = "SELECT " + KEY_MESSAGE_FROM_ADDRESS + " FROM " + TABLE_MESSAGES + " WHERE " + KEY_MESSAGE_TO_ADDRESS + " = '" + currentUserAddress + "' " +
                "UNION SELECT " + KEY_MESSAGE_TO_ADDRESS + " FROM " + TABLE_MESSAGES + " WHERE " + KEY_MESSAGE_FROM_ADDRESS + " = '" + currentUserAddress + "'";
        Cursor friendAddressCursor = db.rawQuery(subQuery, null);

        if (friendAddressCursor.moveToFirst()){
            do {
                int addressIndex = friendAddressCursor.getColumnIndex(KEY_MESSAGE_FROM_ADDRESS);
                if(addressIndex != -1) {
                    // 友達のアドレスを1つ取り出します
                    String friendAddress = friendAddressCursor.getString(addressIndex);

                    // 2. その友達とやり取りした【一番新しい（最新の）メッセージ】を1件だけ取得します。
                    // ORDER BY ... DESC LIMIT 1 で、一番時間の新しいものを1つだけ選びます。
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

                            // 3. 連絡先（friends）テーブルから、この友達の「表示名」と「アイデンティティ」を取得します。
                            String friendInfoQuery = "SELECT " + KEY_FRIEND_NAME + ", " + KEY_FRIEND_IDENTITY + " FROM " + TABLE_FRIENDS + " WHERE " + KEY_FRIEND_ADDRESS + " = ?";
                            Cursor friendInfoCursor = db.rawQuery(friendInfoQuery, new String[]{friendAddress});
                            
                            // まだ連絡先に登録されていない相手の場合はUnknownにします
                            String friendName = "Unknown";
                            String identityAddress = "";
                            
                            if (friendInfoCursor.moveToFirst()) {
                                int nameIndex = friendInfoCursor.getColumnIndex(KEY_FRIEND_NAME);
                                int identIndex = friendInfoCursor.getColumnIndex(KEY_FRIEND_IDENTITY);
                                if (nameIndex != -1) {
                                    friendName = friendInfoCursor.getString(nameIndex);
                                }
                                if (identIndex != -1) {
                                    identityAddress = friendInfoCursor.getString(identIndex);
                                }
                            }
                            friendInfoCursor.close();

                            // 4. チャット一覧に表示するデータとしてリストにまとめます。
                            recentChats.add(new RecentChat(friendName, lastMessage, timestamp, friendAddress, identityAddress));
                        }
                    }
                    lastMessageCursor.close();
                }
            } while (friendAddressCursor.moveToNext()); // 次の友達へ
        }
        friendAddressCursor.close();
        db.close();

        return recentChats;
    }
}