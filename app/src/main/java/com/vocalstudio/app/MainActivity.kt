package com.vocalstudio.app

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import kotlin.math.max

private val Bg = Color(0xFF17191D)
private val Panel = Color(0xFF22252B)
private val Panel2 = Color(0xFF2B2F36)
private val Grid = Color(0xFF383D46)
private val TextMain = Color(0xFFE7E9ED)
private val TextDim = Color(0xFF9299A5)
private val Green = Color(0xFF9BCB68)
private val Purple = Color(0xFF9A78D7)
private val Orange = Color(0xFFE5A65B)

private enum class Workspace(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Playlist("PLAYLIST", Icons.Default.ViewList),
    Piano("PIANO ROLL", Icons.Default.MusicNote),
    Synth("SYNTH", Icons.Default.GraphicEq),
    Vocal("NEW TONE", Icons.Default.Tune),
    Audio("AUDIO EDITOR", Icons.Default.Waves),
    Mixer("MIXER", Icons.Default.Equalizer)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(23, 25, 29)
        window.navigationBarColor = android.graphics.Color.rgb(23, 25, 29)
        setContent { VocalStudioApp() }
    }
}

@Composable
private fun VocalStudioApp() {
    val context = LocalContext.current
    var workspace by remember { mutableStateOf(Workspace.Playlist) }
    var playing by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var bpm by remember { mutableIntStateOf(128) }
    var selectedTrack by remember { mutableIntStateOf(2) }
    var sampleUri by remember { mutableStateOf<Uri?>(null) }
    var sampleName by remember { mutableStateOf("No sample loaded") }
    var playhead by remember { mutableFloatStateOf(0f) }
    var noteCount by remember { mutableIntStateOf(4) }
    var variation by remember { mutableFloatStateOf(35f) }
    var trans by remember { mutableFloatStateOf(50f) }
    var centering by remember { mutableFloatStateOf(75f) }
    var normalize by remember { mutableStateOf(false) }
    var noiseReduction by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val samplePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sampleUri = uri
            sampleName = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported audio sample"
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    DisposableEffect(Unit) { onDispose { mediaPlayer?.release() } }
    LaunchedEffect(playing) {
        while (playing) {
            delay(40)
            playhead = (playhead + 0.0018f) % 1f
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Green)) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            TopTransport(
                playing = playing, recording = recording, bpm = bpm, onBpm = { bpm = it.coerceIn(40, 260) },
                onPlay = {
                    if (!playing) {
                        val uri = sampleUri
                        if (uri != null) {
                            runCatching {
                                mediaPlayer?.release()
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(context, uri)
                                    setOnCompletionListener { playing = false }
                                    prepare()
                                    start()
                                }
                            }
                        }
                    } else mediaPlayer?.pause()
                    playing = !playing
                },
                onStop = { playing = false; mediaPlayer?.let { runCatching { it.pause(); it.seekTo(0) } }; playhead = 0f },
                onRecord = { recording = !recording }
            )
            Row(Modifier.weight(1f).fillMaxWidth()) {
                ToolRail(onImport = { samplePicker.launch(arrayOf("audio/*")) }, onWorkspace = { workspace = it })
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    when (workspace) {
                        Workspace.Playlist -> PlaylistWorkspace(selectedTrack, { selectedTrack = it }, sampleName, playhead, onImport = { samplePicker.launch(arrayOf("audio/*")) }, onOpenPiano = { workspace = Workspace.Piano })
                        Workspace.Piano -> PianoWorkspace(noteCount, { noteCount = it }, sampleName)
                        Workspace.Synth -> SynthWorkspace(sampleName, onImport = { samplePicker.launch(arrayOf("audio/*")) })
                        Workspace.Vocal -> VocalWorkspace(sampleName, variation, { variation = it }, trans, { trans = it }, centering, { centering = it })
                        Workspace.Audio -> AudioWorkspace(sampleName, normalize, { normalize = it }, noiseReduction, { noiseReduction = it })
                        Workspace.Mixer -> MixerWorkspace()
                    }
                    BottomWorkspaceBar(workspace) { workspace = it }
                }
            }
        }
    }
}

