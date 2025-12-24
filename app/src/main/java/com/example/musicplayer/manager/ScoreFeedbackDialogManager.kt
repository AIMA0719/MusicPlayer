package com.example.musicplayer.manager

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import com.example.musicplayer.R
import com.example.musicplayer.scoreAlgorithm.ScoreAnalyzer

/**
 * 점수 피드백 다이얼로그 매니저
 * - 채점 난이도 선택 다이얼로그
 * - 상세 피드백 다이얼로그
 */
object ScoreFeedbackDialogManager {

    /**
     * 채점 난이도
     */
    enum class ScoringDifficulty(val displayName: String, val multiplier: Double) {
        VERY_EASY("피스 오브 케익 모드", 1.3),
        EASY("이지 모드", 1.15),
        NORMAL("노말 모드", 1.0),
        HARD("고수 모드", 0.85),
        VERY_HARD("초고수 모드", 0.7)
    }

    /**
     * 난이도에 따른 점수 조정
     */
    fun calculateAdjustedScore(baseScore: Int, difficulty: ScoringDifficulty): Int {
        var adjustedScore = (baseScore * difficulty.multiplier).toInt()
        adjustedScore = adjustedScore.coerceIn(0, 100)

        // 95점 이상이면 50% 확률로 100점 처리
        if (adjustedScore in 95..<100) {
            val random = kotlin.random.Random.nextBoolean()
            if (random) {
                adjustedScore = 100
            }
        }

        return adjustedScore
    }

    /**
     * 상세 피드백 다이얼로그 표시
     *
     * @param context Context
     * @param analyzer ScoreAnalyzer 인스턴스
     * @param finalScore 최종 선택된 점수
     * @param difficulty 선택한 채점 난이도
     * @param songTitle 노래 제목 (공유 시 사용)
     * @param gameReward 게임 보상 정보 (선택)
     * @param onDismiss 다이얼로그 닫힘 콜백
     */
    @SuppressLint("InflateParams", "SetTextI18n")
    fun showScoreFeedbackDialog(
        context: Context,
        analyzer: ScoreAnalyzer,
        finalScore: Int,
        difficulty: ScoringDifficulty,
        songTitle: String = "",
        gameReward: GameReward? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(
            LayoutInflater.from(context).inflate(R.layout.dialog_score_feedback, null)
        )
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 상세 점수 가져오기
        val detailedScores = analyzer.getDetailedScores()
        val vibratoInfo = analyzer.detectVibrato()

        // 점수 등급
        val tvScoreGrade = dialog.findViewById<TextView>(R.id.tv_score_grade)
        val grade = getScoreGrade(finalScore)
        tvScoreGrade.text = grade.grade
        tvScoreGrade.setTextColor(android.graphics.Color.parseColor(grade.color))

        // 최종 점수
        val tvFinalScore = dialog.findViewById<TextView>(R.id.tv_final_score)
        tvFinalScore.text = "${finalScore}점"

        // 점수 메시지
        val tvScoreMessage = dialog.findViewById<TextView>(R.id.tv_score_message)
        tvScoreMessage.text = getScoreMessage(finalScore)

        // 게임 리워드 정보 표시
        if (gameReward != null && gameReward.exp > 0) {
            dialog.findViewById<LinearLayout>(R.id.game_reward_section).visibility = View.VISIBLE

            // 경험치
            dialog.findViewById<TextView>(R.id.tv_exp_gained).text = "⭐ +${gameReward.exp} 경험치"

            // 레벨업
            if (gameReward.leveledUp) {
                dialog.findViewById<TextView>(R.id.tv_level_up).apply {
                    visibility = View.VISIBLE
                    text = "🎉 레벨 업! Lv.${gameReward.newLevel - 1} → Lv.${gameReward.newLevel}"
                }
            }

            // 도전과제
            if (gameReward.unlockedAchievements.isNotEmpty()) {
                dialog.findViewById<TextView>(R.id.tv_achievements).apply {
                    visibility = View.VISIBLE
                    text = "🏆 새로운 업적 ${gameReward.unlockedAchievements.size}개 달성!"
                }
            }
        }

        // 상세 점수
        dialog.findViewById<TextView>(R.id.tv_pitch_score).text =
            String.format("%.1f%%", detailedScores["pitch_accuracy"]!! * 100)
        dialog.findViewById<TextView>(R.id.tv_rhythm_score).text =
            String.format("%.1f%%", detailedScores["rhythm_score"]!! * 100)
        dialog.findViewById<TextView>(R.id.tv_volume_score).text =
            String.format("%.1f%%", detailedScores["volume_stability"]!! * 100)
        dialog.findViewById<TextView>(R.id.tv_duration_score).text =
            String.format("%.1f%%", detailedScores["duration_match"]!! * 100)

        // 채점 난이도 정보 표시
        if (difficulty != ScoringDifficulty.NORMAL) {
            dialog.findViewById<LinearLayout>(R.id.difficulty_section).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.tv_difficulty).text = difficulty.displayName
        }

        // 비브라토 정보 (감지된 경우만 표시)
        if (vibratoInfo.hasVibrato) {
            dialog.findViewById<LinearLayout>(R.id.vibrato_section).visibility = View.VISIBLE
            val vibratoText = when {
                vibratoInfo.score >= 0.9 -> "감지됨 (우수)"
                vibratoInfo.score >= 0.7 -> "감지됨 (양호)"
                else -> "감지됨"
            }
            dialog.findViewById<TextView>(R.id.tv_vibrato).text = vibratoText
        }

