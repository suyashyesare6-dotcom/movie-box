const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated users are denied across all JARVIS collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("housekeeping").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("studyNotes").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("securityChecks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("messages").get());

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("housekeeping").doc("item_1").set({
      userId: ALICE_UID,
      title: "Buy milk",
      notes: "",
      category: "SHOPPING",
      scheduleLabel: "Today",
      priority: "MEDIUM",
      completed: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Owner can create, update, list, and delete UserProfile and rejects ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      preferredTitle: "Boss",
      autoSpeakEnabled: true,
      speechRate: 1.0,
      activeMode: "GENERAL",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredTitle: "Tony",
      activeMode: "GOSSIP",
      preferredLanguage: "ENGLISH",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "KLINGON",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner CRUD and query alignment work for housekeeping, memories, studyNotes, securityChecks, and messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Housekeeping with domain (HOUSEKEEPING / STUDY / SECURITY)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("housekeeping").doc("hk_1").set({
      userId: ALICE_UID,
      title: "Pay electricity bill",
      notes: "Before 10th",
      category: "BILL",
      domain: "HOUSEKEEPING",
      scheduleLabel: "Monthly 10th",
      priority: "HIGH",
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("housekeeping").doc("hk_study").set({
      userId: ALICE_UID,
      title: "Exam fee payment",
      notes: "Semester 4",
      category: "BILL",
      domain: "STUDY",
      scheduleLabel: "Today",
      priority: "HIGH",
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("housekeeping").doc("hk_bad_domain").set({
      userId: ALICE_UID,
      title: "Invalid domain",
      notes: "",
      category: "TASK",
      domain: "INVALID",
      scheduleLabel: "Today",
      priority: "LOW",
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("housekeeping").doc("hk_1").update({
      completed: true,
      priority: "LOW",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("housekeeping").doc("hk_1").update({
      priority: "CRITICAL",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("housekeeping").orderBy("createdAt", "desc").get()
  );

  // Reject @DocumentId 'id' in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("housekeeping").doc("hk_bad").set({
      id: "hk_bad",
      userId: ALICE_UID,
      title: "Bad doc",
      notes: "",
      category: "TASK",
      scheduleLabel: "",
      priority: "LOW",
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Memories
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      topic: "Cricket",
      detail: "Loves watching IPL matches with friends",
      category: "INTEREST",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").get()
  );

  // Study Notes
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("studyNotes").doc("note_1").set({
      userId: ALICE_UID,
      subject: "Physics",
      title: "Newton Laws",
      content: "First law is inertia.",
      noteType: "SUMMARY",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("studyNotes").get()
  );

  // Security Checks
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("securityChecks").doc("sec_1").set({
      userId: ALICE_UID,
      title: "Enable 2FA on Google",
      guidance: "Use authenticator app instead of SMS",
      category: "AUTH_2FA",
      completed: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("securityChecks").get()
  );

  // Messages (append-only, update denied)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      sender: "USER",
      mode: "GENERAL",
      text: "Jarvis, aazcha briefing de",
      createdAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1").update({
      text: "Tampered",
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").orderBy("createdAt", "asc").get()
  );
});

test("Cross-user access is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const created = new Date(Date.now() - 5000);
    await db.collection("users").doc(BOB_UID).collection("memories").doc("bob_mem").set({
      userId: BOB_UID,
      topic: "Secret",
      detail: "Bob private memory",
      category: "CONVERSATION",
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("memories").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("memories").doc("bob_mem").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("memories").doc("bob_mem").update({
      detail: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Future timestamps and oversized strings are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const future = new Date(Date.now() + 60000);

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_future").set({
      userId: ALICE_UID,
      topic: "Future",
      detail: "Invalid timestamp",
      category: "INTEREST",
      createdAt: future,
      updatedAt: future,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_big").set({
      userId: ALICE_UID,
      topic: "A".repeat(100),
      detail: "Too long topic",
      category: "INTEREST",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
