package com.devilopers.vesclogviewer;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class VESCData implements Serializable {

    protected List<DataPoint> rows;

    public VESCData() {
        rows = new LinkedList<>();
    }

    public void addRow(DataPoint row) {
        this.rows.add(row);
    }
}

