const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_lifter_01";
const BOB_UID = "bob_lifter_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied across exercises, workoutLogs, and bodyScans", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("exercises").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("workoutLogs").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("bodyScans").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("exercises")
      .doc("ex_1")
      .set({
        userId: ALICE_UID,
        programName: "Hypertrophy Split",
        day: "Sunday",
        name: "Incline Press",
        target: "Upper Chest",
        targetSets: "4 x 8-10",
        instructions: "Control the eccentric.",
        youtubeUrl: "https://www.youtube.com/results?search_query=incline+press",
        sortOrder: 0,
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Owner can list empty subcollections and create, update, and delete exercises", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Empty subcollection list succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("exercises").get()
  );

  // Valid create (no @DocumentId id field)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("exercises")
      .doc("ex_sun_1")
      .set({
        userId: ALICE_UID,
        programName: "Boditrax AI Split",
        day: "Sunday",
        name: "Incline Dumbbell Press",
        target: "Upper Chest",
        targetSets: "4 x 8-10",
        instructions: "30 degree bench, 2s negative.",
        youtubeUrl: "https://www.youtube.com/results?search_query=incline+db+press",
        sortOrder: 0,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("exercises")
      .doc("ex_sun_1")
      .update({
        targetSets: "4 x 10-12",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject @DocumentId 'id' field in payload
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("exercises")
      .doc("ex_sun_2")
      .set({
        id: "ex_sun_2",
        userId: ALICE_UID,
        programName: "Boditrax AI Split",
        day: "Sunday",
        name: "Cable Fly",
        target: "Chest",
        targetSets: "3 x 12-15",
        instructions: "Squeeze center.",
        youtubeUrl: "https://www.youtube.com/results?search_query=cable+fly",
        sortOrder: 1,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject invalid day outside Sunday-Thursday
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("exercises")
      .doc("ex_fri_1")
      .set({
        userId: ALICE_UID,
        programName: "Boditrax AI Split",
        day: "Friday",
        name: "Squat",
        target: "Quads",
        targetSets: "4 x 6",
        instructions: "Brace core.",
        youtubeUrl: "https://www.youtube.com/results?search_query=squat",
        sortOrder: 0,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("exercises")
      .doc("ex_sun_1")
      .update({
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Owner delete succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("exercises")
      .doc("ex_sun_1")
      .delete()
  );
});

test("WorkoutLogs: valid create/update with optional notes omitted or present, rejects ghost fields and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Valid create without optional notes
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("workoutLogs")
      .doc("log_1")
      .set({
        userId: ALICE_UID,
        exerciseId: "ex_sun_1",
        exerciseName: "Incline Dumbbell Press",
        day: "Sunday",
        target: "Upper Chest",
        sessionDate: "2026-11-22",
        setNumber: 1,
        weightKg: 32.5,
        reps: 10,
        rpe: 8,
        completed: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Partial update adding notes
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("workoutLogs")
      .doc("log_1")
      .update({
        weightKg: 35,
        reps: 9,
        notes: "Felt strong on last rep",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("workoutLogs")
      .doc("log_future")
      .set({
        userId: ALICE_UID,
        exerciseId: "ex_sun_1",
        exerciseName: "Incline Dumbbell Press",
        day: "Sunday",
        target: "Upper Chest",
        sessionDate: "2026-11-22",
        setNumber: 2,
        weightKg: 35,
        reps: 8,
        rpe: 9,
        completed: true,
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  // Reject ghost field (Shadow Update test)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("workoutLogs")
      .doc("log_1")
      .update({
        isVerified: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("BodyScans: valid create, cross-user isolation enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bodyScans")
      .doc("scan_1")
      .set({
        userId: ALICE_UID,
        weightKg: 78.4,
        muscleMassKg: 35.2,
        bodyFatPercent: 14.8,
        observations: "Upper chest and rear delts focus",
        aiSummary: "5-day hypertrophy split generated",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Cross-user read/write/list denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bodyScans").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bodyScans")
      .doc("scan_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bodyScans")
      .doc("scan_2")
      .set({
        userId: BOB_UID,
        weightKg: 80,
        muscleMassKg: 36,
        bodyFatPercent: 15,
        observations: "Hijack attempt",
        createdAt: created,
        updatedAt: created,
      })
  );
});
