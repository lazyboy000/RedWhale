package ocean.RedWhale;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MessageRouter {
    private static final String TAG = "MessageRouter";
    // MTU is max 517, so 400 is safe
    private static final int MAX_PAYLOAD_SIZE = 400;

    private final Map<Integer, List<MessagePacket>> incomingChunks = new HashMap<>();
    private final Set<Integer> processedMessageIds = new HashSet<>();
    private byte[] localAddressHash;

    public void setLocalAddressHash(byte[] hash) {
        this.localAddressHash = hash;
    }

    public interface MessageListener {
        void onMessageReceived(String message, byte[] senderHash);
        void onRelayRequest(MessagePacket packet);
    }

    private MessageListener listener;

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public List<MessagePacket> preparePackets(String message, byte[] destHash, byte[] srcHash) {
        byte[] data = message.getBytes();
        int msgId = new Random().nextInt();
        short totalChunks = (short) Math.ceil((double) data.length / MAX_PAYLOAD_SIZE);
        List<MessagePacket> packets = new ArrayList<>();
        byte initialTtl = 10;
        long timestamp = System.currentTimeMillis();

        for (short i = 0; i < totalChunks; i++) {
            int start = i * MAX_PAYLOAD_SIZE;
            int length = Math.min(MAX_PAYLOAD_SIZE, data.length - start);
            byte[] payload = new byte[length];
            System.arraycopy(data, start, payload, 0, length);
            packets.add(new MessagePacket(MessagePacket.TYPE_MSG, msgId, i, totalChunks, destHash, srcHash, initialTtl, timestamp, payload));
        }
        processedMessageIds.add(msgId);
        return packets;
    }

    public void handleIncomingPacket(byte[] bytes) {
        MessagePacket packet = MessagePacket.fromBytes(bytes);
        if (packet == null) return;
        if (packet.ttl <= 0) return;

        if (localAddressHash != null && !java.util.Arrays.equals(packet.destHash, localAddressHash)) {
            int uniqueChunkId = packet.msgId + packet.chunkIndex;
            if (processedMessageIds.contains(uniqueChunkId)) return;
            processedMessageIds.add(uniqueChunkId);
            packet.ttl--;
            if (listener != null) listener.onRelayRequest(packet);
            return;
        }

        if (processedMessageIds.contains(packet.msgId)) return;

        if (!incomingChunks.containsKey(packet.msgId)) {
            incomingChunks.put(packet.msgId, new ArrayList<>());
        }
        List<MessagePacket> chunks = incomingChunks.get(packet.msgId);
        for (MessagePacket p : chunks) {
            if (p.chunkIndex == packet.chunkIndex) return;
        }
        chunks.add(packet);

        if (chunks.size() == packet.totalChunks) {
            byte[] fullData = reassemble(chunks, packet.totalChunks);
            incomingChunks.remove(packet.msgId);
            processedMessageIds.add(packet.msgId);
            if (listener != null) listener.onMessageReceived(new String(fullData), packet.srcHash);
        }
    }

    private byte[] reassemble(List<MessagePacket> chunks, short total) {
        chunks.sort((a, b) -> a.chunkIndex - b.chunkIndex);
        int totalSize = 0;
        for (MessagePacket p : chunks) totalSize += p.payloadLength;
        byte[] result = new byte[totalSize];
        int currentPos = 0;
        for (MessagePacket p : chunks) {
            System.arraycopy(p.payload, 0, result, currentPos, p.payloadLength);
            currentPos += p.payloadLength;
        }
        return result;
    }
}
