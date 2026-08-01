package auth

// Service contains the business logic for authentication.
//
// TODO(W2): Implement all methods below.
// Dependencies to inject via constructor:
//   - db        *sql.DB           (or a UserRepository interface)
//   - redisClient redis.Client
//   - firebaseApp *firebase.App
//   - jwtService *jwt.Service
type Service struct {
	// TODO(W2): add fields
}

// NewService creates a new AuthService.
// TODO(W2): Accept concrete dependencies and wire them here.
func NewService() *Service {
	return &Service{}
}

// SendOTP triggers an OTP SMS to the given phone number.
//
// TODO(W2):
//  1. Rate-limit: reject if 3+ OTP requests in last 5 min (Redis INCR + TTL)
//  2. Use Firebase Admin SDK phone auth to send verification SMS
//  3. Return a session/verificationID to the client
func (s *Service) SendOTP(phone string) error {
	// TODO(W2): implement
	return nil
}

// VerifyOTP validates the OTP, upserts the user, and returns JWT tokens.
//
// TODO(W2):
//  1. Verify the Firebase ID token with firebase.Auth.VerifyIDToken()
//  2. Extract phone from token claims
//  3. Upsert user in DB: INSERT ... ON CONFLICT (phone_number) DO UPDATE
//  4. Generate access + refresh JWTs (pkg/jwt)
//  5. Store refresh token hash in Redis (key: "refresh:<userID>", TTL: 7d)
//  6. Return access token, refresh token, and user struct
func (s *Service) VerifyOTP(firebaseIDToken string) (accessToken, refreshToken string, err error) {
	// TODO(W2): implement
	return "", "", nil
}

// GoogleSignIn verifies a Google ID token and upserts the user.
//
// TODO(W2):
//  1. Verify token via firebase.Auth.VerifyIDToken()
//  2. Extract email, name, picture from token claims
//  3. Upsert user in DB by email
//  4. Generate and return JWT tokens
func (s *Service) GoogleSignIn(googleIDToken string) (accessToken, refreshToken string, err error) {
	// TODO(W2): implement
	return "", "", nil
}

// RefreshToken validates a refresh token and issues a new access token.
//
// TODO(W2):
//  1. Parse the refresh token JWT and validate signature + expiry
//  2. Look up token hash in Redis — reject if revoked
//  3. Generate a new access token (15min)
//  4. Optional: rotate refresh token and update Redis
func (s *Service) RefreshToken(refreshToken string) (newAccessToken string, err error) {
	// TODO(W2): implement
	return "", nil
}
