package com.shiftsync.app.domain.model

data class Employee(
    val id: String,
    val name: String,
    val role: RestaurantRole,
    val hourlyRateCents: Int
)

enum class RestaurantRole {
    LineCook,
    Server,
    Host,
    Bartender,
    Manager
}
