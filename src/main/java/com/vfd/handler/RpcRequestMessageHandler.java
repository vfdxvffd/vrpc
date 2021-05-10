package com.vfd.handler;

import com.vfd.message.RpcRequestMessage;
import com.vfd.message.RpcResponseMessage;
import com.vfd.server.ServicesFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.Method;

/**
 * @PackageName: com.vfd
 * @ClassName: RpcRequestMessageHandler
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/5/10 下午12:31
 */
@ChannelHandler.Sharable
public class RpcRequestMessageHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage message) {
        RpcResponseMessage response = new RpcResponseMessage();
        response.setSequenceId(message.getSequenceId());
        try {
            final Object o = ServicesFactory.getService(message.getInterfaceName());
            final Method method = o.getClass().getMethod(message.getMethodName(), message.getParameterTypes());
            final Object invoke = method.invoke(o, message.getParameterValue());
            response.setReturnValue(invoke);
        } catch (Exception e) {
            String msg = e.getCause().getMessage();
            final Exception exception = new Exception("Remote procedure call error: " + msg);
            exception.setStackTrace(new StackTraceElement[]{});
            response.setExceptionValue(exception);
        } finally {
            ctx.writeAndFlush(response);
        }
    }
}
