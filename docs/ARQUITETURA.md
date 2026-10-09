# Arquitetura e decisões de design

Este documento explica como o sistema está organizado, como as peças se conectam e por que cada decisão foi tomada, para facilitar manutenção e adaptações futuras.

## 1. Visão geral

```
 Navegador
    │  HTTP (formulários com token CSRF, cookie de sessão SESSION)
    ▼
┌──────────────────────────────────────────────────────────┐
│ Spring Security (cadeia de filtros)                      │
│  • Spring Session: carrega/grava a sessão no MongoDB     │
│  • CSRF, cabeçalhos de segurança                         │
│  • Login por formulário / logout                         │
│  • Regras de acesso por URL e perfil                     │
└──────────────────────────────────────────────────────────┘
    ▼
 Controllers (controller/)  ──►  Templates Thymeleaf (templates/)
    │                               ▲  layout base + tema ativo
    ▼                               │  (TemaControllerAdvice)
 Services (service/)  ── @PreAuthorize (2ª camada de autorização)
    │
    ▼
 Repository (Spring Data)  ──►  MongoDB Atlas
                                 • usuarios
                                 • sessoes
```

A aplicação é um monólito em camadas, renderizado no servidor. É a arquitetura mais simples que atende aos requisitos e, por guardar a sessão no banco, continua escalável horizontalmente (várias instâncias atrás de um balanceador compartilham o mesmo login).

## 2. Camadas e pacotes

| Pacote | Responsabilidade | Principais classes |
|---|---|---|
| `config` | Configuração do Spring Security e propriedades tipadas | `SecurityConfig`, `TemaProperties`, `SegurancaProperties`, `AdminProperties`, `AdminInicialRunner` |
| `model` | Documentos do MongoDB | `Usuario`, `Perfil` |
| `repository` | Acesso a dados (Spring Data gera a implementação) | `UsuarioRepository` |
| `dto` | Objetos de formulário, separados da entidade | `CadastroForm`, `PerfilForm`, `TrocaSenhaForm` |
| `service` | Regras de negócio | `UsuarioService`, `UsuarioDetailsService`, `SessaoService`, `TemaService` |
| `controller` | Rotas HTTP, sem regra de negócio | `AuthController`, `UsuarioController`, `ModeradorController`, `AdminController` |
| `web` | Integração com a camada web | `LoginSucessoHandler`, `LoginFalhaHandler`, `TemaControllerAdvice`, `FormatoData` |

Regra geral: **controller** recebe e valida a requisição, **service** decide, **repository** persiste, **template** apenas exibe.

## 3. Fluxo de autenticação

### Cadastro
1. `GET /cadastro` exibe o formulário ligado a um `CadastroForm`.
2. `POST /cadastro` valida com Bean Validation (`@Valid`): nome, e-mail, senha forte (regex) e confirmação (`@AssertTrue`).
3. `UsuarioService.cadastrar` normaliza o e-mail (minúsculas), verifica duplicidade, gera o hash BCrypt e grava com perfil `USUARIO`.
4. O índice único do MongoDB em `email` garante a unicidade mesmo com cadastros simultâneos.
5. Redireciona para `/login` (padrão Post/Redirect/Get).

### Login
1. O `POST /login` é tratado pelo próprio Spring Security.
2. `UsuarioDetailsService` busca o usuário pelo e-mail e informa ao Spring o hash, o perfil e se a conta está ativa/bloqueada.
3. O Spring compara a senha digitada com o hash (`PasswordEncoder.matches`).
4. **Sucesso:** `LoginSucessoHandler` zera as tentativas, grava o último acesso, coloca o tema do usuário na sessão e redireciona para `/painel`. O ID da sessão é trocado (anti fixação de sessão) e a sessão é gravada na coleção `sessoes`.
5. **Falha:** `LoginFalhaHandler` incrementa as tentativas; ao chegar a 5, bloqueia a conta por 15 minutos. Exibe mensagens distintas para credencial inválida, conta bloqueada e conta inativa.

### Logout
`POST /logout` (com CSRF) invalida a sessão, remove o documento correspondente no MongoDB e apaga o cookie.

## 4. Autorização

### Perfis e hierarquia
`Perfil` é um enum com `USUARIO`, `MODERADOR` e `ADMIN`. No `SecurityConfig`:

```java
RoleHierarchyImpl.fromHierarchy("""
        ROLE_ADMIN > ROLE_MODERADOR
        ROLE_MODERADOR > ROLE_USUARIO
        """);
```

Com isso, uma regra `hasRole('MODERADOR')` aceita também administradores, sem repetir `hasAnyRole(...)` em todo lugar. A mesma hierarquia é aplicada às URLs, ao `@PreAuthorize` e ao `sec:authorize` dos templates.

