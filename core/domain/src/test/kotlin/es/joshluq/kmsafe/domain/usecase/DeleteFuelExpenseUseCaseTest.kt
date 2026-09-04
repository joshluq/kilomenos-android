package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteFuelExpenseUseCaseTest {

    private val expenseRepository: FuelExpenseRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: DeleteFuelExpenseUseCase

    @Before
    fun setUp() {
        useCase = DeleteFuelExpenseUseCaseImpl(expenseRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given expenseId when invoke then deletes expense and emits Progress then Success`() = runTest {
        val expenseId = "exp-1"
        every { expenseRepository.deleteExpense(expenseId) } returns flowOf(Unit)

        val emissions = useCase(DeleteFuelExpenseUseCase.Input(expenseId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DeleteFuelExpenseUseCase.Output.Progress)
        assertTrue(emissions[1] is DeleteFuelExpenseUseCase.Output.Success)

        coVerify(exactly = 1) { expenseRepository.deleteExpense(expenseId) }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        val expenseId = "exp-1"
        every { expenseRepository.deleteExpense(expenseId) } returns flow { throw RuntimeException("Delete error") }

        val emissions = useCase(DeleteFuelExpenseUseCase.Input(expenseId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DeleteFuelExpenseUseCase.Output.Progress)
        val failure = emissions[1] as DeleteFuelExpenseUseCase.Output.Failure
        assertEquals(KmError.UnknownError, failure.error)
    }
}
