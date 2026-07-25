package com.shiftsync.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shiftsync.app.domain.model.Employee
import com.shiftsync.app.domain.model.RestaurantRole

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: RestaurantRole,
    val hourlyRateCents: Int
) {
    fun toDomain(): Employee = Employee(id, name, role, hourlyRateCents)
}

fun Employee.toEntity(): EmployeeEntity = EmployeeEntity(id, name, role, hourlyRateCents)
