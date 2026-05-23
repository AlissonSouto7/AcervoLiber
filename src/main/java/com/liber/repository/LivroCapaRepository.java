package com.liber.repository;

import com.liber.entity.LivroCapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LivroCapaRepository extends JpaRepository<LivroCapa, Long> {
}
