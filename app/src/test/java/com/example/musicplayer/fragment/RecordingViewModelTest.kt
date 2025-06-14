package com.example.musicplayer.fragment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import be.tarsos.dsp.AudioDispatcher
import com.example.musicplayer.manager.ToastManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class RecordingViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: RecordingViewModel
    private val testDispatcher = TestCoroutineDispatcher()

    @Mock
    private lateinit var mockDispatcher: AudioDispatcher

    @Mock
    private lateinit var mockToastManager: ToastManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = RecordingViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
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