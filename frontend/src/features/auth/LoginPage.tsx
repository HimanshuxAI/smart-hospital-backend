import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { HeartPulse, ArrowRight, ShieldCheck, Stethoscope, User } from "lucide-react";
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
      background: "var(--bg-page)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      padding: "2rem 1rem"
    }}>
      <div style={{
        width: "100%",
        maxWidth: "400px",
        background: "var(--bg-surface)",
        border: "1px solid var(--border-color)",
        borderRadius: "var(--radius-lg)",
        padding: "2.25rem 2rem",
        boxShadow: "var(--shadow-card)"
      }}>
        {/* Brand header */}
        <div style={{ textAlign: "center", marginBottom: "1.75rem" }}>
          <div style={{
            width: "2.75rem",
            height: "2.75rem",
            margin: "0 auto 0.75rem",
            borderRadius: "var(--radius-md)",
            background: "linear-gradient(135deg, #0284c7, #0ea5e9)",
            color: "white",
            display: "flex",
            alignItems: "center",
            justifyContent: "center"
          }}>
            <HeartPulse size={22} />
          </div>
          <h1 style={{ fontSize: "1.35rem", fontWeight: 700, color: "var(--text-main)" }}>
            Smart Hospital
          </h1>
          <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", marginTop: "0.2rem" }}>
            Sign in to access your clinical dashboard
          </p>
        </div>

        {generalError && (
          <div role="alert" style={{
            padding: "0.75rem",
            marginBottom: "1.25rem",
            borderRadius: "var(--radius-sm)",
            background: "var(--badge-red-bg)",
            border: "1px solid var(--badge-red-border)",
            color: "var(--badge-red-text)",
            fontSize: "0.85rem"
          }}>
            {generalError}
          </div>
        )}

        <form noValidate onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <div>
            <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "0.3rem" }}>
              Email address
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="name@smarthospital.local"
              autoComplete="email"
              required
              className="field-control"
            />
            {fieldErrors.email && (
              <span style={{ fontSize: "0.75rem", color: "#e11d48", marginTop: "0.25rem", display: "block" }}>
                {fieldErrors.email}
              </span>
            )}
          </div>

          <div>
            <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "0.3rem" }}>
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••••••"
              autoComplete="current-password"
              required
              className="field-control"
            />
            {fieldErrors.password && (
              <span style={{ fontSize: "0.75rem", color: "#e11d48", marginTop: "0.25rem", display: "block" }}>
                {fieldErrors.password}
              </span>
            )}
          </div>

          <button
            type="submit"
            disabled={loading}
            className="button button-primary"
            style={{ width: "100%", height: "2.75rem", marginTop: "0.5rem" }}
          >
            {loading ? "Signing in..." : <>Sign In <ArrowRight size={16} /></>}
          </button>
        </form>

        {/* Demo Fast Autofill */}
        <div style={{ marginTop: "1.75rem", paddingTop: "1.25rem", borderTop: "1px solid var(--border-color)" }}>
          <div style={{ fontSize: "0.7rem", fontWeight: 600, color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.04em", marginBottom: "0.6rem", textAlign: "center" }}>
            Quick Demo Accounts
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "0.4rem" }}>
            <button
              type="button"
              onClick={() => fillCredentials("admin@smarthospital.local", "Admin@123")}
              style={{
                background: "var(--bg-subtle)",
                border: "1px solid var(--border-color)",
                borderRadius: "var(--radius-sm)",
                padding: "0.5rem 0.2rem",
                color: "var(--text-main)",
                fontSize: "0.75rem",
                fontWeight: 600,
                cursor: "pointer",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: "0.2rem"
              }}
            >
              <ShieldCheck size={15} color="var(--primary)" />
              <span>Admin</span>
            </button>

            <button
              type="button"
              onClick={() => fillCredentials("doctor@smarthospital.local", "Doctor@123")}
              style={{
                background: "var(--bg-subtle)",
                border: "1px solid var(--border-color)",
                borderRadius: "var(--radius-sm)",
                padding: "0.5rem 0.2rem",
                color: "var(--text-main)",
                fontSize: "0.75rem",
                fontWeight: 600,
                cursor: "pointer",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: "0.2rem"
              }}
            >
              <Stethoscope size={15} color="#16a34a" />
              <span>Doctor</span>
            </button>

            <button
              type="button"
              onClick={() => fillCredentials("patient@smarthospital.local", "Patient@123")}
              style={{
                background: "var(--bg-subtle)",
                border: "1px solid var(--border-color)",
                borderRadius: "var(--radius-sm)",
                padding: "0.5rem 0.2rem",
                color: "var(--text-main)",
                fontSize: "0.75rem",
                fontWeight: 600,
                cursor: "pointer",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: "0.2rem"
              }}
            >
              <User size={15} color="#d97706" />
              <span>Patient</span>
            </button>
          </div>
        </div>

        <div style={{ marginTop: "1.25rem", textAlign: "center", fontSize: "0.85rem", color: "var(--text-muted)" }}>
          Don't have an account?{" "}
          <Link to="/register" style={{ color: "var(--primary)", fontWeight: 600, textDecoration: "none" }}>
            Register
          </Link>
        </div>
      </div>
    </div>
  );
}
