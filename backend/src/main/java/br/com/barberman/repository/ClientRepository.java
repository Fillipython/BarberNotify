package br.com.barberman.repository;

import br.com.barberman.model.Client;
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
