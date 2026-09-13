package com.developteca.entity;

public enum Role {
    ADMIN("admin"),
    USER("user"),
    SUPER_ADMIN("super_admin");

    private final String value;

    Role(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
    
}