### Duas camadas de proteção
1. **Por rota** (`SecurityConfig`): `/admin/**` exige ADMIN; `/moderador/**` exige MODERADOR; o resto exige login, exceto páginas públicas.
2. **Por método** (`@PreAuthorize` em `UsuarioService`): mesmo que uma rota seja configurada errada no futuro, a operação sensível continua protegida.

Além disso, regras de negócio finas ficam no serviço: moderador só altera contas de usuários comuns; ninguém altera a própria conta pela tela de gestão.

> Esconder botões no template (`sec:authorize`) é apenas conveniência visual. A segurança real está no servidor.

## 5. Sessões no MongoDB

O Spring Session substitui a sessão em memória do Tomcat por documentos na coleção `sessoes`. Vantagens:

- **Escalabilidade:** várias instâncias da aplicação compartilham as sessões.
- **Resiliência:** reiniciar a aplicação não desloga os usuários.
- **Controle:** é possível encerrar todas as sessões de um usuário (`SessaoService.encerrarSessoesDe`), usado ao desativá-lo, excluí-lo ou alterar seu perfil, para a mudança valer imediatamente.

As sessões expiram após 30 minutos de inatividade (`SESSION_TIMEOUT`), e o próprio MongoDB as remove por meio de um índice TTL.

O objeto guardado na sessão é o `User` do Spring Security (pequeno e serializável), e não a entidade `Usuario`, para evitar dados desatualizados e problemas de serialização.

## 6. Camada visual desacoplada e temas

Objetivo: permitir novos visuais sem mexer em Java nem nas páginas.

1. **Layout único** (`templates/layouts/base.html`): todas as páginas usam `layout:decorate="~{layouts/base}"` e só preenchem o fragmento `conteudo`. Cabeçalho, menu e rodapé ficam em `templates/fragments/`.
2. **CSS em duas camadas:** `css/base.css` define estrutura e componentes usando apenas variáveis CSS; `temas/<nome>/tema.css` define os valores das variáveis (e, se quiser, regras extras).
3. **Seleção do tema:** `TemaService` resolve o tema da requisição (preferência do usuário guardada na sessão, ou o padrão de `app.temas.padrao`). O `TemaControllerAdvice` injeta `temaAtivo` em todas as páginas; nenhum controller conhece temas.
4. **Configuração:** a lista `app.temas.disponiveis` define quais temas podem ser escolhidos; nomes fora da lista são ignorados (evita injeção de caminhos).

### Possíveis evoluções
- Tema por organização/subdomínio: trocar a lógica de `TemaService.resolver`.
- Templates diferentes por tema: criar `templates/temas/<nome>/layout.html` e usar `layout:decorate="~{__${layoutAtivo}__}"`, injetando `layoutAtivo` pelo mesmo advice.
- Temas cadastrados no banco: trocar `TemaProperties` por uma coleção `temas`.

## 7. Decisões de segurança

| Decisão | Motivo |
|---|---|
| `DelegatingPasswordEncoder` (BCrypt) | Hash lento e com sal; o prefixo `{bcrypt}` permite migrar de algoritmo sem invalidar senhas antigas |
| Senha de 8 a 64 caracteres com maiúscula, minúscula e número | Senhas fortes; limite de 64 porque o BCrypt ignora bytes após o 72º |
| DTOs separados da entidade | Impede "mass assignment" (ex.: enviar `perfil=ADMIN` no cadastro) |
| Mensagem genérica no login | Não revela se o e-mail existe |
| Bloqueio após 5 falhas | Dificulta ataques de força bruta |
| CSRF ativo e logout só por POST | Impede ações forjadas a partir de outros sites |
| Novo ID de sessão no login | Previne fixação de sessão |
| Cookie `HttpOnly`, `SameSite=Lax` e `Secure` configurável | Protege o cookie de scripts e de envios entre sites |
| `Content-Security-Policy` sem scripts inline | Reduz o impacto de XSS (por isso o JS fica em arquivo separado) |
| Thymeleaf `th:text` | Escapa HTML automaticamente |
| Credenciais em variáveis de ambiente / `.env` | Nada sensível no repositório público |
| Admin inicial via variáveis | Não há senha padrão embutida no código |

## 8. Como estender

- **Novo perfil:** adicionar ao enum `Perfil`, incluir na hierarquia do `SecurityConfig` e criar as regras de rota necessárias.
- **Nova página protegida:** criar o controller, o template com `layout:decorate="~{layouts/base}"` e a regra em `authorizeHttpRequests`.
- **Novo campo no usuário:** adicionar em `Usuario` (o MongoDB não exige migração de esquema) e no DTO correspondente.
- **Recuperação de senha por e-mail:** novo `TokenService` com coleção `tokens` (TTL) e `spring-boot-starter-mail`.
