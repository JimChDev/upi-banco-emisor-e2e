package com.upi.bank.accountmapper;

import org.springframework.stereotype.Service;

@Service
public class AccountMapperService {
    private final VpaRepository repository;

    // INYECCIÓN POR CONSTRUCTOR (La mejor práctica de la industria)
    // Spring ve que este servicio necesita el repositorio, lo busca en su Contexto y lo inyecta aquí automáticamente.
    public AccountMapperService(VpaRepository repository) {
        this.repository = repository;
    }

    public AccountInfo resolveAccount(String vpa){
        AccountInfo account = repository.findAccountByVpa(vpa);
        if(account == null){
            throw new IllegalArgumentException("VPA Inválido o no pertenece a este banco");
        }

        //Regla de negocio antifraude básca
        if("FROZEN".equals(account.Status())){
            throw new IllegalStateException("Transacción rechazada: la cuenta está congelada por sospecha de fraude");
        }

        return account;
    }
}
