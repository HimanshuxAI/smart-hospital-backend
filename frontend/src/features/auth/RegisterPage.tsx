import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { HeartPulse, ArrowRight, ArrowLeft } from "lucide-react";
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
      background: "var(--bg-page)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      padding: "2rem 1rem"
    }}>
      <div style={{
        width: "100%",
        maxWidth: "480px",
        background: "var(--bg-surface)",
        border: "1px solid var(--border-color)",
        borderRadius: "var(--radius-lg)",
        padding: "2.25rem 2rem",
        boxShadow: "var(--shadow-card)"
      }}>
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
            Create Patient Account
          </h1>
          <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", marginTop: "0.2rem" }}>
            Sign up to book and manage medical appointments
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
              Full name
            </label>
            <input
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="e.g. John Doe"
              required
              className="field-control"
            />
            {fieldErrors.fullName && <span style={{ fontSize: "0.75rem", color: "#e11d48" }}>{fieldErrors.fullName}</span>}
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.75rem" }}>
            <div>
              <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "0.3rem" }}>
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="john@example.com"
                required
                className="field-control"
              />
              {fieldErrors.email && <span style={{ fontSize: "0.75rem", color: "#e11d48" }}>{fieldErrors.email}</span>}
            </div>

            <div>
              <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "0.3rem" }}>
                Phone
              </label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+1 555-0199"
                required
                className="field-control"
              />
              {fieldErrors.phone && <span style={{ fontSize: "0.75rem", color: "#e11d48" }}>{fieldErrors.phone}</span>}
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.75rem" }}>
            <div>
              <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "0.3rem" }}>
                Birth date
              </label>
              <input
                type="date"
                value={dateOfBirth}
                onChange={(e) => setDateOfBirth(e.target.value)}
                required
                className="field-control"
              />
              {fieldErrors.dateOfBirth && <span style={{ fontSize: "0.75rem", color: "#e11d48" }}>{fieldErrors.dateOfBirth}</span>}
            </div>

            <div>
              <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "0.3rem" }}>
                Password (min 8)
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                required
                className="field-control"
              />
              {fieldErrors.password && <span style={{ fontSize: "0.75rem", color: "#e11d48" }}>{fieldErrors.password}</span>}
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="button button-primary"
            style={{ width: "100%", height: "2.75rem", marginTop: "0.5rem" }}
          >
            {loading ? "Registering..." : <>Register Account <ArrowRight size={16} /></>}
          </button>
        </form>

        <div style={{ marginTop: "1.25rem", textAlign: "center", fontSize: "0.85rem", color: "var(--text-muted)" }}>
          Already have an account?{" "}
          <Link to="/login" style={{ color: "var(--primary)", fontWeight: 600, textDecoration: "none" }}>
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
}
