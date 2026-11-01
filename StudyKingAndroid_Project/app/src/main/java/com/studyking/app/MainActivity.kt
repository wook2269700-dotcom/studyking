package com.studyking.app

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { StudyKingApp() } }
    }
}

data class Q(val subject: String, val text: String, val choices: List<String>, val answer: Int)

private fun makeQuestions(child: String, day: String): List<Q> {
    val grade1 = child == "강준영"
    val seed = (day + child).hashCode()
    val r = Random(seed)
    val math = mutableListOf<Q>()
    repeat(10) { i ->
        if (grade1) {
            val a = r.nextInt(1, 10); val b = r.nextInt(1, 10)
            val add = i % 2 == 0
            val ans = if (add) a + b else kotlin.math.abs(a - b)
            val text = if (add) "$a + $b 는 얼마일까요?" else "큰 수에서 작은 수를 빼면 얼마일까요? ($a, $b)"
            math += Q("수학", text, options(ans, r), ans)
        } else {
            val a = r.nextInt(12, 90); val b = r.nextInt(10, 50)
            val add = i % 2 == 0
            val ans = if (add) a + b else a - b
            val text = if (add) "$a + $b 는 얼마일까요?" else "$a - $b 는 얼마일까요?"
            math += Q("수학", text, options(ans, r), ans)
        }
    }
    val vocab1 = listOf(
        Triple("‘크다’의 반대말은 무엇일까요?", listOf("작다","높다","길다","넓다"), 0),
        Triple("‘학교’에서 공부하는 사람은 누구일까요?", listOf("학생","의사","요리사","경찰"), 0),
        Triple("다음 중 동물 이름은 무엇일까요?", listOf("토끼","연필","책상","구름"), 0),
        Triple("‘아침’ 다음에 오는 때는?", listOf("점심","밤","새벽","어제"), 0),
        Triple("문장의 끝에 주로 쓰는 문장 부호는?", listOf("마침표(.)","쉼표(,)","괄호()","따옴표(“ ”)"), 0),
        Triple("‘기쁘다’와 뜻이 가장 가까운 말은?", listOf("즐겁다","슬프다","무섭다","졸리다"), 0),
        Triple("‘바다’와 어울리는 낱말은?", listOf("파도","자동차","교실","지우개"), 0),
        Triple("‘나는 밥을 먹었다.’에서 행동을 나타내는 말은?", listOf("먹었다","나는","밥을","에서"), 0),
        Triple("책을 읽을 때 필요한 것은?", listOf("눈","귀마개","장갑","우산"), 0),
        Triple("‘빠르다’의 반대말은?", listOf("느리다","높다","밝다","맑다"), 0)
    )
    val vocab4 = listOf(
        Triple("‘성실하다’와 뜻이 가장 가까운 말은?", listOf("부지런하다","게으르다","급하다","조용하다"), 0),
        Triple("‘희망’의 뜻으로 알맞은 것은?", listOf("앞날에 대한 기대","지난 일에 대한 후회","깊은 잠","큰 소리"), 0),
        Triple("다음 중 의성어는 무엇일까요?", listOf("멍멍","아름다운","천천히","파란"), 0),
        Triple("‘가는 말이 고와야 오는 말이 곱다’의 뜻은?", listOf("말을 곱게 해야 한다","빨리 걸어야 한다","선물을 주어야 한다","조용히 해야 한다"), 0),
        Triple("문단의 중심 내용을 가장 잘 나타내는 문장은?", listOf("중심 문장","꾸밈 문장","인용 문장","대화 문장"), 0),
        Triple("‘넓다’의 반대말은?", listOf("좁다","깊다","길다","높다"), 0),
        Triple("다음 중 높임 표현은?", listOf("드시다","먹다","자다","보다"), 0),
        Triple("‘발이 넓다’라는 관용 표현의 뜻은?", listOf("아는 사람이 많다","발 크기가 크다","잘 달린다","여행을 좋아한다"), 0),
        Triple("글쓴이의 생각이나 주장을 뒷받침하는 것은?", listOf("근거","제목","쪽수","이름"), 0),
        Triple("‘그러나’와 뜻이 비슷한 이어 주는 말은?", listOf("하지만","그리고","그래서","또한"), 0)
    )
    val words = if (grade1) vocab1 else vocab4
    val korean = words.shuffled(r).map { Q("국어", it.first, it.second, it.third) }
    return (math + korean).shuffled(r)
}

private fun options(answer: Int, r: Random): List<String> {
    val values = linkedSetOf(answer)
    while (values.size < 4) values += (answer + r.nextInt(-5, 6)).coerceAtLeast(0)
    return values.shuffled(r).map { it.toString() }
}

