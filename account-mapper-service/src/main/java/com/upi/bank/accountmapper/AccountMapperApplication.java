package com.upi.bank.accountmapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AccountMapperApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountMapperApplication.class, args);
//        var context = SpringApplication.run(AccountMapperApplication.class, args);
//
//        AccountMapperService service = context.getBean(AccountMapperService.class);
//
//        String[] vpas = {"juan@sbi", "maria@hdfc", "hacker@sbi","john.doe@bank","jane.smith@bank"};
//
//        for (String vpa : vpas) {
//            try {
//                var response = service.resolveAccount(vpa);
//                System.out.println("VPA: " + vpa + " -> " + response);
//            } catch (Exception e) {
//                System.err.println("Error con VPA " + vpa + ": " + e.getMessage());
//            }
//        }
    }
}
