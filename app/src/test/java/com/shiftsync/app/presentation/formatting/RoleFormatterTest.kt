package com.shiftsync.app.presentation.formatting

import com.shiftsync.app.domain.model.RestaurantRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoleFormatterTest {
    @Test
    fun `formats compound restaurant role labels`() {
        assertEquals("Line Cook", RoleFormatter.label(RestaurantRole.LineCook))
    }

    @Test
    fun `formats simple restaurant role labels`() {
        assertEquals("Server", RoleFormatter.label(RestaurantRole.Server))
    }
}