        // 피드백 (70점 미만인 경우만 표시)
        if (finalScore < 70) {
            val feedback = generateFeedback(detailedScores)
            if (feedback.isNotEmpty()) {
                dialog.findViewById<View>(R.id.feedback_divider).visibility = View.VISIBLE
                dialog.findViewById<TextView>(R.id.feedback_title).visibility = View.VISIBLE
                dialog.findViewById<TextView>(R.id.tv_feedback).apply {
                    visibility = View.VISIBLE
                    text = feedback
                }
            }
        }

        // 공유 버튼
        dialog.findViewById<TextView>(R.id.btn_share).setOnClickListener {
            shareScore(context, songTitle, finalScore, detailedScores, grade)
        }

        // 확인 버튼
        dialog.findViewById<TextView>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        dialog.show()

        // 다이얼로그 너비 설정: 화면 너비 - 좌우 각 10dp
        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val marginDp = 10f
            val marginPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                marginDp,
                displayMetrics
            ).toInt()

            val params = window.attributes
            params.width = displayMetrics.widthPixels - (marginPx * 2)
            window.attributes = params
        }
    }

    /**
     * 점수 등급 정보
     */
    private data class ScoreGrade(val grade: String, val color: String)

    /**
     * 점수에 따른 등급 반환
     */
    private fun getScoreGrade(score: Int): ScoreGrade {
        return when {
            score >= 98 -> ScoreGrade("S", "#FFD700") // 금색
            score >= 95 -> ScoreGrade("A+", "#FF6B35") // 주황색
            score >= 90 -> ScoreGrade("A", "#FF8559") // 연한 주황색
            score >= 85 -> ScoreGrade("B+", "#4ECDC4") // 청록색
            score >= 80 -> ScoreGrade("B", "#95E1D3") // 연한 청록색
            score >= 70 -> ScoreGrade("C+", "#9C88FF") // 보라색
            score >= 60 -> ScoreGrade("C", "#C3AEF0") // 연한 보라색
            else -> ScoreGrade("D", "#A8A8A8") // 회색
        }
    }

    /**
     * 점수에 따른 메시지 반환
     */
    private fun getScoreMessage(score: Int): String {
        return when {
            score >= 98 -> "완벽합니다! 프로 수준이에요! 🎉"
            score >= 95 -> "놀라워요! 거의 완벽해요! ⭐⭐⭐"
            score >= 90 -> "훌륭합니다! 정말 잘하셨어요! ⭐⭐"
            score >= 85 -> "아주 좋아요! 실력이 느껴져요! ⭐"
            score >= 80 -> "잘 부르셨어요! 👍"
            score >= 70 -> "좋아요! 계속 연습하면 더 좋아질 거예요! 💪"
            score >= 60 -> "괜찮아요! 조금만 더 연습해봐요 😊"
            else -> "힘내세요! 연습이 실력을 만들어요 📚"
        }
    }

    /**
     * 상세 점수 기반 피드백 생성
     */
    private fun generateFeedback(detailedScores: Map<String, Double>): String {
        val feedback = StringBuilder()
        val pitchScore = detailedScores["pitch_accuracy"]!! * 100
        val rhythmScore = detailedScores["rhythm_score"]!! * 100
        val volumeScore = detailedScores["volume_stability"]!! * 100

        // 가장 낮은 점수 찾기
        val lowestMetric = listOf(
            "음정" to pitchScore,
            "리듬" to rhythmScore,
            "볼륨 안정성" to volumeScore
        ).minByOrNull { it.second }

        lowestMetric?.let { (name, score) ->
            if (score < 70) {
                when (name) {
                    "음정" -> feedback.append("• 음정을 더 정확하게 맞춰보세요. 원곡을 자주 들으며 연습하면 도움이 됩니다.\n")
                    "리듬" -> feedback.append("• 박자를 더 정확하게 맞춰보세요. 메트로놈을 활용해 연습해보세요.\n")
                    "볼륨 안정성" -> feedback.append("• 일정한 볼륨으로 부르도록 노력해보세요. 호흡 조절이 중요합니다.\n")
                }
            }
        }

        // 전반적으로 낮은 경우
        if (pitchScore < 70 && rhythmScore < 70) {
            feedback.append("• 원곡을 충분히 듣고 따라 부르는 연습을 해보세요.\n")
        }

        return feedback.toString().trim()
    }

    /**
     * 점수 결과 공유
     */
    private fun shareScore(
        context: Context,
        songTitle: String,
        finalScore: Int,
        detailedScores: Map<String, Double>,
        grade: ScoreGrade
    ) {
        val pitchScore = String.format("%.1f", detailedScores["pitch_accuracy"]!! * 100)
        val rhythmScore = String.format("%.1f", detailedScores["rhythm_score"]!! * 100)
        val volumeScore = String.format("%.1f", detailedScores["volume_stability"]!! * 100)

        val shareText = buildString {
            append("🎤 노래방 점수 결과\n\n")
            if (songTitle.isNotEmpty()) {
                append("🎵 노래: $songTitle\n\n")
            }
            append("📊 등급: ${grade.grade}\n")
            append("🏆 총점: ${finalScore}점\n\n")
            append("📈 상세 점수\n")
            append("• 음정 정확도: $pitchScore%\n")
            append("• 리듬 정확도: $rhythmScore%\n")
            append("• 볼륨 안정성: $volumeScore%\n\n")
            append("${getScoreMessage(finalScore)}\n\n")
            append("#노래방 #MusicPlayer")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        try {
            context.startActivity(Intent.createChooser(shareIntent, "결과 공유"))
        } catch (e: Exception) {
            ToastManager.showToast("공유할 수 없습니다")
        }
    }
}
