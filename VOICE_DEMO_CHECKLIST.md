# Dearly real-voice demonstration checklist

This checklist produces the evidence required by the final-project rubric. Do
not substitute mock recordings, screenshots of source code, or a successful
build for these demonstrations.

## Before recording

1. Start the services with `docker compose up --build` and confirm the Android
   app is signed in to the same backend.
2. Use two real accounts linked as an elder and caregiver. Enroll five phrases
   for each person in **Settings - Voice enrollment**.
3. Add one unpaid medication for the elder, for example `Amlodipine` at the
   current time. Use the same quiet room and microphone for all recordings.
4. Start screen recording on the emulator/device. Keep the waveform and the
   recognized transcript visible whenever possible.

## Evidence 1 - Enrollment and recording quality

1. Open the enrollment dialog and show progress `0/5`.
2. Make a silent or very short recording. Show Dearly's Vietnamese retry
   message; the count must remain unchanged.
3. Record all five displayed phrases clearly. Show the waveform while speaking
   and the final `5/5` completion state.

Expected: only accepted speech is uploaded; raw temporary recordings disappear
after the request and the app retains the five-vector enrollment profile.

## Evidence 2 - General function before sign-in

1. Sign out and choose **Hỏi Dearly không cần đăng nhập**.
2. Ask `Bây giờ là mấy giờ?` or `Hôm nay là ngày mấy?`.
3. Capture the transcript and spoken/text reply.

Expected: Dearly answers without requesting verification and does not expose a
medication schedule, contacts, or identity information.

## Evidence 3 - Protected medication confirmation

1. Sign in as the elder and open **Gọi điện**.
2. Say `Tôi đã uống Amlodipine rồi`.
3. Capture Dearly's named-dose-and-time confirmation prompt, for example:
   `Bác xác nhận đã uống Amlodipine lúc 08:30 phải không?`.
4. Say `Đúng rồi` clearly. Show the waveform, transcript, and success message.
5. Open **Lịch thuốc** and show that the matching dose changed to **Đã uống**.

Expected: the dose changes only after the confirmation utterance passes speaker
verification and the one-use grant is consumed.

## Evidence 4 - Failed verification and replay protection

1. Create another unpaid dose.
2. Repeat Evidence 3 but let the second, linked user say `Đúng rồi`.
3. Show the failed-verification message and verify that the dose stays unpaid.
4. Immediately upload/reuse the successful `Đúng rồi` recording from Evidence
   3 through the same protected flow.

Expected: the failed attempt is recorded in `GET /api/v1/voice/verification-audit`;
the reused successful recording is rejected with `voice_recording_replayed`.

## Evidence 5 - SID personalization

1. With both linked accounts enrolled, ask from each account: `Hôm nay tôi cần
   uống thuốc gì?`.
2. Capture each recognized name and the schedule selected for that identified
   user.

Expected: only the caller and their directly linked caregiver/elder accounts
are candidates. The backend re-checks the relationship before returning a name
or schedule.

## Evidence 6 - Preferences beyond the name

1. For the enrolled elder, set `reminder_style` to `GENTLE`, `speech_rate` to
   `0.80`, choose a preferred contact, and enable the daily schedule through
   `PUT /api/v1/users/me/voice-preferences`.
2. Ask `Hôm nay uống thuốc gì?` using that elder's enrolled voice.
3. Capture the recognized name, gentle reminder wording, named preferred
   contact, daily schedule, and the Android `Cá nhân hoá theo giọng nói` label.
4. Change the preference to `DIRECT` and repeat once to demonstrate that the
   response wording changes while the schedule still belongs to the SID user.

Expected: a public query never receives these preferences; a SID result outside
the trusted group never loads them either.

## Report screenshots

Replace Figures 3-6 in `report/main.tex` with captures from Evidence 1-5.
Caption the failed verification/replay case honestly and include the two-user
SID result. Do not claim a live result that was not recorded.
