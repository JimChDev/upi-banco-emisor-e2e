package com.upi.bank.accountmapper;

import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository//siempre considera que es por responsabildiad y en este caso Repository maneja las excepciones de DAO
public class VpaRepository {
    private final Map<String, AccountInfo> dataStore = new ConcurrentHashMap<>();

    public VpaRepository() {
        dataStore.put("john.doe@bank", new AccountInfo("john.doe@bank", "1234567890", "ACTIVE"));
        dataStore.put("jane.smith@bank", new AccountInfo("jane.smith@bank", "0987654321", "FROZEN"));
    }

    public AccountInfo findAccountByVpa(String vpa) {
        return dataStore.get(vpa);
    }
}
