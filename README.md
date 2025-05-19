# Serviço de Usuários Spring

Um microsserviço para gerenciamento de usuários que se integra com o Keycloak para autenticação e autorização.

## Visão Geral

O Serviço de Usuários Spring é uma aplicação Spring Boot que fornece uma API RESTful para operações de gerenciamento de usuários. Ele atua como uma fachada para o Keycloak, fornecendo endpoints simplificados para criar, atualizar, recuperar e excluir usuários com controle de acesso baseado em papéis.

## Tecnologias

- **Java 21**
- **Spring Boot 3.4.5**
- **Spring Cloud 2024.0.0**
- **Spring Security com OAuth2**
- **Spring WebFlux** para programação reativa
- **Keycloak 25.0.3** para autenticação e autorização
- **OpenAPI/Swagger** para documentação da API
- **Docker & Kubernetes** para conteinerização e orquestração
- **OpenTelemetry** para rastreamento distribuído
- **Micrometer & Prometheus** para métricas
- **Loki** para logging centralizado
- **RabbitMQ** para barramento de mensagens
- **MapStruct** para mapeamento de objetos
- **Lombok** para redução de código boilerplate
- **JUnit 5 & WireMock** para testes

## Estrutura do Projeto

```
spring-user/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── br/dev/leandro/spring/cloud/user/
│   │   │       ├── config/           # Classes de configuração
│   │   │       ├── controller/       # Controladores REST
│   │   │       ├── converter/        # Conversores de dados
│   │   │       ├── dto/              # Objetos de Transferência de Dados
│   │   │       ├── exception/        # Tratamento de exceções
│   │   │       ├── filter/           # Filtros de requisição
│   │   │       ├── keycloak/         # Integração com Keycloak
│   │   │       ├── model/            # Modelos de domínio
│   │   │       ├── service/          # Lógica de negócios
│   │   │       ├── utils/            # Classes utilitárias
│   │   │       └── SpringUserApplication.java
│   │   └── resources/
│   │       ├── application.yml       # Configuração da aplicação
│   │       ├── banner.txt            # Banner personalizado
│   │       └── logback-spring.xml    # Configuração de logging
│   └── test/
│       ├── java/                     # Classes de teste
│       └── resources/
│           └── application-test.yml  # Configuração de teste
├── Dockerfile                        # Arquivo de build Docker
├── docker-compose.yml                # Configuração do Docker Compose
├── pom.xml                           # Arquivo de build Maven
└── README.md                         # Este arquivo
```

## Funcionalidades

- **Gerenciamento de Usuários**: Criar, ler, atualizar e excluir usuários
- **Controle de Acesso Baseado em Papéis**: Diferentes endpoints para administradores, organizadores e participantes
- **Integração com Keycloak**: Autenticação e autorização seguras
- **Programação Reativa**: API não-bloqueante com WebFlux
- **Rastreamento Distribuído**: Rastreamento de requisições entre serviços
- **Logging Centralizado**: Agregação de logs com Loki
- **Documentação da API**: Swagger UI para fácil exploração da API
- **Conteinerização**: Suporte a Docker e Kubernetes
- **Gerenciamento de Configuração**: Configuração externalizada com Spring Cloud Config
- **Barramento de Mensagens**: Arquitetura orientada a eventos com RabbitMQ

## Endpoints da API

### Endpoints Públicos

- `GET /users/public/info` - Obter informações públicas
- `GET /users/public/hello` - Endpoint simples de saudação
- `GET /users/public/profile` - Obter perfil do usuário autenticado

### Endpoints de Administrador

- `POST /users/admin/create` - Criar um novo usuário
- `PUT /users/admin/{id}` - Atualizar um usuário
- `DELETE /users/admin/{id}` - Excluir um usuário
- `GET /users/admin/{id}` - Obter um usuário por ID
- `GET /users/admin` - Obter todos os usuários com paginação e busca
- `GET /users/admin/all` - Obter todos os usuários (apenas admin)

### Endpoints Específicos por Papel

- `GET /users/organizador/events` - Obter eventos para organizadores
- `GET /users/participante/profile` - Obter perfil do participante

## Configuração e Instalação

### Pré-requisitos

- Java 21
- Maven
- Docker e Docker Compose
- Servidor Keycloak

### Variáveis de Ambiente

A aplicação utiliza as seguintes variáveis de ambiente:

- `SPRING_PROFILES_ACTIVE` - Perfil Spring ativo
- `SPRING_CONFIG_SERVER` - URL do servidor Spring Cloud Config
- `LOKI_URL` - URL do servidor de logging Loki
- `SPRING_RABBITMQ_HOST` - Host do RabbitMQ
- `KEYCLOAK_URL` - URL do servidor Keycloak
- `KEYCLOAK_AUTH_SERVER_URL` - URL do servidor de autenticação Keycloak
- `KEYCLOAK_CLIENT_ID` - ID do cliente Keycloak
- `KEYCLOAK_CLIENT_SECRET` - Segredo do cliente Keycloak
- `KEYCLOAK_REALM` - Nome do realm Keycloak

### Desenvolvimento Local

1. Clone o repositório
2. Configure as variáveis de ambiente necessárias
3. Execute a aplicação:

```bash
mvn spring-boot:run
```

### Implantação com Docker

1. Construa a imagem Docker:

```bash
mvn clean package -P docker
```

2. Execute com Docker Compose:

```bash
docker-compose up -d
```

## Testes

A aplicação inclui testes de integração abrangentes usando WireMock para simular serviços externos.

Para executar os testes:

```bash
mvn test                # Executar testes unitários
mvn verify              # Executar testes de integração
```

## Configuração

A aplicação é configurada usando `application.yml`. Opções de configuração principais:

- Porta do servidor: 8091
- Configurações do servidor de recursos OAuth2 do Spring Security
- Configurações de integração com Keycloak
- Configuração de logging
- Caminhos OpenAPI/Swagger

## Monitoramento e Observabilidade

- **Verificações de Saúde**: `/actuator/health`
- **Métricas**: `/actuator/metrics` e `/actuator/prometheus`
- **Rastreamento Distribuído**: Integração com OpenTelemetry
- **Logging**: Logging centralizado com Loki

## Contribuindo

1. Faça um fork do repositório
2. Crie um branch para sua feature
3. Faça commit das suas alterações
4. Faça push para o branch
5. Crie um novo Pull Request

## Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo LICENSE para detalhes.
