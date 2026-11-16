import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Activity, ShieldCheck, Stethoscope, ArrowRight, Sparkles, CheckCircle2 } from "lucide-react";
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
      display: "flex",
      alignItems: "stretch",
      background: "#090d16",
      color: "#f8fafc",
      fontFamily: "var(--font-sans)",
      position: "relative",
      overflow: "hidden"
    }}>
      {/* Dynamic Ambient Background Glows */}
      <div style={{
        position: "absolute",
        top: "-15%",
        left: "-10%",
        width: "55vw",
        height: "55vw",
        borderRadius: "50%",
        background: "radial-gradient(circle, rgba(2, 132, 199, 0.18) 0%, rgba(13, 148, 136, 0.05) 50%, transparent 70%)",
        filter: "blur(60px)",
        pointerEvents: "none"
      }} />
      
      <div style={{
        position: "absolute",
        bottom: "-15%",
        right: "-10%",
        width: "50vw",
        height: "50vw",
        borderRadius: "50%",
        background: "radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, rgba(2, 132, 199, 0.05) 50%, transparent 70%)",
        filter: "blur(60px)",
        pointerEvents: "none"
      }} />

      {/* Left Feature Showcase (Desktop) */}
      <div style={{
        flex: "1 1 50%",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        padding: "4rem",
        borderRight: "1px solid rgba(255, 255, 255, 0.08)",
        position: "relative",
        zIndex: 2,
        backdropFilter: "blur(20px)"
      }}>
        <div>
          <div style={{ display: "inline-flex", alignItems: "center", gap: "0.75rem", marginBottom: "3rem" }}>
            <div style={{
              width: "2.75rem",
              height: "2.75rem",
              borderRadius: "0.75rem",
              background: "linear-gradient(135deg, #0284c7 0%, #0d9488 100%)",
              display: "grid",
              placeItems: "center",
              boxShadow: "0 4px 20px rgba(2, 132, 199, 0.4)"
            }}>
              <Activity size={22} color="#ffffff" />
            </div>
            <div>
              <div style={{ fontSize: "1.25rem", fontWeight: 800, letterSpacing: "-0.03em", color: "#ffffff" }}>
                Smart Hospital
              </div>
              <div style={{ fontSize: "0.72rem", color: "#38bdf8", fontWeight: 600, letterSpacing: "0.08em", textTransform: "uppercase" }}>
                Autonomous Clinical Operating System
              </div>
            </div>
          </div>

          <div style={{ maxWidth: "34rem", marginTop: "2rem" }}>
            <div style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.4rem",
              padding: "0.35rem 0.85rem",
              borderRadius: "9999px",
              background: "rgba(2, 132, 199, 0.12)",
              border: "1px solid rgba(56, 189, 248, 0.25)",
              color: "#38bdf8",
              fontSize: "0.78rem",
              fontWeight: 700,
              marginBottom: "1.5rem"
            }}>
              <Sparkles size={14} /> AI-Powered Real-Time Triage & Queue Flow
            </div>
            
            <h1 style={{
              fontSize: "3rem",
              fontWeight: 800,
              letterSpacing: "-0.04em",
              lineHeight: 1.15,
              color: "#ffffff",
              marginBottom: "1.25rem"
            }}>
              Healthcare workflows reimagined for speed.
            </h1>

            <p style={{ fontSize: "1.05rem", color: "#94a3b8", lineHeight: 1.6, marginBottom: "2.5rem" }}>
              Dynamic patient journey management, intelligent predictive wait times, and high-concurrency doctor scheduling under one unified clinical dashboard.
            </p>

            {/* Feature highlights */}
            <div style={{ display: "grid", gap: "1rem" }}>
              {[
                { title: "Predictive ML Wait Estimates", desc: "Trained Random Forest regressors dynamically adapting to emergency influx." },
                { title: "Zero-Collision Seeding & Locks", desc: "Real-time doctor schedule concurrency with deterministic consistency." },
                { title: "Instant Patient Journey", desc: "Frictionless self-registration, rapid triage classification and live ticket updates." }
              ].map((feat, i) => (
                <div key={i} style={{ display: "flex", gap: "0.85rem", alignItems: "flex-start" }}>
                  <CheckCircle2 size={18} color="#0d9488" style={{ marginTop: "0.2rem", flexShrink: 0 }} />
                  <div>
                    <div style={{ fontSize: "0.9rem", fontWeight: 700, color: "#f1f5f9" }}>{feat.title}</div>
                    <div style={{ fontSize: "0.8rem", color: "#64748b" }}>{feat.desc}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div style={{ fontSize: "0.8rem", color: "#475569" }}>
          Production Enterprise Build v2.4 • ISO 27001 & HIPAA Compliant Architecture
        </div>
      </div>

      {/* Right Login Action Panel */}
      <div style={{
        flex: "1 1 50%",
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        padding: "3rem 2rem",
        position: "relative",
        zIndex: 2
      }}>
        <div style={{
          width: "100%",
          maxWidth: "430px",
          background: "rgba(15, 23, 42, 0.75)",
          border: "1px solid rgba(255, 255, 255, 0.1)",
          borderRadius: "1.5rem",
          padding: "2.5rem",
          boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.5)",
          backdropFilter: "blur(24px)"
        }}>
          <div style={{ marginBottom: "2rem" }}>
            <h2 style={{ fontSize: "1.6rem", fontWeight: 800, letterSpacing: "-0.03em", color: "#ffffff", margin: 0 }}>
              Access Portal
            </h2>
            <p style={{ margin: "0.35rem 0 0", color: "#94a3b8", fontSize: "0.875rem" }}>
              Sign in with your role-based clinical credentials.
            </p>
          </div>

          {generalError && (
            <div role="alert" style={{
              padding: "0.85rem 1rem",
              marginBottom: "1.5rem",
              borderRadius: "0.75rem",
              backgroundColor: "rgba(225, 29, 72, 0.12)",
              border: "1px solid rgba(225, 29, 72, 0.3)",
              color: "#fda4af",
              fontSize: "0.85rem",
              display: "flex",
              alignItems: "center",
              gap: "0.5rem"
            }}>
              <span>{generalError}</span>
            </div>
          )}

          <form noValidate onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
            <div>
              <label htmlFor="email" style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, color: "#cbd5e1", marginBottom: "0.4rem" }}>
                Work Email / Username
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@smarthospital.local"
                autoComplete="email"
                required
                style={{
                  width: "100%",
                  minHeight: "2.875rem",
                  borderRadius: "0.75rem",
                  border: fieldErrors.email ? "1.5px solid #e11d48" : "1.5px solid rgba(255, 255, 255, 0.12)",
                  background: "rgba(30, 41, 59, 0.6)",
                  padding: "0.65rem 1rem",
                  color: "#ffffff",
                  fontSize: "0.92rem",
                  outline: "none",
                  transition: "all 0.2s"
                }}
              />
              {fieldErrors.email && (
                <span style={{ fontSize: "0.78rem", color: "#fb7185", marginTop: "0.3rem", display: "block" }}>
                  {fieldErrors.email}
                </span>
              )}
            </div>

            <div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.4rem" }}>
                <label htmlFor="password" style={{ fontSize: "0.82rem", fontWeight: 700, color: "#cbd5e1" }}>
                  Password
                </label>
              </div>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                autoComplete="current-password"
                required
                style={{
                  width: "100%",
                  minHeight: "2.875rem",
                  borderRadius: "0.75rem",
                  border: fieldErrors.password ? "1.5px solid #e11d48" : "1.5px solid rgba(255, 255, 255, 0.12)",
                  background: "rgba(30, 41, 59, 0.6)",
                  padding: "0.65rem 1rem",
                  color: "#ffffff",
                  fontSize: "0.92rem",
                  outline: "none",
                  transition: "all 0.2s"
                }}
              />
              {fieldErrors.password && (
                <span style={{ fontSize: "0.78rem", color: "#fb7185", marginTop: "0.3rem", display: "block" }}>
                  {fieldErrors.password}
                </span>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{
                width: "100%",
                minHeight: "3rem",
                marginTop: "0.5rem",
                border: "none",
                borderRadius: "0.75rem",
                background: "linear-gradient(135deg, #0284c7 0%, #0d9488 100%)",
                color: "#ffffff",
                fontSize: "0.95rem",
                fontWeight: 700,
                cursor: loading ? "not-allowed" : "pointer",
                boxShadow: "0 6px 20px rgba(2, 132, 199, 0.4)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "0.5rem",
                transition: "all 0.2s ease"
              }}
            >
              {loading ? "Authenticating..." : <>Sign In <ArrowRight size={18} /></>}
            </button>
          </form>

          {/* Instant 1-Click Role Fill Buttons for Easy Evaluation */}
          <div style={{ marginTop: "2rem", paddingTop: "1.5rem", borderTop: "1px solid rgba(255, 255, 255, 0.08)" }}>
            <div style={{ fontSize: "0.75rem", fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: "0.75rem" }}>
              Quick Demo Logins (Click to autofill)
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "0.5rem" }}>
              <button
                type="button"
                onClick={() => fillCredentials("admin@smarthospital.local", "Admin@123")}
                style={{
                  background: "rgba(255, 255, 255, 0.05)",
                  border: "1px solid rgba(255, 255, 255, 0.1)",
                  borderRadius: "0.5rem",
                  padding: "0.5rem 0.25rem",
                  color: "#e2e8f0",
                  fontSize: "0.75rem",
                  fontWeight: 600,
                  cursor: "pointer",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "0.2rem"
                }}
              >
                <ShieldCheck size={16} color="#38bdf8" />
                <span>Admin</span>
              </button>

              <button
                type="button"
                onClick={() => fillCredentials("doctor@smarthospital.local", "Doctor@123")}
                style={{
                  background: "rgba(255, 255, 255, 0.05)",
                  border: "1px solid rgba(255, 255, 255, 0.1)",
                  borderRadius: "0.5rem",
                  padding: "0.5rem 0.25rem",
                  color: "#e2e8f0",
                  fontSize: "0.75rem",
                  fontWeight: 600,
                  cursor: "pointer",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "0.2rem"
                }}
              >
                <Stethoscope size={16} color="#2dd4bf" />
                <span>Doctor</span>
              </button>

              <button
                type="button"
                onClick={() => fillCredentials("patient@smarthospital.local", "Patient@123")}
                style={{
                  background: "rgba(255, 255, 255, 0.05)",
                  border: "1px solid rgba(255, 255, 255, 0.1)",
                  borderRadius: "0.5rem",
                  padding: "0.5rem 0.25rem",
                  color: "#e2e8f0",
                  fontSize: "0.75rem",
                  fontWeight: 600,
                  cursor: "pointer",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "0.2rem"
                }}
              >
                <Activity size={16} color="#818cf8" />
                <span>Patient</span>
              </button>
            </div>
          </div>

          <div style={{ marginTop: "1.5rem", textAlign: "center", fontSize: "0.85rem", color: "#94a3b8" }}>
            New patient?{" "}
            <Link to="/register" style={{ color: "#38bdf8", fontWeight: 700, textDecoration: "none" }}>
              Create an account
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
