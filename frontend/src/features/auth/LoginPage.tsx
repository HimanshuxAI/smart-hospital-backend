import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ArrowUpRight, Zap, Shield, Key, Stethoscope, User, Fingerprint } from "lucide-react";
import { useAuth } from "../../auth/auth-context";
import { validateEmail, validatePassword } from "../../lib/validation";

export function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);

  const fillCredentials = (demoEmail: string, demoPass: string) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setFieldErrors({});
    setGeneralError(null);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setGeneralError(null);
    const errors: Record<string, string> = {};

    const emailErr = validateEmail(email);
    if (emailErr) errors.email = emailErr;

    const passErr = validatePassword(password);
    if (passErr) errors.password = passErr;

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setFieldErrors({});
    setLoading(true);

    try {
      const user = await signIn({ email, password });
      const from = (location.state as { from?: string })?.from;
      if (from) {
        navigate(from, { replace: true });
      } else {
        const dest = user.role === "PATIENT" ? "/patient" : user.role === "DOCTOR" ? "/doctor" : "/admin";
        navigate(dest, { replace: true });
      }
    } catch (err: unknown) {
      const msg = err && typeof err === "object" && "detail" in err ? String((err as { detail: unknown }).detail) : "Invalid email or password";
      setGeneralError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: "100vh",
      background: "var(--bg-canvas)",
      display: "flex",
      flexDirection: "column",
      justifyContent: "space-between",
      padding: "2rem clamp(1.5rem, 5vw, 4rem)",
      fontFamily: "var(--font-body)"
    }}>
      {/* Top Architectural Header Strip */}
      <header style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        paddingBottom: "1.5rem",
        borderBottom: "1.5px solid var(--color-ink)"
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
          <div style={{
            width: "2.25rem",
            height: "2.25rem",
            background: "var(--color-ink)",
            color: "var(--bg-canvas)",
            display: "grid",
            placeItems: "center",
            fontWeight: 800,
            fontSize: "1rem",
            fontFamily: "var(--font-display)",
            borderRadius: "4px"
          }}>
            Æ
          </div>
          <div>
            <div style={{ fontFamily: "var(--font-display)", fontSize: "1.1rem", fontWeight: 800, letterSpacing: "-0.02em" }}>
              AEGLE CLINICAL OS
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.68rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
              SYS.ID: TOKYO-MED-992 // RUNNING
            </div>
          </div>
        </div>

        <div style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
          <span style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "0.4rem",
            padding: "0.25rem 0.65rem",
            border: "1.5px solid var(--color-ink)",
            background: "var(--accent-wasabi)",
            borderRadius: "3px",
            fontSize: "0.7rem",
            fontFamily: "var(--font-mono)",
            fontWeight: 700,
            boxShadow: "2px 2px 0px var(--color-ink)"
          }}>
            <span className="status-dot" /> LIVE ENGINE
          </span>
          <Link
            to="/register"
            style={{
              padding: "0.35rem 0.85rem",
              border: "1.5px solid var(--color-ink)",
              borderRadius: "3px",
              background: "#ffffff",
              fontSize: "0.75rem",
              fontWeight: 700,
              fontFamily: "var(--font-display)",
              color: "var(--color-ink)",
              boxShadow: "2px 2px 0px var(--color-ink)"
            }}
          >
            Create Profile →
          </Link>
        </div>
      </header>

      {/* Main Structural Asymmetric Grid */}
      <main style={{
        display: "grid",
        gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))",
        gap: "4rem",
        alignItems: "center",
        padding: "3.5rem 0"
      }}>
        {/* Left Column: Bold Editorial Typographic Statement */}
        <div>
          <div style={{
            display: "inline-block",
            fontFamily: "var(--font-mono)",
            fontSize: "0.75rem",
            color: "var(--accent-vermilion)",
            fontWeight: 700,
            letterSpacing: "0.06em",
            textTransform: "uppercase",
            marginBottom: "1rem"
          }}>
            [ 01 // AUTONOMOUS TRIAGE DISPATCH ]
          </div>

          <h1 style={{
            fontFamily: "var(--font-serif)",
            fontSize: "clamp(2.8rem, 5.5vw, 4.5rem)",
            fontWeight: 400,
            lineHeight: 1.05,
            letterSpacing: "-0.03em",
            color: "var(--color-ink)",
            margin: "0 0 1.5rem"
          }}>
            Clinical operations, <br />
            <em style={{ fontFamily: "var(--font-serif)", fontStyle: "italic", color: "var(--accent-vermilion)" }}>calibrated</em> to zero latency.
          </h1>

          <p style={{
            fontSize: "1.05rem",
            color: "var(--color-ink-subtle)",
            lineHeight: 1.6,
            maxWidth: "32rem",
            margin: "0 0 2rem"
          }}>
            Deterministic scheduling locks combined with scikit-learn random forest queues. Designed for physicians, operations leads, and patients requiring instant precision.
          </p>

          {/* Quick Metrics Bar */}
          <div style={{
            display: "grid",
            gridTemplateColumns: "repeat(3, 1fr)",
            gap: "1.5rem",
            paddingTop: "2rem",
            borderTop: "1.5px solid var(--color-ink)"
          }}>
            <div>
              <div style={{ fontFamily: "var(--font-display)", fontSize: "1.75rem", fontWeight: 800 }}>&lt; 50ms</div>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.7rem", color: "var(--color-muted)", textTransform: "uppercase" }}>Queue Triage ML</div>
            </div>
            <div>
              <div style={{ fontFamily: "var(--font-display)", fontSize: "1.75rem", fontWeight: 800 }}>100%</div>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.7rem", color: "var(--color-muted)", textTransform: "uppercase" }}>Slot Lock Safety</div>
            </div>
            <div>
              <div style={{ fontFamily: "var(--font-display)", fontSize: "1.75rem", fontWeight: 800 }}>24/7</div>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.7rem", color: "var(--color-muted)", textTransform: "uppercase" }}>Facility Sync</div>
            </div>
          </div>
        </div>

        {/* Right Column: Physical Ticket / Brutalist Access Card */}
        <div style={{
          background: "#ffffff",
          border: "2px solid var(--color-ink)",
          borderRadius: "8px",
          padding: "2.5rem",
          boxShadow: "var(--shadow-hard-md)",
          position: "relative"
        }}>
          {/* Card Eyebrow */}
          <div style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            paddingBottom: "1.25rem",
            marginBottom: "1.5rem",
            borderBottom: "1.5px dashed var(--color-border)"
          }}>
            <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", fontWeight: 700, textTransform: "uppercase" }}>
              ENTRY TERMINAL // SECURE AUTH
            </span>
            <Fingerprint size={18} color="var(--color-ink)" />
          </div>

          {generalError && (
            <div role="alert" style={{
              padding: "0.75rem 1rem",
              marginBottom: "1.25rem",
              border: "1.5px solid var(--color-ink)",
              background: "#fee2e2",
              color: "#991b1b",
              fontSize: "0.85rem",
              fontFamily: "var(--font-mono)",
              fontWeight: 700,
              boxShadow: "2px 2px 0px var(--color-ink)"
            }}>
              ERR: {generalError}
            </div>
          )}

          <form noValidate onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
            <div>
              <label htmlFor="email" style={{
                display: "block",
                fontSize: "0.78rem",
                fontFamily: "var(--font-mono)",
                fontWeight: 700,
                textTransform: "uppercase",
                marginBottom: "0.35rem"
              }}>
                [01] Email Credential
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="physician@smarthospital.local"
                autoComplete="email"
                required
                className="field-control"
              />
              {fieldErrors.email && (
                <span style={{ fontSize: "0.75rem", color: "#dc2626", fontFamily: "var(--font-mono)", marginTop: "0.25rem", display: "block" }}>
                  ! {fieldErrors.email}
                </span>
              )}
            </div>

            <div>
              <label htmlFor="password" style={{
                display: "block",
                fontSize: "0.78rem",
                fontFamily: "var(--font-mono)",
                fontWeight: 700,
                textTransform: "uppercase",
                marginBottom: "0.35rem"
              }}>
                [02] Secret Passphrase
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                autoComplete="current-password"
                required
                className="field-control"
              />
              {fieldErrors.password && (
                <span style={{ fontSize: "0.75rem", color: "#dc2626", fontFamily: "var(--font-mono)", marginTop: "0.25rem", display: "block" }}>
                  ! {fieldErrors.password}
                </span>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="button button-primary"
              style={{
                width: "100%",
                minHeight: "3.25rem",
                fontSize: "0.95rem",
                marginTop: "0.5rem"
              }}
            >
              {loading ? "INITIALIZING SESSION..." : <>AUTHENTICATE & ENTER →</>}
            </button>
          </form>

          {/* Rapid Role Pass Injection */}
          <div style={{
            marginTop: "2rem",
            paddingTop: "1.25rem",
            borderTop: "1.5px dashed var(--color-border)"
          }}>
            <div style={{
              fontSize: "0.7rem",
              fontFamily: "var(--font-mono)",
              fontWeight: 700,
              color: "var(--color-muted)",
              textTransform: "uppercase",
              marginBottom: "0.75rem"
            }}>
              QUICK PASS INJECTION:
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "0.5rem" }}>
              <button
                type="button"
                onClick={() => fillCredentials("admin@smarthospital.local", "Admin@123")}
                style={{
                  background: "var(--bg-canvas)",
                  border: "1.5px solid var(--color-ink)",
                  borderRadius: "4px",
                  padding: "0.6rem 0.25rem",
                  color: "var(--color-ink)",
                  fontSize: "0.75rem",
                  fontFamily: "var(--font-mono)",
                  fontWeight: 700,
                  cursor: "pointer",
                  boxShadow: "2px 2px 0px var(--color-ink)",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "0.25rem"
                }}
              >
                <Shield size={14} color="var(--accent-vermilion)" />
                <span>ADMIN</span>
              </button>

              <button
                type="button"
                onClick={() => fillCredentials("doctor@smarthospital.local", "Doctor@123")}
                style={{
                  background: "var(--bg-canvas)",
                  border: "1.5px solid var(--color-ink)",
                  borderRadius: "4px",
                  padding: "0.6rem 0.25rem",
                  color: "var(--color-ink)",
                  fontSize: "0.75rem",
                  fontFamily: "var(--font-mono)",
                  fontWeight: 700,
                  cursor: "pointer",
                  boxShadow: "2px 2px 0px var(--color-ink)",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "0.25rem"
                }}
              >
                <Stethoscope size={14} color="var(--color-ink)" />
                <span>DOCTOR</span>
              </button>

              <button
                type="button"
                onClick={() => fillCredentials("patient@smarthospital.local", "Patient@123")}
                style={{
                  background: "var(--bg-canvas)",
                  border: "1.5px solid var(--color-ink)",
                  borderRadius: "4px",
                  padding: "0.6rem 0.25rem",
                  color: "var(--color-ink)",
                  fontSize: "0.75rem",
                  fontFamily: "var(--font-mono)",
                  fontWeight: 700,
                  cursor: "pointer",
                  boxShadow: "2px 2px 0px var(--color-ink)",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "0.25rem"
                }}
              >
                <User size={14} color="var(--color-ink)" />
                <span>PATIENT</span>
              </button>
            </div>
          </div>
        </div>
      </main>

      {/* Bottom Technical Footer */}
      <footer style={{
        paddingTop: "1.5rem",
        borderTop: "1.5px solid var(--color-ink)",
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        flexWrap: "wrap",
        gap: "1rem",
        fontSize: "0.72rem",
        fontFamily: "var(--font-mono)",
        color: "var(--color-muted)"
      }}>
        <div>SYSTEM STATUS: NORMAL // ENCRYPTED AES-256 // ZERO-KNOWLEDGE ARCHITECTURE</div>
        <div>AEGLE CLINICAL ENGINE © 2026 • DESIGNED FOR HIGH-PRESSURE BIOMEDICAL WORKFLOWS</div>
      </footer>
    </div>
  );
}
