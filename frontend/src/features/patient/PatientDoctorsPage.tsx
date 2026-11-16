import { useState } from "react";
import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { Stethoscope, Calendar, Search, Star, Clock, ArrowUpRight, CheckCircle2 } from "lucide-react";

const DEMO_DOCTORS = [
  {
    id: 1,
    name: "Dr. Sarah Jenkins",
    department: "Cardiology",
    rating: "4.9",
    experience: "14 yrs",
    nextSlot: "Today, 4:30 PM",
    bio: "Specializing in preventive cardiology, coronary interventions, and echocardiography."
  },
  {
    id: 2,
    name: "Dr. Marcus Chen",
    department: "General Medicine",
    rating: "4.8",
    experience: "11 yrs",
    nextSlot: "Tomorrow, 10:00 AM",
    bio: "Lead primary care internist focusing on complex chronic care management and preventative diagnostics."
  },
  {
    id: 3,
    name: "Dr. Elena Rostova",
    department: "Neurology",
    rating: "5.0",
    experience: "16 yrs",
    nextSlot: "Today, 6:00 PM",
    bio: "Head of neurology services with deep expertise in neurovascular and migraine therapies."
  }
];

export function PatientDoctorsPage() {
  const { user } = useAuth();
  const [searchTerm, setSearchTerm] = useState("");
  const [bookedDoctor, setBookedDoctor] = useState<string | null>(null);

  if (!user) return null;

  const filtered = DEMO_DOCTORS.filter(d => 
    d.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    d.department.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <AppShell role="PATIENT" user={user}>
      <div style={{ display: "flex", flexDirection: "column", gap: "2.5rem" }}>
        
        {/* Header */}
        <div style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-end",
          flexWrap: "wrap",
          gap: "1.5rem",
          paddingBottom: "1.5rem",
          borderBottom: "1.5px solid var(--color-ink)"
        }}>
          <div>
            <div style={{
              display: "inline-block",
              fontFamily: "var(--font-mono)",
              fontSize: "0.75rem",
              color: "var(--accent-vermilion)",
              fontWeight: 700,
              textTransform: "uppercase",
              letterSpacing: "0.06em",
              marginBottom: "0.5rem"
            }}>
              [ CLINICAL DIRECTORY // PHYSICIANS ]
            </div>
            <h1 style={{
              fontFamily: "var(--font-serif)",
              fontSize: "clamp(2.5rem, 4vw, 3.5rem)",
              margin: 0,
              color: "var(--color-ink)",
              lineHeight: 1.05
            }}>
              Find a <em style={{ fontStyle: "italic", color: "var(--accent-vermilion)" }}>Physician</em>
            </h1>
          </div>

          <div style={{ position: "relative", minWidth: "300px" }}>
            <Search size={16} color="var(--color-muted)" style={{ position: "absolute", left: "1rem", top: "50%", transform: "translateY(-50%)" }} />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by name or department..."
              className="field-control"
              style={{ paddingLeft: "2.5rem" }}
            />
          </div>
        </div>

        {bookedDoctor && (
          <div style={{
            padding: "1rem 1.25rem",
            background: "var(--accent-wasabi)",
            border: "1.5px solid var(--color-ink)",
            borderRadius: "4px",
            boxShadow: "var(--shadow-hard-xs)",
            display: "flex",
            alignItems: "center",
            gap: "0.5rem",
            fontFamily: "var(--font-mono)",
            fontWeight: 700,
            fontSize: "0.85rem"
          }}>
            <CheckCircle2 size={18} /> Slot requested with {bookedDoctor}! Added to your calendar.
          </div>
        )}

        {/* Doctor Grid */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "1.5rem" }}>
          {filtered.map((doc) => (
            <div
              key={doc.id}
              style={{
                background: "#ffffff",
                border: "2px solid var(--color-ink)",
                borderRadius: "8px",
                padding: "1.75rem",
                boxShadow: "var(--shadow-hard-md)",
                display: "flex",
                flexDirection: "column",
                justifyContent: "space-between"
              }}
            >
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "1rem" }}>
                  <div>
                    <h2 style={{ fontFamily: "var(--font-display)", fontSize: "1.25rem", fontWeight: 800, margin: 0, color: "var(--color-ink)" }}>
                      {doc.name}
                    </h2>
                    <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--accent-vermilion)", fontWeight: 700, textTransform: "uppercase" }}>
                      {doc.department}
                    </span>
                  </div>
                  <span style={{
                    display: "inline-flex",
                    alignItems: "center",
                    gap: "0.2rem",
                    padding: "0.15rem 0.5rem",
                    border: "1px solid var(--color-ink)",
                    background: "var(--bg-canvas)",
                    borderRadius: "4px",
                    fontFamily: "var(--font-mono)",
                    fontSize: "0.75rem",
                    fontWeight: 700
                  }}>
                    <Star size={13} fill="currentColor" color="var(--color-ink)" /> {doc.rating}
                  </span>
                </div>

                <p style={{ fontSize: "0.85rem", color: "var(--color-ink-subtle)", lineHeight: 1.5, margin: "0 0 1.25rem" }}>
                  {doc.bio}
                </p>

                <div style={{ display: "flex", gap: "1rem", padding: "0.75rem", background: "var(--bg-canvas)", border: "1px solid var(--color-ink)", borderRadius: "4px", marginBottom: "1.25rem" }}>
                  <div>
                    <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.68rem", color: "var(--color-muted)" }}>EXPERIENCE</div>
                    <div style={{ fontFamily: "var(--font-display)", fontSize: "0.95rem", fontWeight: 700 }}>{doc.experience}</div>
                  </div>
                  <div style={{ borderLeft: "1px solid var(--color-border)", paddingLeft: "1rem" }}>
                    <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.68rem", color: "var(--color-muted)" }}>EARLIEST OPENING</div>
                    <div style={{ fontFamily: "var(--font-display)", fontSize: "0.95rem", fontWeight: 700, color: "var(--accent-vermilion)" }}>{doc.nextSlot}</div>
                  </div>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setBookedDoctor(doc.name)}
                className="button button-primary"
                style={{ width: "100%", justifyContent: "space-between" }}
              >
                <span>BOOK APPOINTMENT</span>
                <ArrowUpRight size={18} />
              </button>
            </div>
          ))}
        </div>
      </div>
    </AppShell>
  );
}
