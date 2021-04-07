package com.xysss.airtestdemo;

public class ComBean {
    public byte[] bRec = null;
    public ComBean(byte[] paramArrayOfByte, int paramInt) {
        this.bRec = new byte[paramInt];
        for (int i = 0; ; i++) {
            if (i >= paramInt) {
                return;
            }
            this.bRec[i] = paramArrayOfByte[i];
        }
    }
}
