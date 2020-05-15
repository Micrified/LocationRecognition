package com.example.lieu;

public class CellDataItem {
    private int cell_id;
    private String cell_text;

    public CellDataItem(int cell_id) {
        this.cell_id = cell_id;
        this.cell_text = "Cell " + cell_id;
    }

    public int getCellID () {
        return this.cell_id;
    }

    public String getCellText () {
        return this.cell_text;
    }
}
