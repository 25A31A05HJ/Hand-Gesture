# Bloom Presenter (hand-gesture slides)

Light yellow + sky blue web app. Works with your left hand, right hand, or both at once.

## Put it online with Vercel (gets you a URL)

Files `index.html` and `vercel.json` are at the top of this folder, so there is nothing to configure.

Option A - website (no install):
1. Create a GitHub repo and upload everything in this folder.
2. vercel.com > Add New > Project > import the repo.
3. Framework Preset: Other. Leave Root Directory, Build Command and Output Directory empty. Press Deploy.

Option B - command line:
    npm i -g vercel
    cd gesture-presenter
    vercel --prod

Open the URL (it is https, which the camera needs), press "Start camera" and allow access.

## Gestures (either hand)
1 finger = plant flowers | 2 fingers = hold ring on a button | 3 fingers = erase
open palm swipe = next / previous slide | fist = pause

Tips: good light, plain background, keep hands about an arm's length from the camera.

## Optional: Java desktop version
    cd java-app
    mvn compile exec:java
Needs JDK 17+ and Maven. (Vercel ignores this folder.)
## 🌼 Live Demo

**👉https://vercel.com/tech-trio3/handgesture/8USvShsw4nSTdpuj2XqoAX1Sp4Yd**

Open the link in Chrome or Edge, press **Start camera**, allow camera access, and control the slides with either hand.

> Note: the camera only works on `https` links or `localhost`.

## ✨ Features

- Works with the **left hand, right hand, or both hands at the same time**
- Plant flowers on your slides by pointing with one finger
- Hands-free slide control with an open-palm swipe
- Erase, undo, clear and save your drawing as a PNG
- Upload your own slide images and present hands-free
- Mouse and touch fallback, so you can test without a camera
- Keyboard shortcuts: `←` `→` change slides, `C` clears, `F` toggles fullscreen
- Light yellow and sky blue theme

## 🖐️ How the Gestures Work

| Gesture | Action |
|---|---|
| ☝️ One finger up | Plant flowers |
| ✌️ Two fingers up | Move the ring over a button and hold to press it |
| 🤟 Three fingers up | Erase |
| 🖐️ Open palm, swipe right | Next slide |
| 🖐️ Open palm, swipe left | Previous slide |
| ✊ Fist | Pause |

The webcam finds 21 points on each hand. Which fingers are up or down decides the action.

## 🛠️ Tech Stack

- HTML, CSS and JavaScript
- [MediaPipe Hands](https://developers.google.com/mediapipe) for hand tracking in the browser
- Canvas API for the flower drawing
- Vercel for hosting
- Optional desktop version: Java, OpenCV and Maven

## 📁 Project Structure

```
gesture-presenter/
├── index.html        # the whole web app
├── vercel.json       # allows camera access on Vercel
├── README.md
└── java-app/         # optional Java + OpenCV desktop version
    ├── pom.xml
    └── src/main/java/gesture/
        ├── Main.java
        ├── HandDetector.java
        └── FlowerLayer.java
```

## 💻 Run Locally

```bash
npx serve
```

Open `http://localhost:3000` in Chrome.

## ❓ Troubleshooting

- **Camera does not start:** allow camera access in the browser address bar. Use https or localhost.
- **Hand not detected:** use good lighting and a plain background, and keep your hands about an arm's length from the camera.
- **Gestures feel jumpy:** hold the pose steady for a moment before moving.
- **Camera works in Chrome but not in the VS Code Simple Browser:** open the link in a normal browser.
- **Another app is using the webcam:** close Zoom, Teams or other camera apps.

## 🚀 Future Improvements

- More gestures (zoom, laser pointer)
- Import slides directly from PDF or PowerPoint
- Custom flower shapes and brush sizes
- Multiple language support

## 👩‍💻 Developer

**Veera Purnima Venkata**

- 📧 Email: purnimaveera678@gmail.com
- 🐙 GitHub: [https://github.com/25A31A05HJ]




