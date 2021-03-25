package com.github.taven.xstream;

import oracle.streams.*;

public class LcrEventHandler implements XStreamLCRCallbackHandler {
    @Override
    public void processLCR(LCR lcr) throws StreamsException {
        System.out.println("processLCR");
        if (lcr instanceof RowLCR) {
            // 模拟消费

        } else if (lcr instanceof DDLLCR) {
            // 模拟消费

        }
    }

    @Override
    public void processChunk(ChunkColumnValue chunkColumnValue) throws StreamsException {
        System.out.println("processChunk");
    }

    @Override
    public LCR createLCR() throws StreamsException {
        System.out.println("createLCR");
        return null;
    }

    @Override
    public ChunkColumnValue createChunk() throws StreamsException {
        System.out.println("createChunk");
        return null;
    }
}
