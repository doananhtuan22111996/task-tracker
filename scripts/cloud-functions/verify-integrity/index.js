/**
 * Serverless Google Play Integrity token decoder for Task Tracker.
 * Project: task-tracker-2b75d (Project #661684282575)
 * Package: dev.tuandoan.tasktracker
 */

const functions = require("firebase-functions");
const { GoogleAuth } = require("google-auth-library");

const PACKAGE_NAME = "dev.tuandoan.tasktracker";
const PLAY_INTEGRITY_SCOPE = "https://www.googleapis.com/auth/playintegrity";

const auth = new GoogleAuth({
  scopes: [PLAY_INTEGRITY_SCOPE],
});

exports.verifyIntegrity = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed. Use POST." });
  }

  const { integrityToken, requestHash } = req.body;
  if (!integrityToken) {
    return res.status(400).json({ error: "Missing integrityToken parameter." });
  }

  try {
    const client = await auth.getClient();
    const url = `https://playintegrity.googleapis.com/v1/${PACKAGE_NAME}:decodeIntegrityToken`;

    const response = await client.request({
      url,
      method: "POST",
      data: {
        integrity_token: integrityToken,
      },
    });

    const payload = response.data.tokenPayloadExternal || {};
    const appIntegrity = payload.appIntegrity || {};
    const deviceIntegrity = payload.deviceIntegrity || {};
    const accountDetails = payload.accountDetails || {};
    const requestDetails = payload.requestDetails || {};

    const appLicensingVerdict = accountDetails.appLicensingVerdict || "UNEVALUATED";
    const appRecognitionVerdict = appIntegrity.appRecognitionVerdict || "UNEVALUATED";
    const deviceRecognitionVerdict = deviceIntegrity.deviceRecognitionVerdict || [];

    const isLicensed = appLicensingVerdict === "LICENSED";
    const isPlayRecognized = appRecognitionVerdict === "PLAY_RECOGNIZED";
    const meetsDeviceIntegrity = deviceRecognitionVerdict.includes("MEETS_DEVICE_INTEGRITY");

    return res.status(200).json({
      success: true,
      isLicensed,
      isPlayRecognized,
      meetsDeviceIntegrity,
      appLicensingVerdict,
      appRecognitionVerdict,
      deviceRecognitionVerdict,
      recommendedDialogCode: isLicensed ? null : 1, // 1 = GET_LICENSED
      timestampMillis: Date.now(),
    });
  } catch (error) {
    console.error("Error decoding Play Integrity token:", error);
    return res.status(500).json({
      success: false,
      error: error.message || "Failed to decode integrity token",
    });
  }
});