@Composable
fun StudyKingApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("studyking", 0) }
    var child by remember { mutableStateOf("") }
    var screen by remember { mutableStateOf("home") }
    var questions by remember { mutableStateOf(emptyList<Q>()) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(3) }
    var seconds by remember { mutableIntStateOf(600) }
    var selected by remember { mutableIntStateOf(-1) }
    var message by remember { mutableStateOf("") }
    var eventWon by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(true) }
    var signatureCount by remember { mutableIntStateOf(0) }
    val today = LocalDate.now().toString()
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 75) }

    fun play(correct: Boolean) {
        if (soundOn) tone.startTone(if (correct) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, 180)
    }
    fun startGame(name: String) {
        child = name
        val eventKey = "event_${name}_$today"
        eventWon = if (prefs.contains(eventKey)) prefs.getBoolean(eventKey, false)
                   else (Random.nextInt(100) < 20).also { prefs.edit().putBoolean(eventKey, it).apply() }
        questions = makeQuestions(name, today)
        index = 0; score = 0; hearts = 3; seconds = 600; selected = -1
        screen = "game"
    }

    LaunchedEffect(screen) {
        if (screen == "game") {
            while (seconds > 0 && screen == "game" && index < 20) {
                delay(1000); seconds--
            }
            if (seconds == 0 && screen == "game") {
                message = "시간이 끝났어요! 다시 도전해 보세요."
                screen = "result"
            }
        }
    }

    val bg = Color(0xFFF3F7FF)
    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("🏆 강준영 · 강채영 학습왕", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF244B86))
            when (screen) {
                "home" -> {
                    Text("오늘도 즐겁게 공부해 볼까요?", fontSize = 17.sp)
                    Button(onClick = { startGame("강준영") }, modifier = Modifier.fillMaxWidth()) {
                        Text("👦 강준영  ·  초등학교 1학년")
                    }
                    Button(onClick = { startGame("강채영") }, modifier = Modifier.fillMaxWidth()) {
                        Text("👧 강채영  ·  초등학교 4학년")
                    }
                    OutlinedButton(onClick = { soundOn = !soundOn }) {
                        Text(if (soundOn) "🔊 소리 켜짐 (눌러서 끄기)" else "🔇 소리 꺼짐 (눌러서 켜기)")
                    }
                    Text("수학 10문제 + 국어 10문제 · 총 20문제")
                    Text("10분 제한 · 하트 3개 · 만점 100점")
                    Text("아빠 서명 5개를 모으면 선물 보상 🎁")
                    Text("깜짝 이벤트: 매일 아이별 20% 확률")
                }
                "game" -> {
                    val q = questions[index]
                    Text("$child  |  ${q.subject}", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("남은 시간  ${seconds / 60}:${(seconds % 60).toString().padStart(2,'0')}     ❤️ $hearts",
                        fontSize = 20.sp, color = Color(0xFFB33D53))
                    LinearProgressIndicator(progress = { (index + 1) / 20f }, modifier = Modifier.fillMaxWidth())
                    Text("문제 ${index + 1} / 20", color = Color.Gray)
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Text(q.text, Modifier.padding(22.dp), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    }
                    q.choices.forEachIndexed { i, choice ->
                        Button(onClick = {
                            if (selected == -1) {
                                selected = i
                                val correct = choice == if (q.subject == "수학") q.answer.toString() else q.choices[q.answer]
                                if (correct) { score += 5; play(true); message = "딩동댕! 정답이에요 ✨" }
                                else { hearts--; play(false); message = "아깝다! 다음 문제도 도전해요 💪" }
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = selected == -1) {
                            Text("${i + 1}. $choice", fontSize = 18.sp)
                        }
                    }
                    if (selected != -1) {
                        Text(message, fontWeight = FontWeight.Bold)
                        Button(onClick = {
                            if (index == 19) {
                                screen = "result"
                                message = if (score == 100) "100점! 축하합니다! 아빠한테 싸인 받아오세요 ✍️"
                                          else "수고했어요! 점수: $score 점"
                            } else { index++; selected = -1 }
                        }) { Text(if (index == 19) "결과 보기" else "다음 문제 →") }
                    }
                    if (eventWon && index == 0) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE8B8))) {
                            Text("🎉 당첨되었습니다!\n강동욱 아빠의 선물!\n아빠에게 선물을 요청하세요 🎁",
                                Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "result" -> {
                    Text(message, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF245E43))
                    Text("최종 점수: $score / 100", fontSize = 24.sp)
                    if (score == 100) {
                        var signed by remember { mutableStateOf(false) }
                        Text("아빠 서명 확인")
                        SignaturePad(Modifier.fillMaxWidth().height(150.dp))
                        Button(onClick = {
                            if (!signed) {
                                signatureCount = prefs.getInt("sign_$child", 0) + 1
                                prefs.edit().putInt("sign_$child", signatureCount).apply()
                                signed = true
                            }
                        }) { Text(if (signed) "서명 저장 완료" else "아빠 서명 확인 완료") }
                        Text("서명 ${prefs.getInt("sign_$child", 0)} / 5")
                        if (prefs.getInt("sign_$child", 0) >= 5) Text("🎁 아빠한테 선물을 받으세요!", fontSize = 22.sp)
                    }
                    Button(onClick = { screen = "home" }) { Text("처음 화면으로") }
                }
            }
        }
    }
}

@Composable
fun SignaturePad(modifier: Modifier = Modifier) {
    val points = remember { mutableStateListOf<Offset>() }
    Canvas(modifier.background(Color.White).pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { points.add(it) },
            onDrag = { change, _ -> points.add(change.position); change.consume() }
        )
    }) {
        val path = Path()
        points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        drawPath(path, Color.Black, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
    }
}
