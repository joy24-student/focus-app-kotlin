# Focus-app

A clean, aesthetic, and distraction-free Pomodoro & Focus Mood web application built with vanilla HTML5, CSS3, and JavaScript.

---

## ✨ Features

- **🌿 Ambient Focus Moods**:
  - Switch between 5 atmospheric color themes: *Forest Zen*, *Rainy Cafe*, *Warm Sunset*, *Midnight Deep*, and *Minimal Dark*.
  - Synchronizes browser theme colors and smooth backdrop glows.

- **⏱️ Flexible Focus Timer**:
  - Standard Pomodoro presets: Deep Focus (25m), Short Break (5m), and Long Break (15m).
  - **Custom Timer Settings**: Set any custom duration (1–180 minutes) with one-click presets.
  - **Quick Adjusters**: Tap `[-5m]`, `[-1m]`, `[+1m]`, or `[+5m]` to fine-tune time on the fly.
  - Interactive SVG progress ring with smooth countdown tracking.

- **🔔 Browser Notifications**:
  - Native browser notification alerts when focus sessions and break sessions finish.
  - Custom notification title and details including your active target task.
  - Dedicated notification permission toggle (`🔔`) in the header with visual active/blocked status indicators and test notification trigger.

- **🎵 Custom MP3 Audio Player**:
  - **Custom Upload**: Upload your own favorite MP3 audio, study beats, or ambient tracks via a simple file selector.
  - **LocalStorage Persistence**: Uploaded songs are automatically encoded and saved directly in browser `localStorage` for offline playback without re-uploading.
  - **Easy Song Changing**:
    - Clean playlist interface displaying all uploaded tracks, file sizes, and active indicators.
    - One-click song switching: tap any song to play it instantly.
    - Previous (`⏮`) and Next (`⏭`) track controls.
    - Animated audio equalizer visualization while playing.
    - Interactive seek scrub bar with current time and total duration timestamps.
    - Repeat modes: Loop All (`🔁`), Loop Current Song (`🔂`), or Repeat Off (`➡️`).
    - Smooth volume slider with 1-click mute/unmute toggle (`🔇` / `🔉` / `🔊`).
    - Song management: individual track delete buttons (`✕`) and "Clear All" option with storage usage counter.
  - **All default sounds removed**: No default synthetic noise generators or unwanted audio chimes; you have 100% control over your focus music.

- **📋 To-Do Task Manager**:
  - Add, complete, and delete focus tasks.
  - **🎯 Target Focus**: Star any task to display it directly on the timer countdown as your active mission.
  - Filter tasks by *All*, *Active*, or *Done*, with live completion progress bar.

- **📊 Daywise Activity & Streak Tracker**:
  - **7-Day Visual Mini Bar Chart**: Dynamic bars showing daily focus minutes across the week.
  - **Streak Counter**: Tracks consecutive active focus days (`🔥 X Day Streak`).
  - **Daywise History**: Expandable log showing breakdown of sessions and minutes by date.

- **📱 Mobile Responsive & Touch-Optimized**:
  - Fully responsive layout for mobile, tablet, and desktop viewports.
  - Full-screen distraction-free mode toggle.

- **💾 100% LocalStorage Persistence**:
  - Custom audio tracks, playback preferences, tasks, custom timer durations, selected mood, daywise statistics, and streak counts persist seamlessly across browser sessions.

---

## 🚀 Getting Started

No external dependencies required! Simply clone and open in any modern browser:

```bash
git clone https://github.com/joy24-student/Focus-app.git
cd Focus-app
```

### Run Locally

Using Python's built-in web server:

```bash
python -m http.server 8080
```

Then visit:
**`http://localhost:8080`**

---

## 🛠️ Built With

- **HTML5** — Semantic layout, audio element & SVG circular progress
- **CSS3** — Custom properties, dynamic palettes, glassmorphism & responsive CSS Grid/Flexbox
- **JavaScript (ES6+)** — Custom audio player engine, Web Notification API, timer state machine, and localStorage management
