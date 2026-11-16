import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, Fingerprint, ShieldAlert, ArrowLeft } from "lucide-react";
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
      const dest = user.role === "PATIENT" ? "/patient" : user.role === "DOCTOR" ? "/doctor" : "/admin";
      navigate(dest, { replace: true });
    } catch (err: unknown) {
      const msg = err && typeof err === "object" && "detail" in err ? String((err as { detail: unknown }).detail) : "Registration failed";
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
      alignItems: "center",
      justifyContent: "center",
      padding: "2rem 1.5rem",
      fontFamily: "var(--font-body)"
    }}>
      <div style={{
        width: "100%",
        maxWidth: "540px",
        background: "#ffffff",
        border: "2px solid var(--color-ink)",
        borderRadius: "8px",
        padding: "2.75rem",
        boxShadow: "var(--shadow-hard-md)",
        position: "relative"
      }}>
        <div style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          paddingBottom: "1.25rem",
          marginBottom: "1.75rem",
          borderBottom: "1.5px dashed var(--color-border)"
        }}>
          <div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--accent-vermilion)", fontWeight: 700, textTransform: "uppercase" }}>
              [ FORM 104-A // PATIENT INTAKE ]
            </div>
            <h1 style={{ fontFamily: "var(--font-serif)", fontSize: "2rem", margin: "0.25rem 0 0", color: "var(--color-ink)", lineHeight: 1.1 }}>
              Register Identity
            </h1>
          </div>
          <Fingerprint size={28} color="var(--color-ink)" />
        </div>

        {generalError && (
          <div role="alert" style={{
            padding: "0.75rem 1rem",
            marginBottom: "1.5rem",
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

        <form noValidate onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1.15rem" }}>
          <div>
            <label style={{ display: "block", fontSize: "0.78rem", fontFamily: "var(--font-mono)", fontWeight: 700, textTransform: "uppercase", marginBottom: "0.35rem" }}>
              Full Legal Name
            </label>
            <input
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="e.g. Eleanor Vance"
              required
              className="field-control"
            />
            {fieldErrors.fullName && <span style={{ fontSize: "0.75rem", color: "#dc2626", fontFamily: "var(--font-mono)" }}>! {fieldErrors.fullName}</span>}
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
            <div>
              <label style={{ display: "block", fontSize: "0.78rem", fontFamily: "var(--font-mono)", fontWeight: 700, textTransform: "uppercase", marginBottom: "0.35rem" }}>
                Email Address
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="patient@gmail.com"
                required
                className="field-control"
              />
              {fieldErrors.email && <span style={{ fontSize: "0.75rem", color: "#dc2626", fontFamily: "var(--font-mono)" }}>! {fieldErrors.email}</span>}
            </div>

            <div>
              <label style={{ display: "block", fontSize: "0.78rem", fontFamily: "var(--font-mono)", fontWeight: 700, textTransform: "uppercase", marginBottom: "0.35rem" }}>
                Phone Number
              </label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+1 555-0199"
                required
                className="field-control"
              />
              {fieldErrors.phone && <span style={{ fontSize: "0.75rem", color: "#dc2626", fontFamily: "var(--font-mono)" }}>! {fieldErrors.phone}</span>}
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
            <div>
              <label style={{ display: "block", fontSize: "0.78rem", fontFamily: "var(--font-mono)", fontWeight: 700, textTransform: "uppercase", marginBottom: "0.35rem" }}>
                Birth Date
              </label>
              <input
                type="date"
                value={dateOfBirth}
                onChange={(e) => setDateOfBirth(e.target.value)}
                required
                className="field-control"
              />
              {fieldErrors.dateOfBirth && <span style={{ fontSize: "0.75rem", color: "#dc2626", fontFamily: "var(--font-mono)" }}>! {fieldErrors.dateOfBirth}</span>}
            </div>

            <div>
              <label style={{ display: "block", fontSize: "0.78rem", fontFamily: "var(--font-mono)", fontWeight: 700, textTransform: "uppercase", marginBottom: "0.35rem" }}>
                Passphrase (8+ chars)
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                required
                className="field-control"
              />
              {fieldErrors.password && <span style={{ fontSize: "0.75rem", color: "#dc2626", fontFamily: "var(--font-mono)" }}>! {fieldErrors.password}</span>}
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="button button-primary"
            style={{ width: "100%", minHeight: "3.25rem", fontSize: "0.95rem", marginTop: "0.75rem" }}
          >
            {loading ? "ENROLLING PROFILE..." : <>CREATE PROFILE & ENTER →</>}
          </button>
        </form>

        <div style={{
          marginTop: "2rem",
          paddingTop: "1.25rem",
          borderTop: "1.5px dashed var(--color-border)",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center"
        }}>
          <Link
            to="/login"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.4rem",
              fontSize: "0.8rem",
              fontFamily: "var(--font-mono)",
              fontWeight: 700,
              color: "var(--color-ink)",
              textDecoration: "none"
            }}
          >
            <ArrowLeft size={15} /> Return to Terminal
          </Link>
          <span style={{ fontSize: "0.72rem", fontFamily: "var(--font-mono)", color: "var(--color-muted)" }}>
            IDENTITY VERIFIED VIA SHA-256
          </span>
        </div>
      </div>
    </div>
  );
}
