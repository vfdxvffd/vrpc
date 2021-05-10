package com.vfd;

import com.vfd.handler.RpcResponseMessageHandler;
import com.vfd.message.RpcRequestMessage;
import com.vfd.protocol.MessageCodec;
import com.vfd.protocol.ProtocolFrameDecoder;
import com.vfd.protocol.SequenceIdGenerator;
import com.vfd.protocol.serializer.Serializer;
import com.vfd.protocol.serializer.impl.FastjsonSerializer;
import com.vfd.server.RPCServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;

import java.lang.reflect.Proxy;

/**
 * @PackageName: com.vfd
 * @ClassName: VRpc
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/5/10 下午1:03
 */
public class VRpc {

    private final String destHost;
    private final int destPort;
    private Serializer serializer = new FastjsonSerializer();

    private Channel channel = null;

    final RpcResponseMessageHandler RPC_HANDLER = new RpcResponseMessageHandler();

    public VRpc(String destHost, int destPort) {
        this.destHost = destHost;
        this.destPort = destPort;
    }

    public VRpc(String destHost, int destPort, Serializer serializer) {
        this.destHost = destHost;
        this.destPort = destPort;
        this.serializer = serializer;
    }

    public void startProvideServer(int port) {
        new RPCServer(port).provider();
    }

    public void startProvideServer(int port, Serializer serializer) {
        new RPCServer(port, serializer).provider();
    }

    public void closeProvideServer (int port) {
        final Channel channel = RPCServer.channelMap.getOrDefault(port, null);
        if (channel != null)    channel.close();
    }

    @SuppressWarnings("all")
    public <T> T getRemoteObj (Class<T> interfaceType) {
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
                    getChannel().writeAndFlush(msg);
                    DefaultPromise<Object> promise = new DefaultPromise<>(getChannel().eventLoop());
                    RPC_HANDLER.getPROMISES().put(sequenceId, promise);
                    promise.await();
                    if (promise.isSuccess()) {
                        final Object now = promise.getNow();
                        channel.close();
                        return now;
                    } else {
                        channel.close();
                        throw new RuntimeException(promise.cause());
                    }
                }));
        return (T) o;
    }

    private Channel getChannel () {
        if (channel == null)
            initChannel();
        return channel;
    }

    private void initChannel () {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        MessageCodec MESSAGE_CODEC = new MessageCodec(serializer);
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
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
            channelFuture.addListener(future -> {
                group.shutdownGracefully();
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
