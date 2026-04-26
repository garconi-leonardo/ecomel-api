# 🐝 ECOMEL - Backend Ecosystem

Sistema de alta performance para gerenciamento de ativos criptográficos com valorização programada via Índice Global.

## 🚀 Tecnologias
- **Java 21** & **Spring Boot 3.2**
- **PostgreSQL** (Banco de dados relacional)
- **Flyway** (Migração de banco)
- **Redis** (Opcional para cache de ordens/matching futuramente)
- **Docker** (Containerização)
- **MapStruct** & **Lombok** (Produtividade)

## 🧮 Regras de Negócio Core
- **Redistribuição Global:** 10% de taxa em depósitos/saques.
  - 5.00% -> Revaloriza a moeda ECM através do `IndiceGlobal`.
  - 4.01% -> Distribuído aos detentores de `FAVOS`.
  - 0.99% -> Taxa operacional (Sistema).

## 🛠️ Arquitetura de Saldo
Utilizamos o conceito de **Saldo Base vs Saldo Real**.
- `Saldo Real = Saldo Base * Indice Global`
- Isso evita o processamento O(n) ao atualizar milhões de carteiras simultaneamente.

## 🏁 Como Rodar
1. Certifique-se de ter o Docker instalado.
2. Clone o repositório.
3. Execute `docker-compose up -d` para subir o PostgreSQL.
4. Execute `./mvnw spring-boot:run`.

## 📖 Documentação API
Acesse: `http://localhost:8080/swagger-ui.html`
