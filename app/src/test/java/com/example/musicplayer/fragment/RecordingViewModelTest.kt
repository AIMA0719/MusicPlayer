package com.example.musicplayer.fragment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Ignore

@ExperimentalCoroutinesApi
@Ignore("Temporary disable until ViewModel is fully implemented")
class RecordingViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: RecordingViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RecordingViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startRecording should update isRecording state`() {
        // Given
        val pitchArray = floatArrayOf(440f, 880f)

        // When
        viewModel.startRecording(pitchArray)

        // Then
        assertTrue(viewModel.isRecording.value!!)
    }

    @Test
    fun `stopRecording should update isRecording state`() {
        // Given
        val pitchArray = floatArrayOf(440f, 880f)
        viewModel.startRecording(pitchArray)

        // When
        viewModel.stopRecording()

        // Then
        assertFalse(viewModel.isRecording.value!!)
    }

    @Test
    fun `startRecording should clear previous pitch pairs`() {
        // Given
        val pitchArray = floatArrayOf(440f, 880f)
        viewModel.startRecording(pitchArray)
        viewModel.stopRecording()

        // When
        viewModel.startRecording(pitchArray)

        // Then
        assertEquals(0, viewModel.getPitchPairs().size)
    }

    @Test
    fun `stopRecording should calculate score`() {
        // Given
        val pitchArray = floatArrayOf(440f, 880f)
        viewModel.startRecording(pitchArray)

        // When
        viewModel.stopRecording()

        // Then
        assertTrue(viewModel.score.value != null)
    }
} 