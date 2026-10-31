# Chord Follow

אפליקציית Android בעברית שמציגה את האקורד הנוכחי ואת האקורד הבא, ומתקדמת אוטומטית כאשר מנגנים את צלילי האקורד דרך מקלדת USB-MIDI.

## יכולות
- ממשק כהה בעברית.
- הצגת האקורד הנוכחי והאקורד הבא.
- ייבוא אקורדים מתוך קובץ PDF.
- הדבקת רצף אקורדים כטקסט.
- מצב פשוט ומצב מלא.
- זיהוי תווי MIDI דרך התקן USB-MIDI.
- תמיכה ב-Android 5.0 ומעלה (API 21).

## בנייה
הפרויקט משתמש ב-Gradle 8.7, Android Gradle Plugin 8.5.2 ו-JDK 17.

```bash
gradle testDebugUnitTest
gradle assembleRelease
```

קובץ ה-APK נוצר בנתיב:
`app/build/outputs/apk/release/app-release-unsigned.apk`

כל push לענף main מפעיל בדיקות ובניית APK. הקובץ מתפרסם גם כארטיפקט של GitHub Actions וגם ב-GitHub Release.
