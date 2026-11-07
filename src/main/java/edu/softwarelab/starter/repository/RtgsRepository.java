package edu.softwarelab.starter.repository;

import edu.softwarelab.starter.entity.RtgsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RtgsRepository extends JpaRepository<RtgsTransaction, String> {
}