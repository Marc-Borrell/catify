package com.example.criptoprojecte;

public class SongItem {
    private String fileName;
    private boolean isSigned;

    public SongItem(String fileName, boolean isSigned) {
        this.fileName = fileName;
        this.isSigned = isSigned;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isSigned() {
        return isSigned;
    }
}
