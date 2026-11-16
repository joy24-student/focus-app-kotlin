# FocusMood Android App (Kotlin)

A native Android Pomodoro & Focus Mood application built with 100% Kotlin, matching all features of the original Focus App with a sleek 2-tab navigation (Home & History).

## 📱 Features

- **🏠 Home Tab**:
  - **🌿 Ambient Focus Moods**: Forest Zen, Rainy Cafe, Warm Sunset, Midnight Deep, Minimal Dark. Dynamically re-themes backgrounds, glows, status bars, and mood quotes.
  - **⏱️ Pomodoro Timer**: Focus (25m), Short Break (5m), Long Break (15m) with smooth custom circular progress ring, large digital countdown, and active session status.
  - **⚙️ Custom Timer Settings**: Configure any custom duration (1–180 minutes) with quick one-click presets.
  - **⚡ Quick Adjusters**: Instant `[-5m]`, `[-1m]`, `[+1m]`, `[+5m]` adjustment buttons.
  - **🎯 Target Focus Goal**: Star any task to display it right inside the timer countdown as your active mission.
  - **📋 To-Do Tasks Manager**: Add, complete (strikethrough), filter (All / Active / Done), track progress bar, and clear completed tasks.
  - **🎵 Custom MP3 Audio Player**: Upload and import your own audio/MP3 files. Files are copied directly into internal app storage for 100% offline playback. Includes animated equalizer bars, seek scrub bar, previous/next, repeat modes (All / One / Off), and volume slider with mute toggle.
- **📊 History Tab**:
  - **🔥 Streak Counter**: Tracks consecutive active focus days (`🔥 X Day Streak`).
  - **📊 7-Day Visual Mini Bar Chart**: Dynamic visual bars showing daily focus minutes across the week with today highlighted and weekly total.
  - **📅 Daywise History Log**: Complete breakdown of past sessions and minutes by date with clear history option.
- **🔔 Notifications**: Native Android notifications when focus and break sessions finish, indicating your completed target task.
- **⛶ Fullscreen**: Distraction-free immersive fullscreen mode toggle.

## 🚀 Building & Running

### Termux (On-Device Build):
```bash
cd focus-android
chmod +x build-apk.sh
./build-apk.sh
```
The compiled, signed APK will be created at:
- `~/FocusMood.apk`
- `/storage/emulated/0/Download/FocusMood.apk`

### Android Studio:
Open the `focus-android` folder in Android Studio and select `Run > Run 'app'`.
