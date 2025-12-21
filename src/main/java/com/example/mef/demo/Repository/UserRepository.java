package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface    UserRepository extends CrudRepository<User, Integer> {

   User findByUsername(String name);
    
    


}
