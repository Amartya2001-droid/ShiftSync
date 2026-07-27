package com.shiftsync.app.presentation.formatting

import com.shiftsync.app.domain.model.RestaurantRole

object RoleFormatter {
    fun label(role: RestaurantRole): String {
        return when (role) {
            RestaurantRole.LineCook -> "Line Cook"
            RestaurantRole.Server -> "Server"
            RestaurantRole.Host -> "Host"
            RestaurantRole.Bartender -> "Bartender"
            RestaurantRole.Manager -> "Manager"
        }
    }
}
