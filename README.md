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
