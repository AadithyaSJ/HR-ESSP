package com.dotsolution.dot.leave.repository;

import com.dotsolution.dot.leave.entity.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {
    List<PublicHoliday> findByCountry(String country);
    Optional<PublicHoliday> findByCountryAndHolidayDate(String country, LocalDate holidayDate);
}
