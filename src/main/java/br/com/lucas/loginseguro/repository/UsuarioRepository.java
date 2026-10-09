package br.com.lucas.loginseguro.repository;

import br.com.lucas.loginseguro.model.Perfil;
import br.com.lucas.loginseguro.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Acesso à coleção "usuarios".
 * O Spring Data gera a implementação a partir do nome dos métodos.
 */
public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPerfil(Perfil perfil);

    long countByPerfil(Perfil perfil);
}
