package com.smarthospital.doctor;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {

    Optional<DoctorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Optional<DoctorProfile> findByLicenseNumber(String licenseNumber);

    List<DoctorProfile> findByDepartment_IdAndActiveTrueOrderByUser_FullNameAsc(Long departmentId);

    List<DoctorProfile> findByActiveTrueOrderByUser_FullNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DoctorProfile d join fetch d.user where d.id = :doctorId")
    Optional<DoctorProfile> findByIdForUpdate(@Param("doctorId") Long doctorId);
}
