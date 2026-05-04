package br.com.ecomel.security;

import br.com.ecomel.domain.entity.Usuario;
import br.com.ecomel.domain.enums.StatusUsuario;
import br.com.ecomel.exception.BusinessException;
import br.com.ecomel.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    // =====================================================
    // 🔐 AUTENTICAÇÃO POR CÓDIGO DA CARTEIRA (PADRÃO CRIPTO)
    // =====================================================

    @Override
    public UserDetails loadUserByUsername(String codigoCarteira) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository
                .findByCodigoCarteira(codigoCarteira, StatusUsuario.ATIVO)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Carteira não encontrada: " + codigoCarteira)
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(usuario.getCarteira().getCodigoCarteira()) // 🔥 identidade real
                .password(usuario.getSenha())
                .authorities(Collections.emptyList()) // adicionar roles se necessário
                .accountLocked(!usuario.isAtivo())
                .disabled(!usuario.isAtivo())
                .build();
    }

    // =====================================================
    // 🔁 RETRY PARA CONCORRÊNCIA (LOCK OTIMISTA)
    // =====================================================

    public void executarComRetry(Runnable operacao) {
        int tentativas = 0;
        int maxTentativas = 3;

        while (true) {
            try {
                operacao.run();
                return;

            } catch (ObjectOptimisticLockingFailureException e) {
                tentativas++;

                if (tentativas >= maxTentativas) {
                    throw new BusinessException("Sistema ocupado. Tente novamente.");
                }

                try {
                    Thread.sleep(50); // 🔹 pequeno backoff
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
