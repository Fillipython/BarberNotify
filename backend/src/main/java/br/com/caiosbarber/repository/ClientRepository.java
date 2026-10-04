package br.com.caiosbarber.repository;

import br.com.caiosbarber.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByPhone(String phone);
    Optional<Client> findByEmail(String email);
    boolean existsByPhone(String phone);
}
