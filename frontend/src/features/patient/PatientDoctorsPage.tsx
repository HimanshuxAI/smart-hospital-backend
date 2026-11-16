import { useState } from "react";
import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { Stethoscope, Search, Star, Clock, ArrowRight, CheckCircle2 } from "lucide-react";

const DOCTORS_DATA = [
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
  const [search, setSearch] = useState("");
  const [selectedDoc, setSelectedDoc] = useState<string | null>(null);

  if (!user) return null;

  const filtered = DOCTORS_DATA.filter(d => 
    d.name.toLowerCase().includes(search.toLowerCase()) || 
    d.department.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <AppShell role="PATIENT" user={user}>
      <div style={{ display: "flex", flexDirection: "column", gap: "1.75rem" }}>
        
        {/* Header with Search */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", flexWrap: "wrap", gap: "1rem" }}>
          <div>
            <h1 style={{ fontSize: "1.75rem", fontWeight: 700, color: "var(--text-main)", letterSpacing: "-0.02em" }}>
              Find a Doctor
            </h1>
            <p style={{ color: "var(--text-muted)", fontSize: "0.92rem", marginTop: "0.2rem" }}>
              Browse specialists and book your next clinical consultation.
            </p>
          </div>

          <div style={{ position: "relative", width: "280px" }}>
            <Search size={16} color="var(--text-light)" style={{ position: "absolute", left: "0.75rem", top: "50%", transform: "translateY(-50%)" }} />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by doctor or specialty..."
              className="field-control"
              style={{ paddingLeft: "2.25rem" }}
            />
          </div>
        </div>

        {selectedDoc && (
          <div style={{
            padding: "0.85rem 1.25rem",
            background: "var(--badge-green-bg)",
            border: "1px solid var(--badge-green-border)",
            borderRadius: "8px",
            display: "flex",
            alignItems: "center",
            gap: "0.5rem",
            fontSize: "0.875rem",
            color: "var(--badge-green-text)",
            fontWeight: 500
          }}>
            <CheckCircle2 size={18} /> Appointment successfully scheduled with {selectedDoc}!
          </div>
        )}

        {/* Doctor Grid */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "1.25rem" }}>
          {filtered.map((doc) => (
            <div key={doc.id} className="card-saas" style={{ display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "0.85rem" }}>
                  <div>
                    <h2 style={{ fontSize: "1.1rem", fontWeight: 700, color: "var(--text-main)" }}>
                      {doc.name}
                    </h2>
                    <span style={{ fontSize: "0.8rem", color: "var(--primary)", fontWeight: 600 }}>
                      {doc.department}
                    </span>
                  </div>
                  <span style={{
                    display: "inline-flex",
                    alignItems: "center",
                    gap: "0.25rem",
                    padding: "0.15rem 0.5rem",
                    background: "var(--bg-subtle)",
                    border: "1px solid var(--border-color)",
                    borderRadius: "4px",
                    fontSize: "0.75rem",
                    fontWeight: 600
                  }}>
                    <Star size={12} fill="#eab308" color="#eab308" /> {doc.rating}
                  </span>
                </div>

                <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", lineHeight: 1.5, marginBottom: "1rem" }}>
                  {doc.bio}
                </p>

                <div style={{ display: "flex", gap: "1rem", padding: "0.75rem", background: "var(--bg-subtle)", borderRadius: "6px", marginBottom: "1rem" }}>
                  <div>
                    <div style={{ fontSize: "0.7rem", color: "var(--text-muted)" }}>Experience</div>
                    <div style={{ fontSize: "0.875rem", fontWeight: 600 }}>{doc.experience}</div>
                  </div>
                  <div style={{ borderLeft: "1px solid var(--border-color)", paddingLeft: "1rem" }}>
                    <div style={{ fontSize: "0.7rem", color: "var(--text-muted)" }}>Next Available</div>
                    <div style={{ fontSize: "0.875rem", fontWeight: 600, color: "#16a34a" }}>{doc.nextSlot}</div>
                  </div>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setSelectedDoc(doc.name)}
                className="button button-primary"
                style={{ width: "100%", justifyContent: "center" }}
              >
                <span>Select & Book Slot</span>
              </button>
            </div>
          ))}
        </div>
      </div>
    </AppShell>
  );
}
