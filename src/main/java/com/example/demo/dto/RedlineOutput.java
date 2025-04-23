package com.example.demo.dto;

import java.util.Arrays;

public class RedlineOutput {
    private final byte[] zipContent;
    
    public RedlineOutput(byte[] zipContent) {
        this.zipContent = zipContent;
    }
    
    public byte[] getZipContent() {
        return zipContent;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RedlineOutput that = (RedlineOutput) o;
        
        return Arrays.equals(zipContent, that.zipContent);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(zipContent);
    }
}