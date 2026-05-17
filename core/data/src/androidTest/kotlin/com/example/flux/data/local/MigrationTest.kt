package com.example.flux.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FluxDatabase::class.java,
    )

    @Test
    fun createV1_schemaMatchesExpected() {
        helper.createDatabase(DB_NAME, 1).close()
    }

    companion object {
        private const val DB_NAME = "flux-migration-test"
    }
}
