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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @PackageName: com.vfd.server
 * @ClassName: RPCServer
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/5/10 上午11:17
 */
public class RPCServer {

    private final int port;
    private Serializer serializer = new FastjsonSerializer();

    public static final Map<Integer, Channel> channelMap = new ConcurrentHashMap<>();

    public RPCServer(int port) {
        this.port = port;
    }

    public RPCServer(int port, Serializer serializer) {
        this.port = port;
        this.serializer = serializer;
    }

    // 为远程的服务消费者提供服务
    public void provider() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        MessageCodec MESSAGE_CODEC = new MessageCodec(serializer);
        RpcRequestMessageHandler RPC_HANDLER = new RpcRequestMessageHandler();

        try {
            ServerBootstrap serverBootStrap = new ServerBootstrap();
            serverBootStrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
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
