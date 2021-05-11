package com.vfd.client;

import com.vfd.handler.RpcResponseMessageHandler;
import com.vfd.message.RpcRequestMessage;
import com.vfd.protocol.MessageCodec;
import com.vfd.protocol.ProtocolFrameDecoder;
import com.vfd.protocol.SequenceIdGenerator;
import com.vfd.protocol.serializer.Serializer;
import com.vfd.protocol.serializer.impl.FastjsonSerializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;

import java.lang.reflect.Proxy;

/**
 * @PackageName: com.vfd
 * @ClassName: VRpc
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/5/10 下午1:03
 */
@SuppressWarnings("unused")
public class VClient {

    private String destHost;
    private int destPort;
    private Serializer serializer = new FastjsonSerializer();
    private boolean closeConnect = true;

    private Channel channel = null;

    final RpcResponseMessageHandler RPC_HANDLER = new RpcResponseMessageHandler();

    public VClient() {
    }

    public VClient(boolean closeConnect) {
        this.closeConnect = closeConnect;
    }

    public VClient(String destHost) {
        this.destHost = destHost;
    }

    public VClient(String destHost, boolean closeConnect) {
        this.destHost = destHost;
        this.closeConnect = closeConnect;
    }

    public VClient(int destPort) {
        this.destPort = destPort;
    }

    public VClient(int destPort, boolean closeConnect) {
        this.destPort = destPort;
        this.closeConnect = closeConnect;
    }

    public VClient(Serializer serializer) {
        this.serializer = serializer;
    }

    public VClient(Serializer serializer, boolean closeConnect) {
        this.serializer = serializer;
        this.closeConnect = closeConnect;
    }

    public VClient(String destHost, int destPort) {
        this.destHost = destHost;
        this.destPort = destPort;
    }

    public VClient(String destHost, int destPort, boolean closeConnect) {
        this.destHost = destHost;
        this.destPort = destPort;
        this.closeConnect = closeConnect;
    }

    public VClient(String destHost, Serializer serializer) {
        this.destHost = destHost;
        this.serializer = serializer;
    }

    public VClient(String destHost, Serializer serializer, boolean closeConnect) {
        this.destHost = destHost;
        this.serializer = serializer;
        this.closeConnect = closeConnect;
    }

    public VClient(int destPort, Serializer serializer) {
        this.destPort = destPort;
        this.serializer = serializer;
    }

    public VClient(int destPort, Serializer serializer, boolean closeConnect) {
        this.destPort = destPort;
        this.serializer = serializer;
        this.closeConnect = closeConnect;
    }

    public VClient(String destHost, int destPort, Serializer serializer) {
        this.destHost = destHost;
        this.destPort = destPort;
        this.serializer = serializer;
    }

    public VClient(String destHost, int destPort, Serializer serializer, boolean closeConnect) {
        this.destHost = destHost;
        this.destPort = destPort;
        this.serializer = serializer;
        this.closeConnect = closeConnect;
    }

    public void closeConnect () {
        if (channel != null)    channel.close();
    }

    public <T> T getRemoteObj (Class<T> interfaceType) {
        return getRemoteObj0(this.destHost, this.destPort, this.serializer, interfaceType);
    }

    public <T> T getRemoteObj (String destHost, Class<T> interfaceType) {
        return getRemoteObj0(destHost, this.destPort, this.serializer, interfaceType);
    }

    public <T> T getRemoteObj (int destPort, Class<T> interfaceType) {
        return getRemoteObj0(this.destHost, destPort, this.serializer, interfaceType);
    }

    public <T> T getRemoteObj (Serializer serializer, Class<T> interfaceType) {
        return getRemoteObj0(this.destHost, this.destPort, serializer, interfaceType);
    }

    public <T> T getRemoteObj (String destHost, int destPort, Class<T> interfaceType) {
        return getRemoteObj0(destHost, destPort, this.serializer, interfaceType);
    }

    public <T> T getRemoteObj (String destHost, Serializer serializer, Class<T> interfaceType) {
        return getRemoteObj0(destHost, this.destPort, serializer, interfaceType);
    }

    public <T> T getRemoteObj (int destPort, Serializer serializer, Class<T> interfaceType) {
        return getRemoteObj0(this.destHost, destPort, serializer, interfaceType);
    }

    public <T> T getRemoteObj (String destHost, int destPort, Serializer serializer, Class<T> interfaceType) {
        return getRemoteObj0(destHost, destPort, serializer, interfaceType);
    }

    @SuppressWarnings("all")
    private <T> T getRemoteObj0 (String destHost, int destPort, Serializer serializer,
                                Class<T> interfaceType) {
        final Object o = Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[] {interfaceType},
                ((proxy, method, args) -> {
                    final int sequenceId = SequenceIdGenerator.nextId();
                    RpcRequestMessage msg = new RpcRequestMessage(
                            sequenceId,
                            interfaceType.getName(),
                            method.getName(),
                            method.getReturnType(),
                            method.getParameterTypes(),
                            args
                    );
                    final Channel ch = getChannel(destHost, destPort, serializer);
                    ch.writeAndFlush(msg);
                    DefaultPromise<Object> promise = new DefaultPromise<>(ch.eventLoop());
                    RPC_HANDLER.getPROMISES().put(sequenceId, promise);
                    promise.await();
                    if (promise.isSuccess()) {
                        final Object now = promise.getNow();
                        if (closeConnect)   closeConnect();
                        return now;
                    } else {
                        final Throwable cause = promise.cause();
                        ch.close();
                        throw new RuntimeException(cause);
                    }
                }));
        return (T) o;
    }

    private Channel getChannel (String destHost, int destPort, Serializer serializer) {
        if (channel == null)
            initChannel(destHost, destPort, serializer);
        return channel;
    }

    private void initChannel (String destHost, int destPort, Serializer serializer) {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        MessageCodec MESSAGE_CODEC = new MessageCodec(serializer);
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        final ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new ProtocolFrameDecoder())
                                //.addLast(new LoggingHandler(LogLevel.DEBUG))
                                .addLast(MESSAGE_CODEC)
                                .addLast(RPC_HANDLER);
                    }
                });
        try {
            channel = bootstrap.connect(destHost, destPort).sync().channel();
            final ChannelFuture channelFuture = channel.closeFuture();
            channelFuture.addListener(future -> group.shutdownGracefully());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
