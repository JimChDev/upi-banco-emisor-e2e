package com.upi.bank.accountmapper.repository;

import com.upi.bank.accountmapper.model.AccountInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository//siempre considera que es por responsabildiad y en este caso Repository maneja las excepciones de DAO
public class VpaRepository {
    private static final Logger log = LoggerFactory.getLogger(VpaRepository.class);

    //Inyección del template que spring boot autoconfiguró gracias al POM y YAML
    private final RedisTemplate<String, Object> redisTemplate;

    //Prefijo en Redis para no mezclar datos si compartimos la  DB
    private static final String REDIS_PREFIX = "vpa_route:";

    private final Map<String, AccountInfo> dataStore = new ConcurrentHashMap<>();

    // Constructor que inicializa el repositorio con algunos datos de ejemplo.
    // En un escenario real, esto se conectaría a una base de datos.
    public VpaRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public AccountInfo findAccountByVpa(String vpa) {
        //Acceso O(1) Real cruzando la red hacia el contenedor redis
        return (AccountInfo) redisTemplate.opsForValue().get(REDIS_PREFIX + vpa);
    }

    // @PostConstruct hace que este metodo se ejecute apenas arranca el microservicio.
    // Usaremos esto temporalmente para "sembrar" (Seed) los datos de prueba en la DB.
    public void seedDatabase() {
        redisTemplate.opsForValue().set(REDIS_PREFIX + "juan@sbi", new AccountInfo("juan@sbi", "1234567890", "ACTIVE"));
        redisTemplate.opsForValue().set(REDIS_PREFIX + "maria@hdfc", new AccountInfo("maria@hdfc", "0987654321", "FROZEN"));
        redisTemplate.opsForValue().set(REDIS_PREFIX + "oll@sbi", new AccountInfo("oll@sbi", "1234567840", "ACTIVE"));
        log.info("Datos semilla intectados exitosamente en Redis");
    }
}
