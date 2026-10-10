# Google Play Integrity Token Verification Cloud Function

Serverless Firebase Cloud Function to decrypt and decode Google Play Integrity API tokens for **Task Tracker** (`dev.tuandoan.tasktracker`).

---

## 1. Architecture Overview

As recommended in Google's official [Play Integrity documentation](https://developer.android.com/google/play/integrity/overview):
1. The Task Tracker Android app requests an integrity token via `StandardIntegrityManager.request()` using a dynamic SHA-256 request hash.
2. The token is sent to this secure backend endpoint.
3. This Cloud Function calls Google's `playintegrity.googleapis.com/v1/{packageName}:decodeIntegrityToken` REST API using authenticated Google Cloud application default credentials.
4. The decoded verdict (`appLicensingVerdict`, `appRecognitionVerdict`, `deviceRecognitionVerdict`) is evaluated and returned to the client.

---

## 2. Prerequisites

- **Node.js**: >= 18.0.0
- **Firebase CLI**: `npm install -g firebase-tools`
- **GCP Project**: `task-tracker-2b75d` (Project Number: `661684282575`)
- **API Enabled**: Enable the **Play Integrity API** in [Google Cloud Console API Library](https://console.cloud.google.com/apis/library/playintegrity.googleapis.com) for project `task-tracker-2b75d`.
- **Play Console Linking**: Link Google Cloud Project `661684282575` in **Google Play Console** -> **Release** -> **App integrity** -> **Link a Cloud project**.

---

## 3. Deployment

```bash
# 1. Login to Firebase CLI
firebase login

# 2. Select project
firebase use task-tracker-2b75d

# 3. Install dependencies
cd scripts/cloud-functions/verify-integrity
npm install

# 4. Deploy function
npm run deploy
# Or: firebase deploy --only functions:verifyIntegrity
```

---

## 4. API Specification

### Endpoint
`POST https://us-central1-task-tracker-2b75d.cloudfunctions.net/verifyIntegrity`

### Headers
```http
Content-Type: application/json
```

### Request Body
```json
{
  "integrityToken": "eyJhbGciOiJ...",
  "requestHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

### Response (Success - 200 OK)
```json
{
  "success": true,
  "isLicensed": true,
  "isPlayRecognized": true,
  "meetsDeviceIntegrity": true,
  "appLicensingVerdict": "LICENSED",
  "appRecognitionVerdict": "PLAY_RECOGNIZED",
  "deviceRecognitionVerdict": [
    "MEETS_DEVICE_INTEGRITY",
    "MEETS_BASIC_INTEGRITY"
  ],
  "recommendedDialogCode": null,
  "timestampMillis": 1728567000000
}
```

### Remediation Guidance
If `isLicensed` is `false` (e.g. `UNLICENSED`), the response returns `"recommendedDialogCode": 1` which corresponds to `StandardIntegrityDialogRequest.StandardIntegrityDialogTypeCode.GET_LICENSED`, allowing the client app to invoke Play Integrity's native dialog prompt.
