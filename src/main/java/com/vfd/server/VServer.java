package com.vfd.server;

import com.vfd.handler.RpcRequestMessageHandler;
import com.vfd.protocol.MessageCodec;
import com.vfd.protocol.ProtocolFrameDecoder;
import com.vfd.protocol.serializer.Serializer;
import com.vfd.protocol.serializer.impl.FastjsonSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @PackageName: com.vfd.server
 * @ClassName: RPCServer
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/5/10 上午11:17
 */
@SuppressWarnings("unused")
public class VServer {

    private int port;
    private Serializer serializer = new FastjsonSerializer();
    private String interfaceToImplement;

    public final Map<Integer, Channel> channelMap = new ConcurrentHashMap<>();

    public VServer() {
    }

    public VServer(int port) {
        this.port = port;
    }

    public VServer(String interfaceToImplement) {
        this.interfaceToImplement = interfaceToImplement;
    }

    public VServer(Serializer serializer) {
        this.serializer = serializer;
    }

    public VServer(int port, Serializer serializer) {
        this.port = port;
        this.serializer = serializer;
    }

    public VServer(Serializer serializer, String interfaceToImplement) {
        this.serializer = serializer;
        this.interfaceToImplement = interfaceToImplement;
    }

    public VServer(int port, String interfaceToImplement) {
        this.port = port;
        this.interfaceToImplement = interfaceToImplement;
    }

    public VServer(int port, Serializer serializer, String interfaceToImplement) {
        this.port = port;
        this.serializer = serializer;
        this.interfaceToImplement = interfaceToImplement;
    }

    public void startProvideServer () {
        provide0(this.port, this.serializer, this.interfaceToImplement);
    }

    public void startProvideServer (int port) {
        provide0(port, serializer, interfaceToImplement);
    }

    public void startProvideServer (Serializer serializer) {
        provide0(this.port, serializer, this.interfaceToImplement);
    }

    public void startProvideServer (String interfaceToImplement) {
        provide0(this.port, this.serializer, interfaceToImplement);
    }

    public void startProvideServer (int port, Serializer serializer) {
        provide0(port, serializer, this.interfaceToImplement);
    }

    public void startProvideServer (int port, String interfaceToImplement) {
        provide0(port, this.serializer, interfaceToImplement);
    }

    public void startProvideServer (Serializer serializer,  String interfaceToImplement) {
        provide0(this.port, serializer, interfaceToImplement);
    }

    public void startProvideServer (int port, Serializer serializer, String interfaceToImplement) {
        provide0(port, serializer, interfaceToImplement);
    }

    public void closeProvideServer (int port) {
        final Channel channel = channelMap.getOrDefault(port, null);
        if (channel != null)    channel.close();
    }

    // 为远程的服务消费者提供服务
    private void provide0(int port, Serializer serializer, String interfaceToImplement) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        MessageCodec MESSAGE_CODEC = new MessageCodec(serializer);
        RpcRequestMessageHandler RPC_HANDLER = new RpcRequestMessageHandler(interfaceToImplement);

        try {
            ServerBootstrap serverBootStrap = new ServerBootstrap();
            serverBootStrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            final ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new ProtocolFrameDecoder())
                                    //.addLast(new LoggingHandler(LogLevel.DEBUG))
                                    .addLast(MESSAGE_CODEC)
                                    .addLast(RPC_HANDLER);
                        }
                    });
            Channel channel = serverBootStrap.bind(port).sync().channel();
            channelMap.put(port, channel);
            final ChannelFuture channelFuture = channel.closeFuture();
            channelFuture.addListener(future -> {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
