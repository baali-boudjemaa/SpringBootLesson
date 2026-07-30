package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.AnneeScolaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnneeScolaireRepository extends JpaRepository<AnneeScolaire, Long> {

    Optional<AnneeScolaire> findByLibelleAnneesc(String libelleAnneesc);

    boolean existsByLibelleAnneesc(String libelleAnneesc);

}
