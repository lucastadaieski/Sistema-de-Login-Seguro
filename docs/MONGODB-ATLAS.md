# Integração com o MongoDB Atlas

## 1. Criando o banco no Atlas

1. Crie uma conta em <https://www.mongodb.com/cloud/atlas/register> (pode usar a conta Google).
2. **Criar cluster:** escolha o plano **M0 (Free)**, provedor **AWS** e região **São Paulo (sa-east-1)**, que reduz a latência para usuários no Brasil. Nome sugerido: `Cluster0`.
3. **Usuário do banco** (*Security > Database Access > Add New Database User*):
   - Método: *Password*;
   - Usuário: `loginseguro_app`;
   - Senha: use **Autogenerate Secure Password** e guarde-a;
   - Privilégios: *Specific Privileges* > `readWrite` no database `loginseguro`. (Se preferir simplificar, *Read and write to any database* também funciona, mas é menos restritivo.)
4. **Acesso de rede** (*Security > Network Access > Add IP Address*):
   - *Add Current IP Address* para liberar sua máquina;
   - Evite `0.0.0.0/0` (qualquer IP), exceto temporariamente para testes.
5. **String de conexão** (*Database > Connect > Drivers*, driver Java): copie a URI e substitua `<db_password>` pela senha do passo 3.

## 2. Como a aplicação se conecta

Em `src/main/resources/application.properties`:

```properties
spring.config.import=optional:file:.env[.properties]
spring.data.mongodb.uri=${MONGODB_URI}
spring.data.mongodb.database=${MONGODB_DATABASE:loginseguro}
spring.data.mongodb.auto-index-creation=true
spring.session.mongodb.collection-name=sessoes
```

- `MONGODB_URI` vem do arquivo `.env` (ignorado pelo Git) ou de uma variável de ambiente. O arquivo versionado não contém nenhuma senha.
- O Spring Boot cria automaticamente o `MongoClient`, o `MongoTemplate` e os repositórios.
- `auto-index-creation=true` faz o Spring criar o índice único do campo `email` a partir da anotação `@Indexed(unique = true)`.
- O Spring Session usa a mesma conexão para gravar as sessões na coleção `sessoes`.

## 3. Segurança da conexão

| Camada | Como está protegida |
|---|---|
| Transporte | O esquema `mongodb+srv://` ativa **TLS** automaticamente; o tráfego entre a aplicação e o Atlas é criptografado |
| Autenticação | Usuário e senha específicos da aplicação (SCRAM), diferentes da sua conta do Atlas |
| Autorização | Usuário com `readWrite` apenas no database `loginseguro` (princípio do menor privilégio) |
| Rede | Lista de IPs liberados (*Network Access*) |
| Credenciais | Fora do código: `.env` local (no `.gitignore`) ou variáveis de ambiente no servidor |
| Dados sensíveis | Senhas gravadas somente como hash BCrypt |

**Se uma senha vazar** (por exemplo, `.env` enviado ao GitHub por engano): troque a senha do usuário em *Database Access* imediatamente e atualize o `.env`. Apagar o arquivo do repositório não basta, pois ele continua no histórico do Git.

## 4. Coleções criadas

### `usuarios`

```json
{
  "_id": ObjectId("..."),
  "nome": "Maria Silva",
  "email": "maria@teste.com",
  "senhaHash": "{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMye...",
  "perfil": "USUARIO",
  "ativo": true,
  "tentativasFalhas": 0,
  "bloqueadoAte": null,
  "tema": "escuro",
  "criadoEm": ISODate("2026-10-08T22:00:00Z"),
  "ultimoLogin": ISODate("2026-10-08T22:05:00Z"),
   "_class": "br.com.lucas.loginseguro.model.Usuario"
}
```

Índice: `email` único.

### `sessoes`

Criada pelo Spring Session. Cada documento é uma sessão ativa, com o ID da sessão, o usuário (`principal`), o conteúdo serializado (contexto de segurança, tema, token CSRF) e a data de expiração (`expireAt`). Um índice **TTL** em `expireAt` faz o próprio MongoDB apagar sessões expiradas.

## 5. Conferindo no Atlas

Em *Database > Browse Collections*:
- após um cadastro, veja o documento em `usuarios` com a senha em formato `{bcrypt}...`;
- após um login, veja o documento em `sessoes`; após o logout, ele desaparece.

## 6. Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Could not resolve placeholder 'MONGODB_URI'` | `.env` não encontrado | Confirme que o `.env` está na raiz do projeto e que a execução usa essa pasta como diretório de trabalho |
| `MongoTimeoutException` / timeout ao iniciar | IP não liberado | *Network Access > Add Current IP Address* |
| `Authentication failed` | Usuário/senha errados ou senha com caracteres especiais | Confira em *Database Access*; codifique caracteres especiais na URI (`@` = `%40`) |
| `not authorized on loginseguro` | Privilégio em outro database | Dê `readWrite` no database `loginseguro` |
