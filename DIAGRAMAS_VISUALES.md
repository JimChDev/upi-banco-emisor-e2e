# 📈 DIAGRAMAS VISUALES - ARQUITECTURA MICROSERVICIOS UPI

## 1. DIAGRAMA DE FLUJO DE TRANSACCIÓN COMPLETO

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         USUARIO UPI (Aplicación Móvil)                      │
│                 Ingresa: VPA destino, monto, PIN de seguridad                │
└─────────────────────┬───────────────────────────────────────────────────────┘
                      │ HTTP POST
                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                  BANK UPI SWITCH (Puerto 9091) ❌ PLANEADO                  │
│                   Orquestador Central - Punto de Entrada                     │
│  • Valida formato de solicitud                                              │
│  • Abre transacción                                                         │
│  • Coordina llamadas a servicios especializados                             │
└──┬───────────────────────────────────────────────────────────────┬──────────┘
   │                                                               │
   │ gRPC (HTTP/2)                                                │
   ▼                                                               ▼
┌──────────────────────────────┐                      ┌──────────────────────┐
│ ACCOUNT MAPPER SERVICE       │                      │ DEVICE BINDING       │
│ (Puerto 9090) ✅ IMPLEMENTADO│                      │ (Puerto 9092) ❌     │
│                              │                      │                      │
│ 1. Mapea VPA enviador:       │                      │ 1. Valida dispositivo│
│    john.doe@bank             │                      │ 2. Verifica huella   │
│    ↓↓ 1234567890             │                      │ 3. Valida localización
│                              │                      │ 4. Retorna: OK/ERROR │
│ 2. Mapea VPA receptor:       │                      │                      │
│    jane.smith@bank           │                      │ Status: ✅/⚠️/❌    │
│    ↓↓ 0987654321             │                      │                      │
│                              │                      │                      │
│ 3. Valida estado cuentas:    │                      │                      │
│    ACTIVE / FROZEN           │                      │                      │
│                              │                      │                      │
│ Retorna: CUENTA_ESTRUCTURA   │                      │                      │
└────┬─────────────────────────┘                      └────────────┬─────────┘
     │ Response                                                     │ Response
     └──────────────────────────┬──────────────────────────────────┘
                                ▼
                   ┌──────────────────────────────┐
                   │ FRAUD DETECTION ENGINE       │
                   │ (Puerto 9093) ❌ PLANEADO   │
                   │                              │
                   │ 1. Analiza patrón usuario:   │
                   │    • Monto promedio: $500    │
                   │    • Monto actual: $50,000   │
                   │    ⚠️ 100x mayor (ANOMALÍA)  │
                   │                              │
                   │ 2. Análisis ubicación:       │
                   │    • Última tx: Lima, Perú  │
                   │    • Actual: Hong Kong       │
                   │    ⚠️ Cambio geográfico rápido
                   │                              │
                   │ 3. Análisis de velocidad:    │
                   │    • Última tx: 10 min atrás │
                   │    ✅ Normal                 │
                   │                              │
                   │ 4. Scoring de riesgo:        │
                   │    • Suma puntos de riesgo   │
                   │    • Threshold: 100 puntos   │
                   │                              │
                   │ Retorna: RIESGO_SCORE        │
                   │ (0-200)                      │
                   │ LOW / MEDIUM / HIGH          │
                   └────────────┬─────────────────┘
                                │ Response
                                ▼
                   ┌──────────────────────────────┐
                   │ HIGH-SPEED AUTHORIZER        │
                   │ (Puerto 9094) ❌ PLANEADO   │
                   │                              │
                   │ 1. Valida fondos:            │
                   │    • Saldo: $50,000          │
                   │    • Monto: $50,000          │
                   │    ✅ Fondos disponibles     │
                   │                              │
                   │ 2. Valida límites:           │
                   │    • Límite diario: $100,000 │
                   │    • Total hoy: $10,000      │
                   │    • Nueva tx: $50,000       │
                   │    • Total sería: $60,000    │
                   │    ✅ Dentro de límite       │
                   │                              │
                   │ 3. Registra transacción:     │
                   │    • Genera ID: TXN123456    │
                   │    • Estado: PENDING         │
                   │    • Timestamp: 2026-06-20   │
                   │                              │
                   │ 4. Retorna: AUTHORIZED       │
                   │    o DENIED                  │
                   └────────────┬─────────────────┘
                                │ Response
                   ┌────────────┴──────────────────┐
                   ▼                              ▼
         DECISION ROUTER                    ERROR HANDLER
         (Aprobada o Rechazada)             (Reversal)
                   │                             │
       ┌───────────┴───────────┐                 │
       │                       │                 │
    APROBADA              RECHAZADA           ROLLBACK
       │                       │                 │
       ▼                       ▼                 ▼
   ┌────────┐            ┌──────────┐      ┌──────────┐
   │ MOCK   │            │  SAGA    │      │  SAGA    │
   │ CORE   │            │ REVERSAL │      │ REVERSAL │
   │BANKING │            │          │      │  (2x)    │
   └───┬────┘            └────┬─────┘      └────┬─────┘
       │                      │                  │
       ├─ Deduce saldo       │                  │
       │ de cuenta envío      │                  │
       │                      │                  │
       ├─ Abona saldo        │                  │
       │ cuenta recepción    │                  │
       │                      │                  │
       ├─ Registra           │ Revierte todos   Revierte todos
         transacción           los cambios       los cambios
       │                      │ previos          previos x2
       │                      │                  │
       └───────────┬──────────┘                  │
                   │                             │
                   ▼                             ▼
            ┌──────────────┐            ┌──────────────┐
            │ RECONCILIATION│         NOTIF A USUARIO  │
            │ SERVICE      │          ❌ TRANSACCIÓN   │
            │ (Puerto 9099)│             RECHAZADA     │
            │              │                          │
            │ • Concilia   │              Razones:    │
            │   transacción│ • Fondos insuficientes   │
            │ • Compara    │ • Fraude detectado       │
            │   con DB     │ • Límites excedidos      │
            │ • Reportes   │ • Dispositivo no         │
            │              │   identificado           │
            │ Status: OK   │                          │
            └──────┬───────┘                          │
                   │                                  │
                   ▼                                  ▼
            ┌──────────────┐                  ┌──────────────────┐
            │ NOTIF SUCCESS│                  │  APP MÓVIL       │
            │ A USUARIO    │                  │                  │
            │              │                  │  Status: ❌      │
            │ ✅ Transacción│                 │  Reason: fraud   │
            │   Completada │                 │  Reference: #123 │
            │              │                 │  Date: timestamp  │
            │ De: john.doe │                 │                  │
            │ Para: jane   │                 │  [Reintentar]    │
            │ Monto: $50K  │                 │  [Contactar]     │
            │ Ref: #TXN123 │                 │                  │
            │ Date: time   │                 │                  │
            └──────────────┘                 └──────────────────┘
