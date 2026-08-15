package firebasepkg

import (
	"context"
	"fmt"
	"os"

	firebase "firebase.google.com/go/v4"
	firebaseauth "firebase.google.com/go/v4/auth"
	"firebase.google.com/go/v4/messaging"
	"google.golang.org/api/option"
)

type Clients struct {
	App       *firebase.App
	Auth      *firebaseauth.Client
	Messaging *messaging.Client
}

func InitClients(ctx context.Context) (*Clients, error) {
	var options []option.ClientOption
	if credentialsPath := os.Getenv("FIREBASE_SERVICE_ACCOUNT_PATH"); credentialsPath != "" {
		options = append(options, option.WithCredentialsFile(credentialsPath))
	}

	app, err := firebase.NewApp(ctx, nil, options...)
	if err != nil {
		return nil, fmt.Errorf("initialize Firebase app: %w", err)
	}
	authClient, err := app.Auth(ctx)
	if err != nil {
		return nil, fmt.Errorf("initialize Firebase Auth client: %w", err)
	}
	messagingClient, err := app.Messaging(ctx)
	if err != nil {
		return nil, fmt.Errorf("initialize Firebase Messaging client: %w", err)
	}
	return &Clients{App: app, Auth: authClient, Messaging: messagingClient}, nil
}
