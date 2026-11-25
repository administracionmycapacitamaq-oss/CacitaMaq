const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_capacitamaq";
const ADMIN_EMAIL = "administracionmycapacitamaq@gmail.com";
const STUDENT_UID = "student_carlos";
const STUDENT_EMAIL = "carlos.pizarro@gmail.com";
const OTHER_UID = "student_pedro";
const OTHER_EMAIL = "pedro.soto@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCreatePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    certificateCode: "CERTIFICACION-CM-202611-A1B2C3",
    courseName: "CERTIFICACION DE CARGADOR FRONTAL CAT992-K",
    description: "CERTIFICACION VALIDADA POR UN AÑO CAPACITAMAQ",
    studentName: "Carlos Emmanuel Pizarro Gaete",
    studentRut: "18.456.789-2",
    studentEmail: STUDENT_EMAIL,
    verifiedByAdminEmail: ADMIN_EMAIL,
    certificateType: "Certificación de competencia",
    operatorName: "Roberto Silva",
    operatorRole: "Instructor certificador",
    operatorIdentifier: "OP-409",
    equipment: "Cargador frontal CAT992-K",
    issuer: "CAPACITAMAQ",
    issuedAt: "2026-11-23",
    expiresAt: "2027-11-23",
    status: "active",
    issuerUrl: "https://kangaroo-4gcts4.my.canva.site/",
    whatsapp: "+56983126221",
    email: ADMIN_EMAIL,
    themeColor: "#f3c970",
    backgroundColor: "#ffffff",
    cardColor: "#fafafa",
    fontChoice: "Manrope",
    template: "classic",
    qrColor: "#102b53",
    qrPattern: "square",
    qrCaption: "Escanea para verificar",
    scanCount: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validStudentProfilePayload(uid, email, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    uid,
    email,
    fullName: "Carlos Emmanuel Pizarro Gaete",
    rut: "18.456.789-2",
    verificationStatus: "pending",
    approvedByEmail: "",
    adminNote: "",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function adminDb() {
  return testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
}

function studentDb(uid = STUDENT_UID, email = STUDENT_EMAIL) {
  return testEnv
    .authenticatedContext(uid, { email, email_verified: true })
    .firestore();
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("certificates").get());
  await assertFails(unauthDb.collection("student_profiles").get());
  await assertFails(
    unauthDb.collection("certificates").doc("cert_1").set(validCreatePayload(ADMIN_UID))
  );
});

