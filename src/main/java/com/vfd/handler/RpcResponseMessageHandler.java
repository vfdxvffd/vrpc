package com.vfd.handler;

import com.vfd.message.RpcResponseMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @PackageName: com.vfd.handler
 * @ClassName: RpcResponseMessageHandler
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/5/10 下午1:31
 */
@Slf4j
public class RpcResponseMessageHandler extends SimpleChannelInboundHandler<RpcResponseMessage> {

    private final Map<Integer, Promise<Object>> PROMISES = new ConcurrentHashMap<>();

    public Map<Integer, Promise<Object>> getPROMISES() {
        return PROMISES;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponseMessage message) {
        Promise<Object> promise = PROMISES.remove(message.getSequenceId());
        if (promise != null) {
            final Object returnValue = message.getReturnValue();
            final Exception exceptionValue = message.getExceptionValue();
            if (exceptionValue != null) {
                promise.setFailure(exceptionValue);
            } else {
                promise.setSuccess(returnValue);
            }
        }
    }
}
