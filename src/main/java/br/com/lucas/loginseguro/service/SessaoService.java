package br.com.lucas.loginseguro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

/**
 * Operações sobre as sessões HTTP gravadas no MongoDB (coleção "sessoes").
 *
 * Como as sessões ficam no banco, e não na memória do servidor, é possível
 * encerrar à força as sessões de um usuário: útil quando ele é desativado
 * ou muda de perfil, para que a mudança valha imediatamente.
 */
@Service
public class SessaoService {

    private static final Logger log = LoggerFactory.getLogger(SessaoService.class);
    private static final String COLECAO_SESSOES = "sessoes";

    private final FindByIndexNameSessionRepository<? extends Session> sessoes;
    private final MongoTemplate mongoTemplate;

    public SessaoService(FindByIndexNameSessionRepository<? extends Session> sessoes,
                         MongoTemplate mongoTemplate) {
        this.sessoes = sessoes;
        this.mongoTemplate = mongoTemplate;
    }

    /** Remove do banco todas as sessões abertas do usuário (força novo login). */
    public void encerrarSessoesDe(String email) {
        try {
            sessoes.findByPrincipalName(email).keySet().forEach(sessoes::deleteById);
        } catch (RuntimeException e) {
            // Falha aqui não deve impedir a operação principal (ex.: desativar a conta)
            log.warn("Não foi possível encerrar as sessões de {}: {}", email, e.getMessage());
        }
    }

    /** Quantidade de sessões gravadas no MongoDB neste momento. */
    public long contarSessoesAtivas() {
        if (!mongoTemplate.collectionExists(COLECAO_SESSOES)) {
            return 0;
        }
        return mongoTemplate.getCollection(COLECAO_SESSOES).countDocuments();
    }
}
