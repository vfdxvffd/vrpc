package com.vfd.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @PackageName: com.vfd
 * @ClassName: protocolFrameDecoder
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/5/10 上午10:07
 */
public class ProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {

    public ProtocolFrameDecoder() {
        this(1024, 12, 4, 0, 0);
    }

    public ProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }
}
