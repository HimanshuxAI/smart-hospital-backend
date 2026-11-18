import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  CalendarClock,
  HeartPulse,
  ShieldCheck,
  Sparkles,
  Stethoscope,
  Timer,
  UserRound,
  Shield,
} from "lucide-react";

import { useAuth } from "../../auth/auth-context";
import { validateEmail, validatePassword } from "../../lib/validation";

const demoRoles = [
  { key: "admin", label: "Admin", icon: Shield, email: "admin@smarthospital.local", password: "Admin@123" },
  { key: "doctor", label: "Doctor", icon: Stethoscope, email: "doctor@smarthospital.local", password: "Doctor@123" },
  { key: "patient", label: "Patient", icon: UserRound, email: "patient@smarthospital.local", password: "Patient@123" },
] as const;

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
        const dest =
          user.role === "PATIENT" ? "/patient" : user.role === "DOCTOR" ? "/doctor" : "/admin";
        navigate(dest, { replace: true });
      }
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "detail" in err
          ? String((err as { detail: unknown }).detail)
          : err instanceof Error
            ? err.message
            : "Invalid email or password";
      setGeneralError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <header className="auth-topbar">
        <div className="auth-brand">
          <span className="auth-mark" aria-hidden="true">
            <HeartPulse size={20} />
          </span>
          <div>
            <strong>Aegle</strong>
            <small>Clinical OS</small>
          </div>
        </div>

        <Link to="/register" className="auth-toplink">
          <span>New patient?</span>
          <span style={{ color: "var(--color-primary)", fontWeight: 700 }}>Create account</span>
          <ArrowRight size={15} aria-hidden="true" />
        </Link>
      </header>

      <main className="auth-main">
        <section className="auth-editorial" aria-hidden="true">
          <span className="auth-kicker">
            <span className="auth-kicker-dot" />
            Live care coordination
          </span>

          <h1>
            Care that moves at the <em>speed</em> of need.
          </h1>

          <p className="auth-lede">
            One calm workspace for appointments, live queues and triage — built so clinicians can
            stay with the person in front of them, not the screen.
          </p>

          <div className="auth-features">
            <div className="auth-feature">
              <span className="auth-feature-icon">
                <CalendarClock size={17} />
              </span>
              <div>
                <strong>Book without collisions</strong>
                <span>Deterministic slot locking keeps every appointment honest.</span>
              </div>
            </div>

            <div className="auth-feature">
              <span className="auth-feature-icon">
                <Timer size={17} />
              </span>
              <div>
                <strong>Watch the queue breathe</strong>
                <span>Live positions and wait estimates update as care happens.</span>
              </div>
            </div>

            <div className="auth-feature">
              <span className="auth-feature-icon">
                <Sparkles size={17} />
              </span>
              <div>
                <strong>Triage that thinks ahead</strong>
                <span>A trained model ranks urgency so the sickest are seen first.</span>
              </div>
            </div>
          </div>

          <svg
            className="auth-pulse"
            viewBox="0 0 420 56"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <path
              d="M0 30H96l10-18 14 34 12-26 10 10h38l8-14 12 24 10-10h210"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </section>

        <section className="auth-card" aria-labelledby="signin-heading">
          <div className="auth-card-head">
            <span className="page-eyebrow">Secure sign-in</span>
            <h2 id="signin-heading">Welcome back.</h2>
            <p>Pick up exactly where your day left off.</p>
          </div>

          {generalError && (
            <div role="alert" className="auth-error" style={{ marginTop: "1.25rem" }}>
              <ShieldCheck size={16} style={{ flex: "0 0 auto", marginTop: "1px" }} />
              <span>{generalError}</span>
            </div>
          )}

          <form noValidate onSubmit={handleSubmit} className="auth-form">
            <div className="field">
              <label htmlFor="email" className="field-label">
                Email address
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@smarthospital.local"
                autoComplete="email"
                required
                className={`field-control${fieldErrors.email ? " field-invalid" : ""}`}
              />
              {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
            </div>

            <div className="field">
              <label htmlFor="password" className="field-label">
                Password
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                autoComplete="current-password"
                required
                className={`field-control${fieldErrors.password ? " field-invalid" : ""}`}
              />
              {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="button button-primary"
              style={{ width: "100%", minHeight: "3rem", marginTop: "0.35rem" }}
            >
              {loading ? (
                <>
                  <span className="button-spinner" aria-hidden="true" />
                  Signing in…
                </>
              ) : (
                <>
                  Sign in to workspace
                  <ArrowRight size={17} aria-hidden="true" />
                </>
              )}
            </button>
          </form>

          <div className="auth-divider">or use a demo pass</div>

          <div className="auth-roles">
            {demoRoles.map((role) => {
              const Icon = role.icon;
              return (
                <button
                  key={role.key}
                  type="button"
                  className="auth-role"
                  title={`${role.email} / ${role.password}`}
                  onClick={() => fillCredentials(role.email, role.password)}
                >
                  <Icon size={16} aria-hidden="true" />
                  <span>{role.label}</span>
                </button>
              );
            })}
          </div>

          <p className="auth-note">
            Demo passes fill the form so you can review each role. Real accounts use the
            <span style={{ color: "var(--color-ink)", fontWeight: 600 }}> @smarthospital.local </span>
            domain.
          </p>
        </section>
      </main>

      <footer className="auth-footer">
        <span>Aegle Clinical OS · 2026</span>
        <span>Appointments · Queue · Triage</span>
        <span>TLS 1.3 · HS256 sessions</span>
      </footer>
    </div>
  );
}
