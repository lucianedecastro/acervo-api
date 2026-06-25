package br.com.acervodaatletabrasileira.acervoapi.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ResetSenhaTemp {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String senha = "martarainha10";
        String hash = encoder.encode(senha);
        System.out.println("HASH GERADO:");
        System.out.println(hash);
    }
}

