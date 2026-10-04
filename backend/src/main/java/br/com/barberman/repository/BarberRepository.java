package br.com.barberman.repository;

import br.com.barberman.model.Barber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarberRepository extends JpaRepository<Barber, UUID> {
    List<Barber> findByIsActiveTrueOrderByNameAsc();
    Optional<Barber> findByIdAndIsActiveTrue(UUID id);
}
