# Login Seguro

Sistema web de autenticação e autorização desenvolvido com **Java 17, Spring Boot 3, Spring Security, Thymeleaf e MongoDB Atlas**.

Permite cadastro, login e logout de usuários, com senhas protegidas por hash BCrypt, controle de acesso por quatro perfis (Usuário, Técnico, Moderador e Administrador), sessões armazenadas no MongoDB uma camada visual desacoplada, preparada para receber novos temas sem alterar código Java, e uma API que emite **tokens JWT** para outros microsserviços, como o [ThinDesk](https://github.com/lucastadaieski/sistema-de-chamados).

[![CI](https://github.com/lucastadaieski/Sistema-de-Login-Seguro/actions/workflows/ci.yml/badge.svg)](https://github.com/lucastadaieski/Sistema-de-Login-Seguro/actions)

---

## Sumário

1. [Funcionalidades](#funcionalidades)
2. [Tecnologias](#tecnologias)
3. [Perfis e permissões](#perfis-e-permissões)
4. [Pré-requisitos](#pré-requisitos)
5. [Configurando o MongoDB Atlas](#configurando-o-mongodb-atlas)
6. [Configurando o projeto](#configurando-o-projeto)
7. [Executando](#executando)
8. [Testando os perfis](#testando-os-perfis)
9. [Estrutura do projeto](#estrutura-do-projeto)
10. [Temas visuais](#temas-visuais)
11. [Testes automatizados](#testes-automatizados)
12. [Documentação complementar](#documentação-complementar)

---

## Funcionalidades

**Autenticação**
- Cadastro com validação de dados (nome, e-mail válido e único, senha forte, confirmação de senha)
- Login com página própria e logout seguro (POST com token CSRF)
- Senhas armazenadas apenas como hash **BCrypt** (nunca em texto puro)
- Bloqueio temporário da conta após 5 tentativas de senha erradas (configurável)
- Mensagem genérica de erro no login, para não revelar quais e-mails existem

**Autorização**
- Quatro perfis com hierarquia: `ADMIN > MODERADOR > USUARIO` e `ADMIN > TECNICO > USUARIO`
- Proteção em duas camadas: por rota (`SecurityConfig`) e por método (`@PreAuthorize`)
- Menus e botões exibidos conforme o perfil (`sec:authorize` no Thymeleaf)
- Páginas próprias para acesso negado (403) e página não encontrada (404)

**Sessões**
- Sessões HTTP gravadas no MongoDB Atlas (coleção `sessoes`), via Spring Session
- Novo ID de sessão a cada login (proteção contra fixação de sessão)
- Ao desativar um usuário ou alterar seu perfil, as sessões dele são encerradas na hora

**Interface**
- Thymeleaf com layout base reutilizável (Thymeleaf Layout Dialect)
- Três temas prontos (claro, escuro, alto contraste), escolhidos pelo próprio usuário
- Novos temas são adicionados apenas com um arquivo CSS

**Segurança adicional**
- Proteção CSRF em todos os formulários
- Cabeçalhos `Content-Security-Policy` e `X-Frame-Options`
- Cookie de sessão `HttpOnly` e `SameSite=Lax`
- Credenciais do banco fora do código (variáveis de ambiente / arquivo `.env` ignorado pelo Git)

---

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 17 | Linguagem |
| Spring Boot 3.3 | Base da aplicação |
| Spring Security 6 | Autenticação, autorização, CSRF, hash de senhas |
| Spring Data MongoDB | Persistência dos usuários |
| Spring Session MongoDB | Persistência das sessões |
| Spring Security OAuth2 JOSE | Emissão de tokens JWT para outros microsserviços |
| Bean Validation | Validação dos formulários |
| Thymeleaf + Layout Dialect | Páginas HTML e layouts |
| thymeleaf-extras-springsecurity6 | Controle de exibição por perfil |
| MongoDB Atlas | Banco de dados na nuvem |
| JUnit 5 + MockMvc | Testes automatizados |
| GitHub Actions | Integração contínua |

---

## Perfis e permissões

| Recurso | Usuário | Técnico | Moderador | Administrador |
|---|:---:|:---:|:---:|:---:|
| Painel (`/painel`) e perfil (`/perfil`) | ✅ | ✅ | ✅ | ✅ |
| Escolher tema e trocar senha | ✅ | ✅ | ✅ | ✅ |
| Resolver chamados no ThinDesk | ❌ | ✅ | ❌ | ✅ |
| Listar usuários (`/moderador/usuarios`) | ❌ | ❌ | ✅ | ✅ |
| Ativar/desativar contas de usuários comuns | ❌ | ❌ | ✅ | ✅ |
| Ativar/desativar técnicos, moderadores e administradores | ❌ | ❌ | ❌ | ✅ |
| Alterar perfil de outros usuários | ❌ | ❌ | ❌ | ✅ |
| Excluir contas | ❌ | ❌ | ❌ | ✅ |
| Painel de administração (`/admin`) | ❌ | ❌ | ❌ | ✅ |

Todo novo cadastro recebe o perfil **Usuário**. Somente um administrador pode promover alguém. O primeiro administrador é criado automaticamente na primeira execução (veja abaixo).

---

## Pré-requisitos

- **JDK 17 ou superior** (`java -version`)
- **IntelliJ IDEA** (Community ou Ultimate), que já traz o Maven embutido; ou **Maven 3.9+** instalado
- Conta gratuita no **MongoDB Atlas**
- **Git**

---

## Configurando o MongoDB Atlas

Resumo (o passo a passo detalhado, com explicações de segurança, está em [`docs/MONGODB-ATLAS.md`](docs/MONGODB-ATLAS.md)):

1. Crie uma conta em <https://www.mongodb.com/cloud/atlas/register>.
2. Crie um cluster gratuito **M0** (sugestão: AWS, região São Paulo `sa-east-1`).
3. Em **Database Access**, crie um usuário de banco com senha forte e permissão **readWrite** no database `loginseguro`.
4. Em **Network Access**, libere o seu IP atual (**Add Current IP Address**).
5. Em **Database > Connect > Drivers**, copie a string de conexão. Ela tem este formato:

   ```
   mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0
   ```

> As coleções `usuarios` e `sessoes` e o índice único de e-mail são criados automaticamente na primeira execução.

---

## Configurando o projeto

### 1. Clonar o repositório

```bash
git clone https://github.com/lucastadaieski/Sistema-de-Login-Seguro.git
cd Sistema-de-Login-Seguro
```

### 2. Criar o arquivo `.env`

Copie o modelo e preencha com os seus dados:

```bash
cp .env.example .env        # Linux/macOS
copy .env.example .env      # Windows
```

```properties
MONGODB_URI=mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0
MONGODB_DATABASE=loginseguro
ADMIN_EMAIL=admin@loginseguro.com
ADMIN_PASSWORD=Troque@Esta1Senha
JWT_SECRET=<chave aleatória com pelo menos 32 caracteres, igual à do ThinDesk>
```

O arquivo `.env` está no `.gitignore`, portanto **suas credenciais nunca vão para o GitHub**. A aplicação o lê automaticamente pela linha `spring.config.import=optional:file:.env[.properties]` do `application.properties`.

> Se a senha do banco tiver caracteres especiais (`@`, `:`, `/`, `%`, `#`), eles precisam ser codificados na URI (ex.: `@` vira `%40`). O mais simples é usar a senha gerada automaticamente pelo Atlas, que só tem letras e números.

**Alternativa:** em vez do `.env`, você pode definir as mesmas variáveis de ambiente no sistema ou no IntelliJ (*Run > Edit Configurations > Environment variables*).

---

## API de token (integração com o ThinDesk)

Além das telas, o sistema oferece um endpoint para outros microsserviços:

```http
POST /api/auth/token
Content-Type: application/json

{ "email": "ana@exemplo.com", "senha": "MinhaSenha@123" }
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEm": "2026-10-23T13:00:00Z",
  "usuarioId": "6718f0c2a1b2c3d4e5f60718",
  "perfil": "USUARIO"
}
```

- A autenticação é a mesma da tela: mesmo `AuthenticationManager`, BCrypt, bloqueio por tentativas e contas desativadas.
- O token é um JWT assinado com HS256 usando a `JWT_SECRET`. Ele carrega `sub` (id do usuário), `email`, `nome` e `perfil`, e vale 60 minutos.
- O ThinDesk usa a **mesma** `JWT_SECRET` para validar o token sozinho, sem consultar este sistema.
- As rotas `/api/**` têm regras próprias no `SecurityConfig`: sem sessão, sem CSRF e resposta 401 em vez de redirecionar para a tela de login. As telas continuam funcionando como antes.

Erros: `400` sem e-mail ou senha; `401` com `{"erro": "..."}` para senha errada, conta bloqueada ou desativada.

---

## Executando

### Pelo IntelliJ IDEA

1. *File > Open* e selecione a pasta do projeto (o IntelliJ reconhece o `pom.xml` e baixa as dependências).
2. Confirme o JDK em *File > Project Structure > SDK* (17 ou superior).
3. Abra `src/main/java/br/com/lucas/loginseguro/LoginSeguroApplication.java` e clique no botão ▶ ao lado de `main`.

> O arquivo `.env` é procurado na pasta de trabalho da execução. No IntelliJ ela já é a raiz do projeto por padrão.

### Pelo terminal

```bash
mvn spring-boot:run
```

Ou gerando o `.jar`:

```bash
mvn clean package
java -jar target/login-seguro-1.0.0.jar
```

Acesse **<http://localhost:8080>**.

No log de inicialização deve aparecer `Administrador inicial criado: admin@loginseguro.com` na primeira execução.

---

## Testando os perfis

1. Entre com o administrador definido em `ADMIN_EMAIL` / `ADMIN_PASSWORD`.
2. Em outra janela anônima, crie duas contas pelo **Criar conta** (ex.: `moderador@teste.com` e `usuario@teste.com`).
3. Como administrador, vá em **Usuários** e altere o perfil de `moderador@teste.com` para **Moderador**.
4. Entre com cada conta e observe:
   - o **usuário** não vê o menu *Usuários* e recebe **403** ao acessar `/moderador/usuarios`;
   - o **moderador** vê a lista e pode desativar apenas usuários comuns, mas recebe **403** em `/admin`;
   - o **administrador** acessa tudo.
5. Erre a senha 5 vezes seguidas: a conta fica bloqueada por 15 minutos.
6. No Atlas (*Browse Collections*), confira as coleções `usuarios` (senhas como `{bcrypt}$2a$10$...`) e `sessoes`.

---

## Estrutura do projeto

```
login-seguro/
├── .env.example                  Modelo de configuração (copie para .env)
├── .github/workflows/ci.yml      Compilação e testes automáticos no GitHub
├── docs/
│   ├── ARQUITETURA.md            Estrutura e decisões de design
│   └── MONGODB-ATLAS.md          Integração com o Atlas em detalhes
├── pom.xml
└── src/
    ├── main/java/br/com/lucas/loginseguro/
    │   ├── LoginSeguroApplication.java
    │   ├── config/        Segurança, propriedades tipadas, admin inicial
    │   ├── controller/    Rotas: Auth, Usuario, Moderador, Admin
    │   ├── dto/           Formulários (separados da entidade)
    │   ├── model/         Usuario (documento) e Perfil (enum)
    │   ├── repository/    UsuarioRepository (Spring Data)
    │   ├── service/       Regras de negócio, sessões, temas
    │   └── web/           Handlers de login, injeção de tema, formatação
    ├── main/resources/
    │   ├── application.properties
    │   ├── templates/
    │   │   ├── layouts/base.html     Layout único de todas as páginas
    │   │   ├── fragments/            Cabeçalho, rodapé, alertas
    │   │   ├── auth/ usuario/ moderador/ admin/ error/
    │   │   └── index.html
    │   └── static/
    │       ├── css/base.css          Estrutura (só usa variáveis CSS)
    │       ├── js/confirmar.js
    │       └── temas/<nome>/tema.css Valores das variáveis de cada tema
    └── test/                          Testes unitários e de integração
```

Detalhes e justificativas em [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md).

---

## Temas visuais

O visual é separado em duas camadas:

- `static/css/base.css`: layout e componentes, usando **apenas variáveis CSS** (`var(--cor-primaria)` etc.);
- `static/temas/<nome>/tema.css`: define os **valores** dessas variáveis.

### Criando um tema novo

1. Crie a pasta `src/main/resources/static/temas/meu-tema/`.
2. Copie `temas/claro/tema.css` para ela e altere as cores/fontes.
3. Adicione o nome à lista em `application.properties`:

   ```properties
   app.temas.disponiveis=claro,escuro,alto-contraste,meu-tema
   ```

4. Reinicie a aplicação. O tema já aparece em **Meu perfil > Tema visual**.

Para mudar o tema padrão de todo o sistema: `TEMA_PADRAO=escuro` no `.env`.

---

## Testes automatizados

```bash
mvn test
```

- `CadastroFormValidacaoTest`: regras de validação do cadastro (roda sem banco).
- `LoginSeguroIntegracaoTest`: sobe a aplicação inteira e testa cadastro, hash de senha, CSRF, login, bloqueio por tentativas, sessões no MongoDB e o acesso de cada perfil. Roda automaticamente no **GitHub Actions** com um MongoDB temporário. Localmente, só roda com `CI=true` e um `MONGODB_URI` de **teste**, pois apaga os dados.

---

## Documentação complementar

- [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md): camadas, fluxo de autenticação, decisões de design e como estender.
- [`docs/MONGODB-ATLAS.md`](docs/MONGODB-ATLAS.md): criação do cluster, segurança da conexão e coleções.

---

**Autor:** Lucas do Nascimento Lima
