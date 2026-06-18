package com.dotsolution.dot.leave.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "public_holidays", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"country", "holiday_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;
}
