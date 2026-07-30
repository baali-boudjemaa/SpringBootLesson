package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Expense;
import com.example.mef.demo.enums.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByCategory(ExpenseCategory category);

}