```

---

## 2. MATRIZ DE DEPENDENCIAS ENTRE SERVICIOS

```
                    SWITCH  MAPPER  DEVICE  FRAUD   AUTH    CRYPTO  SAGA    RECONCILE  MANDATE
                     9091    9090    9092    9093    9094    9097    9098    9099       9095
    ┌─────────────────────────────────────────────────────────────────────────────────────────┐
    │  SWITCH      │  ---   │  ✓ ← │  ✓ ←  │  ✓ ←  │  ✓ ←  │  ✓ ←  │  ✓ ←  │  ✓ ←      │  ✓ ←
    │  (9091)      │         │      │        │        │        │        │        │            │
    ├──────────────┼─────────┼──────┼────────┼────────┼────────┼────────┼────────┼────────────┤
    │  MAPPER      │         │  --- │        │        │        │        │        │            │
    │  (9090)      │         │      │        │        │        │        │        │            │
    │              │         │      │        │        │        │        │        │            │
    ├──────────────┼─────────┼──────┼────────┼────────┼────────┼────────┼────────┼────────────┤
    │  DEVICE      │         │      │  ---   │        │        │        │        │            │
    │  (9092)      │         │      │        │        │        │        │        │            │
    │              │         │      │        │        │        │        │        │            │
    ├──────────────┼─────────┼──────┼────────┼────────┼────────┼────────┼────────┼────────────┤
    │  FRAUD       │         │      │        │  ---   │        │        │        │            │
    │  (9093)      │         │      │        │        │        │        │        │            │
    │              │         │      │        │        │        │        │        │            │
    ├──────────────┼─────────┼──────┼────────┼────────┼────────┼────────┼────────┼────────────┤
    │  AUTH        │         │      │        │        │  ---   │        │        │            │
    │  (9094)      │         │      │        │        │        │ ✓ ←    │        │            │
    │              │         │      │        │        │        │        │        │            │
    ├──────────────┼─────────┼──────┼────────┼────────┼────────┼────────┼────────┼────────────┤
    │  CRYPTO      │         │      │        │        │        │  ---   │        │            │
    │  (9097)      │         │      │        │        │ ✓ ←    │        │ ✓ ←    │  ✓ ←       │
    │              │         │      │        │        │        │        │        │            │
    ├──────────────┼─────────┼──────┼────────┼────────┼────────┼────────┼────────┼────────────┤
    │  SAGA        │         │      │        │        │        │        │  ---   │  ✓ ←       │
    │  (9098)      │         │      │        │        │        │        │        │            │
    │              │         │      │        │        │        │        │        │            │
    ├──────────────┼─────────┼──────┼────────┼────────┼────────┼────────┼────────┼────────────┤
    │  RECONCILE   │         │      │        │        │        │        │        │  ---       │
    │  (9099)      │         │      │        │        │        │        │        │            │
    │              │         │      │        │        │        │        │        │            │
    ├──────────────┼─────────┼──────┼────────┼────────┼────────┼────────┼────────┼────────────┤
    │  MANDATE     │         │      │        │        │        │ ✓ ←    │        │            │  ---
    │  (9095)      │         │      │        │        │        │        │        │            │
    │              │         │      │        │        │        │        │        │            │
    └──────────────┴─────────┴──────┴────────┴────────┴────────┴────────┴────────┴────────────┘

    ✓ ← = Llama a este servicio
    --- = Servicios (diagonal)