test("Official admin (administracionmycapacitamaq@gmail.com) can create, list, update, and delete certificates and config", async () => {
  const db = adminDb();
  const certRef = db.collection("certificates").doc("cert_001");

  await assertSucceeds(certRef.set(validCreatePayload(ADMIN_UID)));
  await assertSucceeds(db.collection("certificates").get());

  await assertSucceeds(
    certRef.update({
      studentName: "Carlos Pizarro Actualizado",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    certRef.update({
      scanCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    db.collection("app_config").doc("verifier").set({
      verifierBaseUrl: "https://capacitamaq-verificar.netlify.app",
      updatedByEmail: ADMIN_EMAIL,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(certRef.delete());
});

test("Non-admin Google user CANNOT create, edit, delete certificates, or self-approve", async () => {
  const sDb = studentDb(STUDENT_UID, STUDENT_EMAIL);

  await assertFails(
    sDb.collection("certificates").doc("spoof").set(validCreatePayload(STUDENT_UID))
  );

  await assertFails(
    sDb
      .collection("student_profiles")
      .doc(STUDENT_UID)
      .set(
        validStudentProfilePayload(STUDENT_UID, STUDENT_EMAIL, {
          verificationStatus: "approved",
          approvedByEmail: ADMIN_EMAIL,
        })
      )
  );

  // Student CAN submit a pending verification request with their own email
  const profileRef = sDb.collection("student_profiles").doc(STUDENT_UID);
  await assertSucceeds(
    profileRef.set(validStudentProfilePayload(STUDENT_UID, STUDENT_EMAIL))
  );

  // Student CANNOT approve themselves on update
  await assertFails(
    profileRef.update({
      verificationStatus: "approved",
      approvedByEmail: ADMIN_EMAIL,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Student CANNOT list other students' profiles
  await assertFails(sDb.collection("student_profiles").get());
});

test("Student can read ONLY their own active certificates AFTER admin approval", async () => {
  const aDb = adminDb();
  const sDb = studentDb(STUDENT_UID, STUDENT_EMAIL);
  const oDb = studentDb(OTHER_UID, OTHER_EMAIL);

  // Admin creates one active cert for Carlos and one for Pedro, plus a pending draft for Carlos
  await assertSucceeds(
    aDb.collection("certificates").doc("cert_carlos_active").set(validCreatePayload(ADMIN_UID))
  );
  await assertSucceeds(
    aDb
      .collection("certificates")
      .doc("cert_carlos_pending")
      .set(validCreatePayload(ADMIN_UID, { status: "pending", certificateCode: "CM-PENDING" }))
  );
  await assertSucceeds(
    aDb
      .collection("certificates")
      .doc("cert_pedro_active")
      .set(
        validCreatePayload(ADMIN_UID, {
          studentName: "Pedro Soto",
          studentEmail: OTHER_EMAIL,
          certificateCode: "CM-PEDRO",
        })
      )
  );

  // Carlos submits his profile -> pending
  await assertSucceeds(
    sDb
      .collection("student_profiles")
      .doc(STUDENT_UID)
      .set(validStudentProfilePayload(STUDENT_UID, STUDENT_EMAIL))
  );

  // While pending, Carlos CANNOT read even his own active cert
  await assertFails(
    sDb
      .collection("certificates")
      .where("status", "==", "active")
      .where("studentEmail", "==", STUDENT_EMAIL)
      .get()
  );

  // Admin approves Carlos
  await assertSucceeds(
    aDb.collection("student_profiles").doc(STUDENT_UID).update({
      verificationStatus: "approved",
      approvedByEmail: ADMIN_EMAIL,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Now Carlos CAN list his own active certs!
  const snap = await assertSucceeds(
    sDb
      .collection("certificates")
      .where("status", "==", "active")
      .where("studentEmail", "==", STUDENT_EMAIL)
      .get()
  );
  if (snap.size !== 1 || snap.docs[0].id !== "cert_carlos_active") {
    throw new Error(`Expected 1 active cert for Carlos, got ${snap.size}`);
  }

  // Carlos STILL CANNOT read Pedro's cert, pending certs, or list all certs
  await assertFails(sDb.collection("certificates").get());
  await assertFails(sDb.collection("certificates").doc("cert_pedro_active").get());
  await assertFails(sDb.collection("certificates").doc("cert_carlos_pending").get());
  await assertFails(oDb.collection("certificates").doc("cert_carlos_active").get());
});

test("Public verification index allows get by exact code only, denies list and non-admin writes", async () => {
  const aDb = adminDb();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const sDb = studentDb(STUDENT_UID, STUDENT_EMAIL);
  const now = new Date(Date.now() - 1000);
  const code = "CERTIFICACION-CM-PUB123";

  const publicPayload = {
    certificateCode: code,
    courseName: "CERTIFICACION DE CARGADOR FRONTAL CAT992-K",
    description: "CERTIFICACION VALIDADA POR UN AÑO CAPACITAMAQ",
    studentName: "Carlos Emmanuel Pizarro Gaete",
    studentRut: "18.456.789-2",
    certificateType: "Certificación de competencia",
    operatorName: "Roberto Silva",
    operatorRole: "Instructor certificador",
    equipment: "Cargador frontal CAT992-K",
    issuer: "CAPACITAMAQ",
    issuedAt: "2026-11-23",
    expiresAt: "2027-11-23",
    status: "active",
    whatsapp: "+56983126221",
    email: ADMIN_EMAIL,
    verifiedByAdminEmail: ADMIN_EMAIL,
    themeColor: "#f3c970",
    backgroundColor: "#ffffff",
    cardColor: "#fafafa",
    updatedAt: now,
  };

  // Student and unauthenticated users CANNOT write or list public_certificates
  await assertFails(unauthDb.collection("public_certificates").doc(code).set(publicPayload));
  await assertFails(sDb.collection("public_certificates").doc(code).set(publicPayload));
  await assertFails(unauthDb.collection("public_certificates").get());
  await assertFails(sDb.collection("public_certificates").get());

  // Official admin publishes public verification record
  await assertSucceeds(aDb.collection("public_certificates").doc(code).set(publicPayload));

  // Public web verifier / unauthenticated user CAN get by exact certificateCode
  const snap = await assertSucceeds(unauthDb.collection("public_certificates").doc(code).get());
  if (!snap.exists || snap.data().verifiedByAdminEmail !== ADMIN_EMAIL) {
    throw new Error("Expected public certificate to be readable by exact code");
  }

  // Listing remains forbidden for non-admins even after data exists
  await assertFails(unauthDb.collection("public_certificates").get());
  await assertFails(sDb.collection("public_certificates").get());

  // Admin can delete/unpublish
  await assertSucceeds(aDb.collection("public_certificates").doc(code).delete());
});

