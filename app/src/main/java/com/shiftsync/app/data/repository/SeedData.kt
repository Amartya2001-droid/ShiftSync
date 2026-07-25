package com.shiftsync.app.data.repository

import com.shiftsync.app.domain.model.Employee
import com.shiftsync.app.domain.model.RestaurantRole
import com.shiftsync.app.domain.model.Shift
import com.shiftsync.app.domain.model.ShiftStatus
import java.time.LocalDate
import java.time.LocalTime

object SeedData {
    const val currentEmployeeId = "emp-maya"

    val employees = listOf(
        Employee(currentEmployeeId, "Maya Chen", RestaurantRole.Server, 1850),
        Employee("emp-jon", "Jon Patel", RestaurantRole.LineCook, 2100),
        Employee("emp-sam", "Sam Rivera", RestaurantRole.Host, 1650),
        Employee("emp-lina", "Lina Okafor", RestaurantRole.Manager, 2800)
    )

    fun shifts(today: LocalDate = LocalDate.now()): List<Shift> {
        val monday = today.with(java.time.DayOfWeek.MONDAY)
        return listOf(
            Shift("shift-1", currentEmployeeId, RestaurantRole.Server, "Patio", monday.atTime(11, 0), monday.atTime(17, 0), ShiftStatus.Confirmed),
            Shift("shift-2", currentEmployeeId, RestaurantRole.Server, "Bar Rail", monday.plusDays(2).atTime(17, 0), monday.plusDays(2).atTime(23, 0), ShiftStatus.PendingTrade),
            Shift("shift-3", currentEmployeeId, RestaurantRole.Server, "Main Dining", monday.plusDays(4).atTime(16, 0), monday.plusDays(4).atTime(23, 30), ShiftStatus.Confirmed),
            Shift("shift-4", null, RestaurantRole.Host, "Front Door", monday.plusDays(1).atTime(10, 0), monday.plusDays(1).atTime(16, 0), ShiftStatus.UpForGrabs),
            Shift("shift-5", null, RestaurantRole.Server, "Patio", monday.plusDays(3).atTime(12, 0), monday.plusDays(3).atTime(18, 0), ShiftStatus.UpForGrabs),
            Shift("shift-6", "emp-jon", RestaurantRole.LineCook, "Grill", monday.plusDays(5).atTime(LocalTime.of(15, 0)), monday.plusDays(5).atTime(LocalTime.of(23, 0)), ShiftStatus.Confirmed)
        )
    }
}
