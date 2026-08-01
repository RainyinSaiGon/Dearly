package medication

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Handler handles HTTP requests for the medication schedule module.
type Handler struct {
	// TODO(W3): Inject MedicationService
	// service *Service
}

func NewHandler() *Handler { return &Handler{} }

func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.GET("", h.List)
	rg.POST("", h.Create)
	rg.PUT("/:id", h.Update)
	rg.DELETE("/:id", h.Delete)
	rg.POST("/:id/taken", h.MarkTaken)
}

// List returns all medications and today's dose log for an elder.
//
// TODO(W3):
//  1. Extract elderID from JWT context
//  2. SELECT m.*, ml.status, ml.scheduled_time FROM medications m
//     LEFT JOIN medication_logs ml ON ml.medication_id = m.id
//     AND ml.scheduled_time::date = CURRENT_DATE
//     WHERE m.elder_id = $1
//  3. Group dose slots with their TAKEN/PENDING/SNOOZED status
func (h *Handler) List(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// Create adds a new medication with its schedule.
//
// TODO(W3):
//  1. Bind JSON: { name, frequency_per_day, time_slots: ["07:00","13:00","19:00"] }
//  2. Validate time_slots length == frequency_per_day
//  3. INSERT INTO medications (elder_id, name, frequency_per_day, time_slots) VALUES (...)
//  4. Schedule FCM notification jobs for each time_slot (see notification package)
//  5. Return 201 Created with the new medication
func (h *Handler) Create(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// Update edits a medication's name, frequency, or time slots.
//
// TODO(W3):
//  1. Verify ownership (medication.elder_id == requesting user or their caregiver)
//  2. Update medication row
//  3. Re-schedule FCM notification jobs for updated time_slots
func (h *Handler) Update(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// Delete removes a medication and its log history.
//
// TODO(W3):
//  1. Verify ownership
//  2. DELETE FROM medications WHERE id = $1 AND elder_id = $2
//     (cascades to medication_logs via FK)
//  3. Cancel any pending FCM notification jobs for this medication
func (h *Handler) Delete(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// MarkTaken marks a specific dose slot as TAKEN.
// Called by the elder from the app or from the notification shade "Taken" action.
//
// TODO(W3):
//  1. Bind JSON: { scheduled_time: "2026-08-01T07:00:00Z" }
//  2. UPDATE medication_logs SET status='TAKEN', taken_at=NOW()
//     WHERE medication_id=$1 AND scheduled_time=$2
//  3. If no log row exists yet, INSERT one (upsert)
//  4. Trigger caregiver notification: "Elder has taken [med name]"
func (h *Handler) MarkTaken(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}
