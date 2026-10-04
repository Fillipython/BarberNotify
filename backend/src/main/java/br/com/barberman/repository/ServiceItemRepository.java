package br.com.barberman.repository;

import br.com.barberman.model.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
    List<ServiceItem> findByIsActiveTrueOrderByNameAsc();
    List<ServiceItem> findByIdInAndIsActiveTrue(List<UUID> ids);
}
