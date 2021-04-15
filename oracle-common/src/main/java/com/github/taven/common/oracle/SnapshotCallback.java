package com.github.taven.common.oracle;

import java.util.List;

public interface SnapshotCallback {

    void apply(List<TableColumn> tableColumns, List<Object[]> records);

}
