package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface    UserRepository extends CrudRepository<User, Integer> {
   User findByUsername(String name);

    @Override
    Optional<User> findById(@NotNull Integer integer);
}
