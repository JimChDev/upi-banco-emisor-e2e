package com.upi.bank.accountmapper.repository;

import com.upi.bank.accountmapper.model.AccountInfo;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository//siempre considera que es por responsabildiad y en este caso Repository maneja las excepciones de DAO
public class VpaRepository {
    private final Map<String, AccountInfo> dataStore = new ConcurrentHashMap<>();

    // Constructor que inicializa el repositorio con algunos datos de ejemplo.
    // En un escenario real, esto se conectaría a una base de datos.
    public VpaRepository() {
        dataStore.put("juan@sbi", new AccountInfo("juan@sbi", "1234567890", "ACTIVE"));
        dataStore.put("maria@hdfc", new AccountInfo("maria@hdfc", "0987654321", "FROZEN"));
        dataStore.put("oll@sbi", new AccountInfo("oll@sbi", "1234567840", "ACTIVE"));
    }

    public AccountInfo findAccountByVpa(String vpa) {
        return dataStore.get(vpa);
    }
}
