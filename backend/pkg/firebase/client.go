package firebasepkg

// InitApp initializes the Firebase Admin SDK app.
//
// TODO(W2):
//  1. Import: firebase.google.com/go/v4
//             firebase.google.com/go/v4/auth
//             firebase.google.com/go/v4/messaging
//
//  2. Add the import to go.mod:
//       go get firebase.google.com/go/v4
//
//  3. Implementation pattern:
//
//     import (
//         firebase "firebase.google.com/go/v4"
//         "google.golang.org/api/option"
//     )
//
//     func InitApp() (*firebase.App, error) {
//         saPath := os.Getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
//         opt := option.WithCredentialsFile(saPath)
//         return firebase.NewApp(context.Background(), nil, opt)
//     }
//
//  4. From the returned *firebase.App, call:
//       app.Auth(ctx)      → *auth.Client     (for OTP verification)
//       app.Messaging(ctx) → *messaging.Client (for FCM push)
//
//  5. Wire both clients into AuthService and NotificationService respectively.

// Placeholder to keep the package non-empty until the real implementation is added.
const Placeholder = "firebase package — implement in W2, see TODO above"
