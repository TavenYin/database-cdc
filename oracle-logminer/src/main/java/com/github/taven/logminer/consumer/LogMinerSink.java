package com.github.taven.logminer.consumer;

import com.github.taven.common.oracle.TableColumn;
import com.github.taven.logminer.LogMinerDmlObject;

import java.util.List;

public interface LogMinerSink {

    void init();

    void handleSnapshot(List<TableColumn> tableColumns, List<Object[]> records);

    void handleLogMinerDml(LogMinerDmlObject dmlObject);
}
