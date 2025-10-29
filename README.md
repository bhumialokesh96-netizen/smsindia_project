SMSIndia - polished Java + XML skeleton (generated)

Project location: /mnt/data/smsindia_project
App module: /mnt/data/smsindia_project/app

IMPORTANT:
- Place your real google-services.json at app/google-services.json (already copied: True)
- Create local.properties with your sdk.dir if building locally.

Build:
  chmod +x ./gradlew
  ./gradlew assembleDebug

Notes:
- This project is a functional skeleton: SIM selection, SMS sending via SmsManager, WorkManager worker that updates Firestore balance.
- Test on a real device (emulator can't send SMS).
