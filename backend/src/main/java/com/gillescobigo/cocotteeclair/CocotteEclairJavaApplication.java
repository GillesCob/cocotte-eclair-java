package com.gillescobigo.cocotteeclair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Note : au demarrage, Spring Security genere un avertissement "Using generated security
// password" (UserDetailsServiceAutoConfiguration par defaut). Sans consequence : l'auth
// reelle passe entierement par JWT (JwtAuthFilter + SecurityConfig), ce mot de passe genere
// n'est jamais utilise. Tentative d'exclusion explicite de cette auto-configuration abandonnee
// le 30/07 (classe introuvable sous son ancien nom/package en Boot 4 / Security 7, a reprendre
// si le nom exact est confirme plus tard plutot que de deviner).
@SpringBootApplication
public class CocotteEclairJavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CocotteEclairJavaApplication.class, args);
	}

}
