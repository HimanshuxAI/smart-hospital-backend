import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowLeft, ArrowRight, HeartPulse, ShieldCheck } from "lucide-react";

import { useAuth } from "../../auth/auth-context";
import { validateEmail, validatePassword, validateRequired } from "../../lib/validation";

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [phone, setPhone] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [loading, setLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setGeneralError(null);
    const errors: Record<string, string> = {};

    const nameErr = validateRequired(fullName, "Full name");
    if (nameErr) errors.fullName = nameErr;

    const emailErr = validateEmail(email);
    if (emailErr) errors.email = emailErr;

    const passErr = validatePassword(password);
    if (passErr) errors.password = passErr;

    const phoneErr = validateRequired(phone, "Phone");
    if (phoneErr) errors.phone = phoneErr;

    const dobErr = validateRequired(dateOfBirth, "Date of birth");
    if (dobErr) errors.dateOfBirth = dobErr;

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setFieldErrors({});
    setLoading(true);

    try {
      const user = await register({
        fullName,
        email,
        password,
        phone,
        dateOfBirth,
      });
      const dest =
        user.role === "PATIENT" ? "/patient" : user.role === "DOCTOR" ? "/doctor" : "/admin";
      navigate(dest, { replace: true });
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "detail" in err
          ? String((err as { detail: unknown }).detail)
          : "Registration failed";
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

        <Link to="/login" className="auth-toplink">
          <ArrowLeft size={15} aria-hidden="true" />
          <span>Back to sign-in</span>
        </Link>
      </header>

      <main className="auth-main auth-centered">
        <section className="auth-card auth-card-wide" aria-labelledby="register-heading">
          <div className="auth-card-head">
            <span className="page-eyebrow">Patient intake</span>
            <h2 id="register-heading">Create your profile.</h2>
            <p>A few details and you are ready to book your first visit.</p>
          </div>

          {generalError && (
            <div role="alert" className="auth-error" style={{ marginTop: "1.25rem" }}>
              <ShieldCheck size={16} style={{ flex: "0 0 auto", marginTop: "1px" }} />
              <span>{generalError}</span>
            </div>
          )}

          <form noValidate onSubmit={handleSubmit} className="auth-form">
            <div className="field">
              <label htmlFor="fullName" className="field-label">
                Full name
              </label>
              <input
                id="fullName"
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="e.g. Eleanor Vance"
                required
                className={`field-control${fieldErrors.fullName ? " field-invalid" : ""}`}
              />
              {fieldErrors.fullName && <span className="field-error">{fieldErrors.fullName}</span>}
            </div>

            <div className="auth-grid-2">
              <div className="field">
                <label htmlFor="email" className="field-label">
                  Email address
                </label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  required
                  className={`field-control${fieldErrors.email ? " field-invalid" : ""}`}
                />
                {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
              </div>

              <div className="field">
                <label htmlFor="phone" className="field-label">
                  Phone number
                </label>
                <input
                  id="phone"
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+1 555 0199"
                  required
                  className={`field-control${fieldErrors.phone ? " field-invalid" : ""}`}
                />
                {fieldErrors.phone && <span className="field-error">{fieldErrors.phone}</span>}
              </div>
            </div>

            <div className="auth-grid-2">
              <div className="field">
                <label htmlFor="dateOfBirth" className="field-label">
                  Date of birth
                </label>
                <input
                  id="dateOfBirth"
                  type="date"
                  value={dateOfBirth}
                  onChange={(e) => setDateOfBirth(e.target.value)}
                  required
                  className={`field-control${fieldErrors.dateOfBirth ? " field-invalid" : ""}`}
                />
                {fieldErrors.dateOfBirth && (
                  <span className="field-error">{fieldErrors.dateOfBirth}</span>
                )}
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
                  placeholder="8+ characters"
                  required
                  className={`field-control${fieldErrors.password ? " field-invalid" : ""}`}
                />
                {fieldErrors.password && (
                  <span className="field-error">{fieldErrors.password}</span>
                )}
              </div>
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
                  Creating profile…
                </>
              ) : (
                <>
                  Create profile
                  <ArrowRight size={17} aria-hidden="true" />
                </>
              )}
            </button>
          </form>

          <p className="auth-note">
            By creating a profile you agree to receive appointment updates from your care team.
          </p>
        </section>
      </main>

      <footer className="auth-footer">
        <span>Aegle Clinical OS · 2026</span>
        <span>Patient intake</span>
        <span>TLS 1.3 · HS256 sessions</span>
      </footer>
    </div>
  );
}
