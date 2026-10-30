# VOCAL STUDIO — Android DAW Prototype (v0.2.0)

This revision replaces the generic five-screen navigation scaffold with a landscape-first, FL Studio Mobile-inspired workspace.

## What is included

- Landscape-locked Android activity.
- DAW-style transport strip with play/pause, stop, record toggle, tempo control and time display.
- Left-side tool rail and bottom workspace tabs.
- Playlist/arrangement view with track lanes, clip blocks, bar grid and playhead.
- Piano Roll grid; tapping the grid adds note blocks to the current visual pattern.
- Vocal Sampler instrument panel with source sample import UI, waveform display and synthesis control panels.
- NewTone-inspired pitch editor panel with Variation, Trans and Centering controls.
- Edison-inspired audio editor panel with waveform view and Normalize / Noise Reduction toggles.
- Mixer channel-strip workspace.
- Audio file picker; an imported audio file can be played through Android MediaPlayer from the transport play button.

## Important implementation status

This is a UI/workflow foundation, not a finished professional DAW yet. The displayed synth envelopes, resynthesis controls, pitch blobs, processing toggles and mixer meters are currently interface prototypes. They do not yet perform Harmor-quality resynthesis, automatic pitch analysis, NewTone-style note correction, denoising, EQ, compression, or export processing. The sample playback transport currently plays an imported file as a whole; it is not yet synchronized to the arrangement or Piano Roll. The Record control is currently a UI state toggle, not an audio recorder.

The next engineering milestone is to implement the shared audio clip/sample model, MIDI note data model, audio transport clock, sample voice playback, then DSP processing and pitch detection.

## Build

Open this folder as an Android project in Android Studio (Giraffe or newer; a current stable release is recommended), allow Gradle sync, then build the `app` debug variant. Requirements: JDK 17, Android SDK Platform 35, Android Gradle Plugin 8.7.3, Kotlin 2.0.21.

The project does not include a Gradle wrapper. Android Studio can generate/use its configured Gradle installation. No APK has been built or device-tested in this environment.
