package com.prescription_Management_REST_API.repository;

import com.prescription_Management_REST_API.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {}