```

---

## 3. ESTRUCTURA DE DATOS CLAVE

### AccountInfo (Record - Inmutable)

```
┌──────────────────────────────────┐
│      AccountInfo                 │
│                                  │
│ - vpa: String                    │
│   Ej: "john.doe@bank"            │
│                                  │
│ - accountNumber: String          │
│   Ej: "1234567890"               │
│                                  │
│ - accountStatus: String          │
│   Valores: ACTIVE, FROZEN        │
│                                  │
└──────────────────────────────────┘
```

### Cuenta (Entidad - Base de Datos)

```
┌────────────────────────────────────┐
│      Cuenta                        │
│                                    │
│ - vpa: String (PK)                 │
│   Ej: "john.doe@bank"              │
│                                    │
│ - numeroCuenta: String             │
│   Ej: "1234567890"                 │
│                                    │
│ - estado: String                   │
│   Valores: ACTIVE, FROZEN          │
│                                    │
│ - saldo: Double                    │
│   Ej: 50000.00                     │
│                                    │
│ - fechaCreacion: LocalDateTime     │
│   Ej: 2026-06-20 10:30:00          │
│                                    │
│ - tipoMoneda: String               │
│   Ej: "PEN" (Soles), "USD" (Dólares)
│                                    │
│ - limiteDiario: Double             │
│   Ej: 100000.00                    │
│                                    │
└────────────────────────────────────┘
```

### Transacción (Modelo de dominio)

```
┌────────────────────────────────────┐
│      Transacción                   │
│                                    │
│ - transactionId: String (UUID)     │
│   Ej: "TXN-20260620-001234"        │
│                                    │
│ - vpaOrigen: String                │
│   Ej: "john.doe@bank"              │
│                                    │
│ - vpaDestino: String               │
│   Ej: "jane.smith@bank"            │
│                                    │
│ - monto: Double                    │
│   Ej: 50000.00                     │
│                                    │
│ - moneda: String                   │
│   Ej: "PEN"                        │
│                                    │
│ - timestamp: LocalDateTime         │
│   Ej: 2026-06-20 14:45:32          │
│                                    │
│ - estado: String                   │
│   Valores: PENDING, AUTHORIZED,    │
│            COMPLETED, REJECTED,    │
│            REVERSED                │
│                                    │
│ - razonRechazo: String (nullable)  │
│   Ej: "Fondos insuficientes"       │
│                                    │
│ - riesgo: String                   │
│   Valores: LOW, MEDIUM, HIGH       │
│                                    │
└────────────────────────────────────┘
```

---

## 4. COMPARATIVO: ARQUITECTURA Traditional vs Microservicios

### APLICACIÓN MONOLÍTICA (Antes)

```
┌────────────────────────────────────────────────────┐
│           APLICACIÓN MONOLÍTICA                    │
│           (Un solo JAR/WAR)                        │
│                                                    │
│  ┌─ Account Mapping Code                          │
│  │                                                │
│  ├─ Device Binding Code                           │
│  │                                                │
│  ├─ Fraud Detection Code                          │
│  │                                                │
│  ├─ Authorization Code                            │
│  │                                                │
│  ├─ Cryptography Code                             │
│  │                                                │
│  ├─ Saga Reversal Code                            │
│  │                                                │
│  └─ Reconciliation Code                           │
│                                                    │
│  1 Base de Datos Compartida                       │
│  1 Servidor (Puerto 8080)                         │
│                                                    │
│  PROBLEMAS:                                       │
│  ❌ Un error causa caída total                    │
│  ❌ Escalado: todo o nada                         │
│  ❌ Deploy: recompila todo                        │
│  ❌ Lenguaje único: no es flexible                │
│  ❌ Testing: todo junto es complejo               │
└────────────────────────────────────────────────────┘
```

### ARQUITECTURA MICROSERVICIOS (Actual)

```
                    Orquestador
                       ▲
                       │
    ┌──────────┬───────┼────────┬──────────┐
    ▼          ▼       ▼        ▼          ▼
