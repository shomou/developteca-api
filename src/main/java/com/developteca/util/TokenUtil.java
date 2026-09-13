package com.developteca.util;

import java.util.UUID;

public class TokenUtil {

    // ============ GENERA TOKEN UNICO =============
    public static String generateToken(){
        return UUID.randomUUID().toString();
    }

    // GENERA TOKEN NUMÉRICO  =========
    public static String generateNumericToken(int lenght){
        StringBuilder token = new StringBuilder();
        for(int i = 1 ; i < lenght; i++){
            token.append((int) (Math.random()* 10));
        }

        return token.toString();
    }

}
