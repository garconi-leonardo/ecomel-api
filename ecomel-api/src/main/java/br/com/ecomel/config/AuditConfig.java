package br.com.ecomel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class AuditConfig {

    // O uso do ":ECOMEL" garante que, se a chave não for encontrada, 
    // o sistema use esse valor padrão em vez de lançar uma exceção.
    @Value("${user.ecomel:ECOMEL}")
    private String userEcomel;
	
    @Bean
    public AuditorAware<String> auditorProvider() {
        // O Lambda captura (ou deveria) o valor da variável injetada pelo @Value
        return () -> Optional.ofNullable(userEcomel);
    }
}
