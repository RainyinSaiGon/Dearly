FROM golang:1.25-alpine AS builder

ARG SERVICE=server
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -o /dearly-service ./cmd/${SERVICE}

FROM alpine:3.22
RUN apk add --no-cache ca-certificates tzdata
WORKDIR /app
COPY --from=builder /dearly-service ./dearly-service
COPY migrations ./migrations
EXPOSE 8080
ENTRYPOINT ["./dearly-service"]