@Composable
private fun TopTransport(playing: Boolean, recording: Boolean, bpm: Int, onBpm: (Int) -> Unit, onPlay: () -> Unit, onStop: () -> Unit, onRecord: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(54.dp).background(Color(0xFF111316)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("VOCAL STUDIO", color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.width(14.dp))
        SmallIconButton(Icons.Default.Menu, "Menu")
        SmallIconButton(Icons.Default.FolderOpen, "Open project")
        SmallIconButton(Icons.Default.Save, "Save project")
        VerticalDividerLine()
        SmallIconButton(Icons.Default.SkipPrevious, "Previous")
        SmallIconButton(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play", active = playing, onClick = onPlay)
        SmallIconButton(Icons.Default.Stop, "Stop", onClick = onStop)
        SmallIconButton(Icons.Default.FiberManualRecord, "Record", active = recording, tint = if (recording) Color(0xFFFF5B5B) else TextDim, onClick = onRecord)
        VerticalDividerLine()
        Text("PAT", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text("1", color = Green, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp))
        Text("TEMPO", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(4.dp)).background(Panel2)) {
            Text("−", color = TextDim, modifier = Modifier.clickable { onBpm(bpm - 1) }.padding(horizontal = 7.dp, vertical = 4.dp))
            Text("$bpm", color = Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("+", color = TextDim, modifier = Modifier.clickable { onBpm(bpm + 1) }.padding(horizontal = 7.dp, vertical = 4.dp))
        }
        Text("BPM", color = TextDim, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.weight(1f))
        Text("00:00:00", color = Color(0xFFB6D995), fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(12.dp))
        Text("4/4", color = TextDim, fontSize = 11.sp)
        Spacer(Modifier.width(10.dp))
        SmallIconButton(Icons.Default.Settings, "Settings")
    }
}

@Composable
private fun SmallIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean = false, tint: Color = if (active) Green else TextDim, onClick: () -> Unit = {}) {
    Box(Modifier.padding(horizontal = 2.dp).size(32.dp).clip(RoundedCornerShape(5.dp)).background(if (active) Color(0xFF303B2B) else Color.Transparent).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun VerticalDividerLine() { Box(Modifier.padding(horizontal = 7.dp).width(1.dp).height(25.dp).background(Grid)) }

@Composable
private fun ToolRail(onImport: () -> Unit, onWorkspace: (Workspace) -> Unit) {
    Column(Modifier.width(48.dp).fillMaxHeight().background(Color(0xFF1C1F24)).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        RailIcon(Icons.Default.Mouse, "Select", true)
        RailIcon(Icons.Default.Edit, "Draw")
        RailIcon(Icons.Default.Crop, "Slice")
        RailIcon(Icons.Default.ZoomIn, "Zoom")
        RailIcon(Icons.Default.MusicNote, "Notes")
        Spacer(Modifier.weight(1f))
        RailIcon(Icons.Default.FileUpload, "Import", onClick = onImport)
        RailIcon(Icons.Default.MusicNote, "Piano Roll", onClick = { onWorkspace(Workspace.Piano) })
        RailIcon(Icons.Default.GraphicEq, "Synth", onClick = { onWorkspace(Workspace.Synth) })
    }
}

@Composable
private fun RailIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit = {}) {
    Box(Modifier.padding(vertical = 3.dp).size(36.dp).clip(RoundedCornerShape(6.dp)).background(if (selected) Color(0xFF35402D) else Color.Transparent).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = if (selected) Green else TextDim, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun WorkspaceHeader(title: String, subtitle: String = "") {
    Row(Modifier.fillMaxWidth().height(34.dp).background(Color(0xFF202329)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
        if (subtitle.isNotBlank()) { Spacer(Modifier.width(10.dp)); Text(subtitle, color = TextDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Spacer(Modifier.weight(1f))
        Text("⋮", color = TextDim, fontSize = 20.sp)
    }
}

@Composable
private fun PlaylistWorkspace(selectedTrack: Int, onSelectTrack: (Int) -> Unit, sampleName: String, playhead: Float, onImport: () -> Unit, onOpenPiano: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        WorkspaceHeader("PLAYLIST", "Arrangement • Song mode")
        Row(Modifier.weight(1f)) {
            Column(Modifier.width(118.dp).fillMaxHeight().background(Color(0xFF202329))) {
                Row(Modifier.height(28.dp).fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("TRACK", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Icon(Icons.Default.Add, null, tint = TextDim, modifier = Modifier.size(15.dp)) }
                (1..7).forEach { n ->
                    Row(Modifier.fillMaxWidth().height(47.dp).background(if (selectedTrack == n) Color(0xFF343A43) else Color.Transparent).clickable { onSelectTrack(n) }.padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(5.dp).clip(RoundedCornerShape(2.dp)).background(if (n == 3) Purple else if (n == 1) Green else Orange))
                        Text(if (n == 3) "Vocal" else if (n == 1) "Drums" else "Track $n", color = if (selectedTrack == n) TextMain else TextDim, fontSize = 10.sp, modifier = Modifier.padding(start = 6.dp), maxLines = 1)
                        Spacer(Modifier.weight(1f)); Text("●", color = TextDim.copy(alpha = .65f), fontSize = 8.sp)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Grid.copy(alpha = .55f)))
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxWidth().height(28.dp).background(Color(0xFF252930))) {
                    val step = size.width / 16f
                    for (i in 0..16) {
                        val x = i * step
                        drawLine(Grid, Offset(x, size.height - 7f), Offset(x, size.height), 1f)
                        if (i < 16) drawContext.canvas.nativeCanvas.drawText("${i + 1}", x + 4f, 12f, android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; textSize = 9f })
                    }
                }
                Canvas(Modifier.fillMaxSize().background(Color(0xFF1B1E23)).pointerInput(Unit) { detectTapGestures { onSelectTrack((it.y / (size.height / 7f)).toInt().coerceIn(1, 7)) } }) {
                    val rowH = size.height / 7f
                    val beatW = size.width / 16f
                    for (i in 0..16) drawLine(if (i % 4 == 0) Color(0xFF515761) else Grid.copy(alpha = .55f), Offset(i * beatW, 0f), Offset(i * beatW, size.height), if (i % 4 == 0) 1.4f else .7f)
                    for (i in 0..7) drawLine(Grid.copy(alpha = .6f), Offset(0f, i * rowH), Offset(size.width, i * rowH), 1f)
                    drawRoundRect(Color(0xFF536B43), Offset(beatW * .12f, rowH * .12f), Size(beatW * 3.7f, rowH * .72f), androidx.compose.ui.geometry.CornerRadius(5f))
                    drawRoundRect(Color(0xFF66528A), Offset(beatW * 4f, rowH * 2.12f), Size(beatW * 7.7f, rowH * .72f), androidx.compose.ui.geometry.CornerRadius(5f))
                    drawRoundRect(Color(0xFF80613C), Offset(beatW * 1f, rowH * 4.12f), Size(beatW * 2.8f, rowH * .72f), androidx.compose.ui.geometry.CornerRadius(5f))
                    drawRoundRect(Color(0xFF536B43), Offset(beatW * 8f, rowH * .12f), Size(beatW * 3.7f, rowH * .72f), androidx.compose.ui.geometry.CornerRadius(5f))
                    drawLine(Green, Offset(size.width * playhead, 0f), Offset(size.width * playhead, size.height), 2f, cap = StrokeCap.Round)
                    drawCircle(Green, 4f, Offset(size.width * playhead, 4f))
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(35.dp).background(Color(0xFF202329)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("CLIP", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp)); Text(sampleName, color = TextMain, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TextButton(onClick = onImport, contentPadding = PaddingValues(horizontal = 8.dp)) { Icon(Icons.Default.FileOpen, null, tint = Green, modifier = Modifier.size(14.dp)); Text(" LOAD SAMPLE", color = Green, fontSize = 9.sp) }
            TextButton(onClick = onOpenPiano, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("OPEN PIANO ROLL  ↗", color = Purple, fontSize = 9.sp) }
        }
    }
}

@Composable
private fun PianoWorkspace(noteCount: Int, onNoteCount: (Int) -> Unit, sampleName: String) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        WorkspaceHeader("PIANO ROLL", "Vocal Sampler • $sampleName")
        Row(Modifier.weight(1f)) {
            Column(Modifier.width(48.dp).fillMaxHeight().background(Color(0xFF202329))) {
                (7 downTo 0).forEach { octave ->
                    Box(Modifier.weight(1f).fillMaxWidth().padding(1.dp).clip(RoundedCornerShape(2.dp)).background(if (octave % 2 == 0) Color(0xFFE4E5E7) else Color(0xFFB9BDC3)), contentAlignment = Alignment.Center) {
                        Text("${listOf("C","B","A","G","F","E","D","C")[7-octave]}${octave + 2}", color = Color(0xFF25282D), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            var localNotes by remember { mutableStateOf(listOf(0, 1, 2, 3)) }
            Canvas(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF1A1D22)).pointerInput(Unit) {
                detectTapGestures { tap ->
                    val col = (tap.x / (size.width / 16f)).toInt().coerceIn(0, 15)
                    localNotes = localNotes + col
                    onNoteCount(localNotes.size)
                }
            }) {
                val rowH = size.height / 24f
                val colW = size.width / 16f
                for (r in 0..24) {
                    val y = r * rowH
                    drawLine(if (r % 2 == 0) Color(0xFF454B55) else Grid.copy(alpha = .6f), Offset(0f, y), Offset(size.width, y), if (r % 2 == 0) 1f else .5f)
                }
                for (c in 0..16) drawLine(if (c % 4 == 0) Color(0xFF555C67) else Grid.copy(alpha = .55f), Offset(c * colW, 0f), Offset(c * colW, size.height), if (c % 4 == 0) 1.3f else .6f)
                localNotes.forEachIndexed { index, c ->
                    val row = 4 + (index * 3 % 14)
                    drawRoundRect(if (index % 2 == 0) Color(0xFF91B965) else Color(0xFF8A70C0), Offset(c * colW + 2f, row * rowH + 2f), Size(colW * 1.8f, rowH - 4f), androidx.compose.ui.geometry.CornerRadius(4f))
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(30.dp).background(Color(0xFF202329)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("NOTES: $noteCount", color = TextDim, fontSize = 9.sp); Spacer(Modifier.weight(1f)); Text("Tap grid to add notes • Multi-select & drag editing coming next", color = TextDim, fontSize = 9.sp)
        }
    }
}

@Composable
private fun SynthWorkspace(sampleName: String, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        WorkspaceHeader("VOCAL SAMPLER", "Harmor-inspired instrument • Sample resynthesis")
        Row(Modifier.weight(1f).padding(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(Modifier.weight(1.25f).fillMaxHeight()) {
                PanelCard("SAMPLE / SOURCE", Modifier.weight(1f)) {
                    Text(sampleName, color = TextMain, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    WaveformPlaceholder(Modifier.weight(1f).fillMaxWidth(), Purple)
                    Spacer(Modifier.height(5.dp)); ActionButton("IMPORT VOCAL SAMPLE", onImport, Green)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelCard("AMPLITUDE ENVELOPE", Modifier.weight(1f)) { EnvelopeGraphic(); SliderRow("ATTACK", "12 ms", .16f); SliderRow("DECAY", "420 ms", .45f); SliderRow("SUSTAIN", "82%", .82f); SliderRow("RELEASE", "310 ms", .35f) }
                PanelCard("PLAYBACK / RESYNTHESIS", Modifier.weight(1f)) { SliderRow("PITCH", "0 st", .5f); SliderRow("FORMANT", "0", .5f); SliderRow("TIME", "100%", .5f); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { MiniChip("RESAMPLE", true); MiniChip("LOOP"); MiniChip("MONO") } }
            }
            Column(Modifier.weight(.8f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelCard("FILTER", Modifier.weight(1f)) { SliderRow("CUTOFF", "18.2 kHz", .86f); SliderRow("RESONANCE", "12%", .12f); SliderRow("DRIVE", "0%", .02f) }
                PanelCard("MODULATION", Modifier.weight(1f)) { SliderRow("LFO RATE", "4.0 Hz", .35f); SliderRow("PITCH DEPTH", "0 st", .05f); SliderRow("MIX", "25%", .25f) }
            }
        }
    }
}

@Composable
private fun VocalWorkspace(sampleName: String, variation: Float, onVariation: (Float) -> Unit, trans: Float, onTrans: (Float) -> Unit, centering: Float, onCentering: (Float) -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        WorkspaceHeader("VOCAL PITCH EDITOR", "NewTone-inspired • $sampleName")
        Row(Modifier.weight(1f).padding(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(Modifier.weight(1f)) {
                PanelCard("PITCH GRID / NOTE SELECTION", Modifier.fillMaxSize()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MiniChip("SELECT", true); MiniChip("MOVE"); MiniChip("SPLIT"); MiniChip("SNAP") }
                    Spacer(Modifier.height(8.dp))
                    PitchBlobEditor(Modifier.weight(1f).fillMaxWidth())
                    Text("Pitch detection editor preview • Import and analyze audio to generate editable note blobs", color = TextDim, fontSize = 9.sp)
                }
            }
            Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelCard("NOTE TRANSFORMATION", Modifier.weight(1f)) {
                    SliderRow("VARIATION", "${variation.toInt()}%", variation / 100f)
                    Slider(value = variation, onValueChange = onVariation, valueRange = 0f..100f)
                    SliderRow("TRANS", "${trans.toInt()}%", trans / 100f)
                    Slider(value = trans, onValueChange = onTrans, valueRange = 0f..100f)
                    SliderRow("CENTERING", "${centering.toInt()}%", centering / 100f)
                    Slider(value = centering, onValueChange = onCentering, valueRange = 0f..100f)
                }
                PanelCard("TOOLS", Modifier.height(112.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { MiniChip("CORRECT", true); MiniChip("QUANTIZE"); MiniChip("RESET") }; Spacer(Modifier.height(8.dp)); Text("Selected notes: 0", color = TextDim, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun AudioWorkspace(sampleName: String, normalize: Boolean, onNormalize: (Boolean) -> Unit, denoise: Boolean, onDenoise: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        WorkspaceHeader("AUDIO EDITOR", "Edison-inspired • $sampleName")
        Row(Modifier.weight(1f).padding(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(Modifier.weight(1f)) {
                PanelCard("WAVEFORM EDITOR", Modifier.fillMaxSize()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { MiniChip("SELECT", true); MiniChip("CUT"); MiniChip("COPY"); MiniChip("PASTE"); MiniChip("TRIM"); MiniChip("REVERSE") }
                    Spacer(Modifier.height(10.dp)); WaveformPlaceholder(Modifier.weight(1f).fillMaxWidth(), Green)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("00:00.000", color = TextDim, fontSize = 9.sp); Text("SELECTION: 0.000 – 0.000", color = TextDim, fontSize = 9.sp); Text("02:14.320", color = TextDim, fontSize = 9.sp) }
                }
            }
            Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelCard("PROCESSING", Modifier.weight(1f)) {
                    SwitchRow("Normalize", normalize, onNormalize)
                    SwitchRow("Noise reduction", denoise, onDenoise)
                    ActionButton("APPLY PROCESSING", {}, Green)
                    Spacer(Modifier.height(5.dp)); MiniChip("EQ"); MiniChip("COMPRESSOR"); MiniChip("REVERB"); MiniChip("FADE IN / OUT")
                }
                PanelCard("EXPORT", Modifier.height(100.dp)) { Text("WAV • 44.1 kHz • 16-bit", color = TextDim, fontSize = 9.sp); Spacer(Modifier.height(6.dp)); ActionButton("EXPORT AUDIO", {}, Purple) }
            }
        }
    }
}

@Composable
private fun MixerWorkspace() {
    Column(Modifier.fillMaxSize().background(Bg)) {
        WorkspaceHeader("MIXER", "Insert channels • Master")
        Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            (1..8).forEach { n ->
                Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Panel).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (n == 1) "MASTER" else "INSERT $n", color = if (n == 1) Green else TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp)); Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF171A1F), RoundedCornerShape(3.dp)), contentAlignment = Alignment.BottomCenter) { Box(Modifier.fillMaxWidth(.55f).fillMaxHeight(if (n % 3 == 0) .25f else .62f).background(if (n == 1) Green else Purple, RoundedCornerShape(2.dp))) }
                    Spacer(Modifier.height(8.dp)); Text("0.0 dB", color = TextMain, fontSize = 9.sp); Spacer(Modifier.height(8.dp)); Text("FX +", color = TextDim, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun BottomWorkspaceBar(current: Workspace, onSelect: (Workspace) -> Unit) {
    Row(Modifier.fillMaxWidth().height(43.dp).background(Color(0xFF111316)).horizontalScroll(rememberScrollState()).padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Workspace.values().forEach { item ->
            val selected = item == current
            Row(Modifier.padding(horizontal = 3.dp, vertical = 4.dp).clip(RoundedCornerShape(5.dp)).background(if (selected) Color(0xFF343C30) else Color.Transparent).clickable { onSelect(item) }.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(item.icon, null, tint = if (selected) Green else TextDim, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp)); Text(item.label, color = if (selected) Green else TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.weight(1f)); Text("MASTER", color = TextDim, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 10.dp))
    }
}

@Composable
private fun PanelCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(6.dp)).background(Panel).border(1.dp, Color(0xFF343941), RoundedCornerShape(6.dp)).padding(9.dp)) {
        Text(title, color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
        Spacer(Modifier.height(8.dp)); content()
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, color: Color) {
    Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = .16f)).border(1.dp, color.copy(alpha = .55f), RoundedCornerShape(4.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .4.sp)
    }
}

@Composable
private fun MiniChip(label: String, selected: Boolean = false) {
    Box(Modifier.padding(end = 3.dp).clip(RoundedCornerShape(3.dp)).background(if (selected) Color(0xFF3B4932) else Color(0xFF343941)).padding(horizontal = 7.dp, vertical = 5.dp)) {
        Text(label, color = if (selected) Green else TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SliderRow(label: String, value: String, fraction: Float) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 8.sp, modifier = Modifier.weight(1f))
        Box(Modifier.width(72.dp).height(3.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF454B54))) { Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(Purple)) }
        Text(value, color = TextMain, fontSize = 8.sp, modifier = Modifier.width(53.dp).padding(start = 5.dp))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMain, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.height(25.dp), colors = SwitchDefaults.colors(checkedThumbColor = Green, checkedTrackColor = Color(0xFF46583A)))
    }
}

@Composable
private fun EnvelopeGraphic() {
    Canvas(Modifier.fillMaxWidth().height(54.dp).background(Color(0xFF191C21), RoundedCornerShape(4.dp)).padding(5.dp)) {
        val pts = listOf(Offset(0f, size.height), Offset(size.width * .15f, 0f), Offset(size.width * .38f, size.height * .25f), Offset(size.width * .75f, size.height * .25f), Offset(size.width, size.height))
        for (i in 0 until pts.lastIndex) drawLine(Green, pts[i], pts[i + 1], 2f, cap = StrokeCap.Round)
    }
}

@Composable
private fun WaveformPlaceholder(modifier: Modifier, color: Color) {
    Canvas(modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF171A1F))) {
        val mid = size.height / 2f
        val bars = 150
        for (i in 0 until bars) {
            val x = size.width * i / bars
            val wave = (kotlin.math.abs(kotlin.math.sin(i * .31)) * .7f + kotlin.math.abs(kotlin.math.sin(i * .093)) * .3f)
            val h = max(3f, size.height * .42f * wave)
            drawLine(color.copy(alpha = .85f), Offset(x, mid - h), Offset(x, mid + h), 1.5f, cap = StrokeCap.Round)
        }
        drawLine(Color.White.copy(alpha = .13f), Offset(0f, mid), Offset(size.width, mid), .7f)
    }
}

@Composable
private fun PitchBlobEditor(modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF171A1F))) {
        val row = size.height / 12f
        val col = size.width / 10f
        for (i in 0..12) drawLine(Grid.copy(alpha = .65f), Offset(0f, i * row), Offset(size.width, i * row), 1f)
        for (i in 0..10) drawLine(Grid.copy(alpha = .55f), Offset(i * col, 0f), Offset(i * col, size.height), 1f)
        val notes = listOf(1 to 2, 2 to 3, 3 to 3, 4 to 5, 5 to 4, 6 to 6, 7 to 5, 8 to 7)
        notes.forEachIndexed { idx, pair ->
            val x = pair.first * col + 3f
            val y = pair.second * row
            drawRoundRect(if (idx == 3) Orange else Purple, Offset(x, y), Size(col * .85f, row * .55f), androidx.compose.ui.geometry.CornerRadius(7f))
            drawLine(Color.White.copy(alpha = .5f), Offset(x + 5f, y + row * .27f), Offset(x + col * .8f, y + row * .27f), 1f)
        }
    }
}
