package com.github.taven.logminer;

import java.math.BigInteger;

public class OffsetContext {
    // 该offset应该存储在外部，表示该SCN之前的记录已经全部处理完毕
    Long offsetScn;
    Long committedScn;

    public OffsetContext() {
    }

    public Long getOffsetScn() {
        return offsetScn;
    }

    public void setOffsetScn(Long offsetScn) {
        this.offsetScn = offsetScn;
    }

    public Long getCommittedScn() {
        return committedScn;
    }

    public void setCommittedScn(Long committedScn) {
        this.committedScn = committedScn;
    }
}
