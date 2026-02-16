package me.davidgomes.demo

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.spyk
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock

/**
 * Creates a mock player and stubs functions that are not implemented in BukkitMock
 * source: https://docs.mockbukkit.org/docs/en/user_guide/introduction/first_test.html#unimplementedoperationexception
 */
fun createPlayer(server: ServerMock): PlayerMock =
    spyk(server.addPlayer()).apply {
        every { resetTitle() } just Runs
    }
