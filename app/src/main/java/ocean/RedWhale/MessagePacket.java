package ocean.RedWhale;

import java.nio.ByteBuffer;

/**
 * Bluetoothメッセージの通信単位である「パケット」を表現するクラスです。
 * 大容量のデータを送るために、メッセージはこのパケットに分割して送信されます。
 */
public class MessagePacket {
    public static final byte TYPE_MSG = 0x01;   
    public static final byte TYPE_RELAY = 0x02; 
    public static final byte TYPE_ACK = 0x03;   
    
    // ヘッダーサイズ (1 + 4 + 2 + 2 + 32 + 32 + 1 + 8 + 2 = 84 bytes)
    public static final int HEADER_SIZE = 1 + 4 + 2 + 2 + 32 + 32 + 1 + 8 + 2;

    public byte type;          
    public int msgId;          
    public short chunkIndex;   
    public short totalChunks;  
    public byte[] destHash;    
    public byte[] srcHash;     
    public byte ttl;           
    public long timestamp;     
    public short payloadLength;
    public byte[] payload;     

    public MessagePacket(byte type, int msgId, short chunkIndex, short totalChunks, byte[] destHash, byte[] srcHash, byte ttl, long timestamp, byte[] payload) {
        this.type = type;
        this.msgId = msgId;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.destHash = destHash;
        this.srcHash = srcHash;
        this.ttl = ttl;
        this.timestamp = timestamp;
        this.payloadLength = (short) payload.length;
        this.payload = payload;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + payload.length);
        buffer.put(type);
        buffer.putInt(msgId);
        buffer.putShort(chunkIndex);
        buffer.putShort(totalChunks);
        buffer.put(destHash);
        buffer.put(srcHash);
        buffer.put(ttl);
        buffer.putLong(timestamp);
        buffer.putShort(payloadLength);
        buffer.put(payload);
        return buffer.array();
    }

    public static MessagePacket fromBytes(byte[] bytes) {
        if (bytes.length < HEADER_SIZE) return null;
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte type = buffer.get();
        int msgId = buffer.getInt();
        short chunkIndex = buffer.getShort();
        short totalChunks = buffer.getShort();
        byte[] destHash = new byte[32];
        buffer.get(destHash);
        byte[] srcHash = new byte[32];
        buffer.get(srcHash);
        byte ttl = buffer.get();
        long timestamp = buffer.getLong();
        short payloadLength = buffer.getShort();
        if (bytes.length < HEADER_SIZE + payloadLength) return null;
        byte[] payload = new byte[payloadLength];
        buffer.get(payload);
        return new MessagePacket(type, msgId, chunkIndex, totalChunks, destHash, srcHash, ttl, timestamp, payload);
    }
}
