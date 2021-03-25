package com.github.taven.oracle;

import java.util.List;
import java.util.Map;

public class SnapshotResult {
    public static final SnapshotResult NULL = new SnapshotResult();

    private Map<String, List<TableColumn>> tableStructure;
    private boolean complete;
    private long scn;

    private SnapshotResult() {
    }

    public SnapshotResult(Map<String, List<TableColumn>> tableStructure, boolean complete, long scn) {
        this.tableStructure = tableStructure;
        this.complete = complete;
        this.scn = scn;
    }

    public Map<String, List<TableColumn>> getTableStructure() {
        return tableStructure;
    }

    public boolean isComplete() {
        return complete;
    }

    public long getScn() {
        return scn;
    }

    @Override
    public String toString() {
        return "SnapshotResult{" +
                "tableStructure=" + tableStructure +
                ", complete=" + complete +
                ", scn=" + scn +
                '}';
    }
}
