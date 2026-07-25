package com.evogroup.minicrm.repository;

import com.evogroup.minicrm.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Page<Client> findAllByOrderByIdAsc(Pageable pageable);
    Page<Client> findByOwnerIdOrderByIdAsc(Long ownerId, Pageable pageable);
}
