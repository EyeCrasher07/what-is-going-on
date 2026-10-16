package dev.wigo.protocol;

import dev.wigo.mixin.ConnectionAccessor;
import dev.wigo.mixin.ServerConnectionAccessor;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;

/** Channel-owned state: negotiation once, independent ordered caches in each direction. */
public final class NetworkSessions {
    public record Options(boolean compact,boolean translations) {
        public static final Options LEGACY=new Options(false,false);
    }
    public static final class Peer {
        public volatile Options options=Options.LEGACY;
        public volatile boolean negotiated,helloSent;
        public volatile boolean clientSide;
        public volatile boolean smoothViewing;
        public volatile boolean observerCounts;
        public volatile boolean shortText;
        public volatile boolean richText;
        public volatile boolean packetTemplates;
        public volatile boolean richSource=true;
        public volatile int negotiations;
        final CompactCodec.Cache encoder=new CompactCodec.Cache(),decoder=new CompactCodec.Cache();
        long sentSequence,receivedSequence;
        boolean forceReset=true;
        public synchronized Options negotiate(Options selected) {
            if(!negotiated){options=selected;negotiated=true;negotiations++;}
            return options;
        }
        public void resetEncoder(){synchronized(encoder){forceReset=true;}}
    }
    private static final AttributeKey<Peer> KEY=AttributeKey.valueOf("wigo.network.session");
    private static final ThreadLocal<Peer> ACTIVE=new ThreadLocal<>();
    private NetworkSessions(){}
    public static Peer peer(Channel channel) {
        var attr=channel.attr(KEY);Peer peer=attr.get();
        if(peer==null){var fresh=new Peer();Peer existing=attr.setIfAbsent(fresh);peer=existing==null?fresh:existing;}
        return peer;
    }
    public static Peer peer(Connection connection) {
        if(connection==null)return null;
        Channel channel=((ConnectionAccessor)(Object)connection).wigoChannel();
        return channel==null?null:peer(channel);
    }
    public static Peer peer(ServerPlayer player) {
        return player.connection==null?null:peer(((ServerConnectionAccessor)(Object)player.connection).wigoConnection());
    }
    public static Peer current(){return ACTIVE.get();}
    public static Scope scope(Peer peer){return new Scope(peer);}
    public static final class Scope implements AutoCloseable {
        private final Peer previous;
        private Scope(Peer peer){previous=ACTIVE.get();if(peer==null)ACTIVE.remove();else ACTIVE.set(peer);}
        @Override public void close(){if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}
    }
}
