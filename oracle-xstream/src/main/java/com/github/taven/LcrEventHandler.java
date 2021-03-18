package com.github.taven;

import oracle.streams.*;

public class LcrEventHandler implements XStreamLCRCallbackHandler {
    @Override
    public void processLCR(LCR lcr) throws StreamsException {
        if (lcr instanceof RowLCR) {
            // 模拟消费

        } else if (lcr instanceof DDLLCR) {
            // 模拟消费

        }
    }

    @Override
    public void processChunk(ChunkColumnValue chunkColumnValue) throws StreamsException {

    }

    @Override
    public LCR createLCR() throws StreamsException {
        return null;
    }

    @Override
    public ChunkColumnValue createChunk() throws StreamsException {
        return null;
    }
}
