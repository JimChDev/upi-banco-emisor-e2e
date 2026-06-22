package com.upi.bank.switchboard;

import com.upi.bank.switchboard.client.AccountMapperClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SwitchApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwitchApplication.class, args);
    }

    //Este ben se ejecuta automaticamente al iniciar la app
    @Bean
    public CommandLineRunner runTest(AccountMapperClient client){
        return args -> {
            System.out.println("----------------------------------------");
            System.out.println("Iniciando prueba de conexión gRPC E2E...");

            // Prueba 1: Un VPA Válido
            client.verifyVpa("juan@sbi");

            // Prueba 2: Un VPA Congelado (Debería dar error PERMISSION_DENIED)
            client.verifyVpa("maria@hdfc");

            // Prueba 3: Un VPA Inexistente (Debería dar error NOT_FOUND)
            client.verifyVpa("hacker@sbi");

            System.out.println("----------------------------------------");
        };
    }
}
