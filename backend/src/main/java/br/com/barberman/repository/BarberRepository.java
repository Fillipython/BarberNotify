package br.com.barberman.repository;

import br.com.barberman.model.Barber;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarberRepository extends JpaRepository<Barber, UUID> {
    List<Barber> findByIsActiveTrueOrderByNameAsc();
    Optional<Barber> findByIdAndIsActiveTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Barber b WHERE b.id = :id AND b.isActive = true")
    Optional<Barber> findByIdAndIsActiveTrueWithLock(@Param("id") UUID id);
}

