package com.example.mef.demo.Repository;

public interface AnneeScolaireRepository
        extends JpaRepository<AnneeScolaire, String> {
}

public interface StudentGuardianRepository
        extends JpaRepository<StudentGuardian, String> {
}

public interface EmployeeClassroomRepository
        extends JpaRepository<EmployeeClassroom, String> {
}

public interface InscriptionRepository
        extends JpaRepository<Inscription, String> {
}

public interface AbsenceRepository
        extends JpaRepository<Absence, String> {
}

public interface DailyLogRepository
        extends JpaRepository<DailyLog, String> {
}

public interface MilestoneRepository
        extends JpaRepository<Milestone, String> {
}

public interface StudentMilestoneRepository
        extends JpaRepository<StudentMilestone, String> {
}

public interface SessionPricingRepository
        extends JpaRepository<SessionPricing, String> {
}

public interface KitchenNeedRepository
        extends JpaRepository<KitchenNeed, String> {
}

public interface PurchaseItemRepository
        extends JpaRepository<PurchaseItem, String> {
}
