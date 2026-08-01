# ─── Build stage ──────────────────────────────────────────────────
FROM golang:1.22-alpine AS builder

WORKDIR /app

# Cache module downloads
COPY go.mod go.sum* ./
RUN go mod download 2>/dev/null || true

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o /dearly-api ./cmd/server

# ─── Runtime stage ────────────────────────────────────────────────
FROM alpine:3.20

RUN apk add --no-cache ca-certificates tzdata

WORKDIR /app
COPY --from=builder /dearly-api .

EXPOSE 8080

ENTRYPOINT ["./dearly-api"]
