package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Product;
import com.example.mef.demo.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product,String> {
    
}
