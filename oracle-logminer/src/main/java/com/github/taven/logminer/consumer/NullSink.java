package com.github.taven.logminer.consumer;

import com.github.taven.common.oracle.TableColumn;
import com.github.taven.logminer.LogMinerDmlObject;

import java.util.List;

public class NullSink implements LogMinerSink {
    @Override
    public void init() {

    }

    @Override
    public void handleSnapshot(List<TableColumn> tableColumns, List<Object[]> records) {

    }

    @Override
    public void handleLogMinerDml(LogMinerDmlObject dmlObject) {

    }
}