┌─────────┐ ┌──────┐ ┌─────┐ ┌──────┐ ┌─────────┐
│Account  │ │Device│ │Fraud│ │Auth  │ │ Crypto  │
│Mapper   │ │Binding
│(9090)   │ │(9092)│ │(9093)│ │(9094)│ │(9097)  │
│         │ │      │ │      │ │      │ │         │
│Single   │ │Scaling
│DB       │ │Only  │ │ML    │ │Fast  │ │ HSM    │
│Pod 1    │ │This  │ │Engine
└─────────┘ └──────┘ └─────┘ └──────┘ └─────────┘


VENTAJAS:
✅ Escalado independiente
✅ Deploy independiente
✅ Tecnologías mixtas (Java, Go, Python)
✅ Resiliencia (fallo aislado)
✅ Testing independiente
✅ Equipos independientes
```

---

## 5. FLUJO DE DESARROLLO CON MAVEN

```
SOURCE CODE (.java files)
    │
    ▼
┌──────────────────────┐
│ Maven Compile        │ javac
│                      │
└──────────┬───────────┘
           │
           ▼
        .class files (Bytecode)
    target/classes/
           │
           ▼
┌──────────────────────┐
│ Maven Package        │
│ JAR Creation         │
└──────────┬───────────┘
           │
           ▼
    .jar file
    (Comprimido ZIP)
    target/*.jar
           │
           ▼
┌──────────────────────┐
│ JVM Execution        │
│ java -jar app.jar    │
└──────────┬───────────┘
           │
           ▼
    APLICACIÓN EJECUTÁNDOSE
    (En memoria)
```

---

## 6. CICLO COMPLETO DE UNA PETICIÓN gRPC

### Cliente (Otra aplicación):

```
Cliente gRPC
    │
    ├─ Obtiene stub del servicio
    │     └─ AccountMapperGrpcServiceGrpc.newBlockingStub(channel)
    │
    ├─ Crea mensaje de entrada
    │     └─ AccountResolveRequest.newBuilder()
    │        .setVpa("john.doe@bank")
    │        .build()
    │
    ├─ Llama método RPC
    │     └─ AccountResolveResponse response = 
    │        stub.mapAccount(request)
    │
    └─ Recibe respuesta
          └─ response.getAccountNumber()
             response.getAccountStatus()
```

### Servidor (Account Mapper Service):

```
gRPC Server escuchando (Puerto 9090)
    │
    ├─ Recibe solicitud
    │
    ├─ Deserializa Protocol Buffer
    │     └─ Lee: vpa = "john.doe@bank"
    │
    ├─ Ejecuta lógica en AccountMapperGrpcServiceImpl
    │     └─ mapAccount() en gRpc impl
    │
    ├─ Llama servicio de negocio
    │     └─ accountMapperService.mapAccount(vpa)
    │
    ├─ Accede repositorio
    │     └─ vpaRepository.buscar(vpa)
    │
    ├─ Construye respuesta
    │     └─ AccountResolveResponse.newBuilder()
    │        .setAccountNumber("1234567890")
    │        .setAccountStatus("ACTIVE")
    │        .build()
    │
    ├─ Serializa Protocol Buffer (binario)
    │
    └─ Envía respuesta
         └─ Via HTTP/2 (muy eficiente)
```

---

## 7. JAVA 21 - CARACTERÍSTICAS MODERNAS

```
Java Evolution Timeline:
┌─────────────────────────────────────────────────────────────┐

Java 8  (2014) - Revolucionario
  └─ Lambda Expressions
  └─ Streams API
  └─ Functional Interfaces

Java 11 (2018) - LTS (Long Term Support)
  └─ Local Variable Type Inference (var)
  └─ HTTP Client
  └─ Remove Java EE modules

Java 17 (2021) - LTS
  └─ Sealed Classes
  └─ Pattern Matching (preview)
  └─ Foreign Function & Memory API

Java 21 (2023) - ACTUAL ✓ Este proyecto
  └─ Virtual Threads (proyecto Loom)
  └─ Pattern Matching (completo)
  └─ Record Patterns
  └─ String Templates (preview)
  └─ Unnamed Patterns

┌─────────────────────────────────────────────────────────────┐
```

### Virtual Threads (Java 21 - Cambio revolucionario)

```
ANTES (Java < 21):
┌─────────────────────────┐
│ 1 Thread = 1 MB         │
│ Capacidad: ~1000        │
│ threads por servidor    │
│ Contexto: Pesado        │
└─────────────────────────┘

    Servidor con 1000 HTTP requests
            │
            ├─ Thread 1 (1MB)
            ├─ Thread 2 (1MB)
            ├─ Thread 3 (1MB)
            ...
            └─ Thread 1000 (1MB)
                    ▼
            Total: 1000 MB = 1 GB RAM

    Problema: I/O Blocking
    ┌─━━━━━━━━━━┓
    │ Thread 1  │ Espera...
    │ (Bloqueado│ Base de Datos
    │  por DB)  │ (100ms)
    └─━━━━━━━━━━┘


DESPUÉS (Java 21):
┌─────────────────────────┐
│ 1 Virtual Thread ≈ 1KB  │
│ Capacidad: 1,000,000+   │
│ virtual threads         │
│ Contexto: Ligero        │
└─────────────────────────┘

    Servidor con 1M requests
            │
            ├─ VirtualThread 1 (1KB)
            ├─ VirtualThread 2 (1KB)
            ├─ VirtualThread 3 (1KB)
            ...
            └─ VirtualThread 1M (1KB)
                    ▼
            Total: 1M KB = 1 GB RAM
            (¡Mismo RAM pero 1000x threads!)

    Sin Blocking (async-beneath-the-hood)
    ┌─━━━━━━━━━━┓
    │ VThread 1 │ Suspendido (no CPU)
    │ (Esperando│ Base de Datos
    │ en park)  │ (100ms)
    └─━━━━━━━━━━┘
    CPU libre para otros threads
```

---

## 8. PROTOCOLO HTTP/2 vs HTTP/1.1 (gRPC usa HTTP/2)

### HTTP/1.1 (Tradicional - REST)

```
┌────────────────┐
│ Cliente HTTP   │
└───┬────────────┘
    │ Request 1 (Concatenación de texto)
    │ GET /api/accounts/john.doe@bank HTTP/1.1
    │ Host: localhost:8080
    │ Accept: application/json
    │ [cuerpo vacío]
    │ Size: ~150 bytes
    ▼
┌──────────────────────┐
│ Servidor HTTP/1.1    │
│                      │
│ Response 1           │
│ HTTP/1.1 200 OK      │
│ Content-Type: json   │
│ [JSON body]          │
│ Size: ~200 bytes     │
└──────────┬───────────┘
           │ ESPERA... ⏳ Conexión idle
           │ (100ms entre requests)
           │
    Request 2 (Concatenación de texto)
    POST /api/device-validate
    ...
    ▼
    Response 2
    ...

    Problema: No puede reutilizar conexión efficiently
    Serializado: TEXT (usa más banda)
    Tamaño: GRANDE (headers repetidos)
```

### HTTP/2 (Moderno - gRPC)

```
┌────────────────┐
│ Cliente gRPC   │
└────┬───────────┘
     │ Request 1 (Binario Protocol Buffer)
     │ $20 bytes
     ├─────────────────┐
     │                 │ Sin bloqueo
Request 2 (Binario)    │ Multiplexado
│ $18 bytes     │ sobre misma
│               │ conexión
     │
     ├ Request 3 (Binario)
     └─ $25 bytes
              ▼
┌──────────────────────┐
│ Servidor HTTP/2      │
│                      │
│ Response 1 (Stream 1)│
│ Binary: 50 bytes     │
├─────────────────────┤
│ Response 3 (Stream 3)│
│ Binary: 45 bytes     │
├─────────────────────┤
│ Response 2 (Stream 2)│
│ Binary: 48 bytes     │
└──────────┬───────────┘
           ▼
    TOTAL tiempo: ~100ms
    TOTAL bytes: ~206 bytes
    (vs HTTP/1.1: ~600ms, ~600 bytes)

    Ventajas:
    ✅ Multiplexing (múltiples requests simultaneos)
    ✅ Binary framing (más compacto)
    ✅ Header compression (HPACK)
    ✅ Server push (enviar datos no solicitados)
    ✅ Single connection (reutilizable)
```

---

## 9. SPRING BOOT AUTO-CONFIGURATION MAGIC

### ¿Cómo Spring Boot sabe qué configurar?

```
ANTES (Spring 2.0):
┌──────────────────────────────────┐
│ applicationContext.xml (PESADO)  │
│                                  │
│ <bean id="dataSource" ...>       │
│   <property name="url">          │
│     jdbc:mysql://localhost/db    │
│   </property>                    │
│ </bean>                          │
│                                  │
│ <bean id="transaction">          │
│ ...100+ líneas de XML...         │
│                                  │
└──────────────────────────────────┘

AHORA (Spring Boot 3.2):
┌──────────────────────────────────┐
│ application.properties (SIMPLE)  │
│                                  │
│ spring.datasource.url=jdbc:...   │
│ spring.datasource.username=root  │
│ spring.datasource.password=pass  │
│                                  │
│ # Spring Boot se autoconfigura   │
│ # según las dependencias en pom  │
│                                  │
└──────────────────────────────────┘

Maven POM:
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

Spring Boot detecta:
    ├─ spring-data-jpa está en classpath
    ├─ Busca @Configuration con @ConditionalOnClass
    ├─ Encuentra: DataSourceAutoConfiguration
    ├─ Encuentra: JpaRepositoriesAutoConfiguration
    ├─ Crea beans automáticamente
    ├─ Inyecta propiedades desde application.properties
    └─ ¡Listo! No necesitas configurar nada más

    La magia: @EnableAutoConfiguration (o @SpringBootApplication)
```

---

## 10. PROYECTO FINAL - ESTRUCTURA RECOMENDADA

### Account Mapper Service (Implementado)

```
account-mapper-service/
│
├─ pom.xml
│  └─ Dependencias: Spring Boot, gRPC, Protobuf
│
├─ src/main/java/com/upi/bank/accountmapper/
│  │
│  ├─ AccountMapperApplication.java (@SpringBootApplication)
│  │
│  ├─ controller/
│  │  └─ AccountMapperGrpcController.java (gRPC Service Impl)
│  │
│  ├─ service/
│  │  └─ AccountMapperService.java (@Service)
│  │
│  ├─ repository/
│  │  └─ VpaRepository.java (@Repository)
│  │
│  ├─ model/
│  │  └─ AccountInfo.java (record/POJO)
│  │
│  └─ exception/
│     └─ AccountNotFoundException.java
│
├─ src/main/resources/
│  └─ application.properties (grpc.server.port=9090)
│
└─ target/
   └─ account-mapper-service-1.0.0-SNAPSHOT.jar
```

### Estructura Recomendada para Nuevos Servicios

```
[NUEVO-SERVICIO]/
│
├─ pom.xml
│  ├─ Parent: upi-banco-emisor-e2e
│  ├─ Dependencias específicas del servicio
│  └─ grpc, protobuf, spring-boot
│
├─ src/main/java/com/upi/bank/[servicio-name]/
│  │
│  ├─ [Servicio]Application.java
│  │  └─ @SpringBootApplication
│  │     public static void main() { }
│  │
│  ├─ controller/ (si es HTTP REST)
│  │  └─ [Servicio]Controller.java
│  │
│  ├─ grpc/ (si usa gRPC)
│  │  └─ [Servicio]GrpcService.java
│  │
│  ├─ service/
│  │  └─ [Servicio]Service.java (@Service)
│  │     └─ Lógica de negocio
│  │
│  ├─ repository/
│  │  └─ [Entidad]Repository.java (@Repository)
│  │     └─ Acceso a datos
│  │
│  ├─ model/ (o entity/)
│  │  ├─ [Entidad]Entity.java (@Entity)
│  │  └─ DTOs (Data Transfer Objects)
│  │
│  ├─ exception/
│  │  └─ [Servicio]Exception.java
│  │
│  └─ config/
│     └─ [Servicio]Config.java (@Configuration)
│        └─ Beans personalizados
│
├─ src/main/resources/
│  ├─ application.properties
│  │  ├─ server.port=[PUERTO]
│  │  ├─ spring.application.name=[NAME]
│  │  └─ spring.datasource.* (si usa DB)
│  │
│  └─ application-production.properties
│
├─ src/test/java/ (Tests unitarios)
│  └─ com/upi/bank/[servicio-name]/
│
└─ target/ (Generado por Maven)
   └─ [servicio]-1.0.0-SNAPSHOT.jar
```

---

## 11. RESUMEN VISUAL FINAL

```
UPI BANCO EMISOR E2E
│
├─ 📦 MODULES (10 Microservicios)
│  ├─ account-mapper-service (✅ IMPLEMENTADO)
│  ├─ bank-upi-switch (❌ PLANEADO - ORQUESTADOR)
│  ├─ device-binding-service (❌ PLANEADO)
│  ├─ fraud-detection-engine (❌ PLANEADO)
│  ├─ high-speed-authorizer (❌ PLANEADO)
│  ├─ hsm-crypto-service (❌ PLANEADO)
│  ├─ mandate-autopay-service (❌ PLANEADO)
│  ├─ mock-core-banking (❌ PLANEADO)
│  ├─ reconciliation-service (❌ PLANEADO)
│  ├─ saga-reversal-service (❌ PLANEADO)
│  └─ upi-shared-proto (📦 MÓDULO BASE - Protobuffers)
│
├─ 🔧 TECHNOLOGIES
│  ├─ Java 21 (Lenguaje)
│  ├─ Spring Boot 3.2.4 (Framework)
│  ├─ gRPC 1.62.2 (Comunicación)
│  ├─ Protocol Buffers 3.25.3 (Serialización)
│  └─ Maven 3.9+ (Build)
│
├─ 🏗️ ARQUITECTURA
│  ├─ Microservicios independientes
│  ├─ Comunicación gRPC (HTTP/2)
│  ├─ cada servicio su DB (cuando se implemente)
│  └─ Patrón SAGA para transacciones distribuidas
│
├─ 📊 PUERTOS asignados
│  ├─ 9090: Account Mapper
│  ├─ 9091: Bank UPI Switch
│  ├─ 9092: Device Binding
│  ├─ 9093: Fraud Detection
│  ├─ 9094: High-Speed Authorizer
│  ├─ 9095: Mandate Autopay
│  ├─ 9096: Mock Core Banking
│  ├─ 9097: HSM Crypto
│  ├─ 9098: Saga Reversal
│  └─ 9099: Reconciliation
│
└─ ✅ FLOW
   Usuario App → Bank Switch → [Múltiples servicios] → CORE → CONFIRMACIÓN
```

---

## CONCLUSIÓN

Esta es una arquitectura empresarial moderna de sistema UPI (Pago Unificado) implementada con:
- **Java 21** para máxima performance
- **Spring Boot** para desarrollo ágil
- **gRPC + HTTP/2** para comunicación ultrarrápida entre servicios
- **Microservicios** para escalabilidad y mantenibilidad
- **Patrón SAGA** para consistencia distribuida

Solo 1 de 10 servicios está implementado. Los próximos pasos son implementar los demás servicios siguiendo el mismo patrón.

