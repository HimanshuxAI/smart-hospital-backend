import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Activity, ArrowRight, UserPlus, ShieldAlert } from "lucide-react";
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
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      background: "#090d16",
      color: "#f8fafc",
      fontFamily: "var(--font-sans)",
      padding: "2rem 1rem",
      position: "relative",
      overflow: "hidden"
    }}>
      {/* Background ambient lighting */}
      <div style={{
        position: "absolute",
        top: "10%",
        right: "15%",
        width: "45vw",
        height: "45vw",
        borderRadius: "50%",
        background: "radial-gradient(circle, rgba(13, 148, 136, 0.15) 0%, transparent 60%)",
        filter: "blur(60px)",
        pointerEvents: "none"
      }} />

      <div style={{
        width: "100%",
        maxWidth: "520px",
        background: "rgba(15, 23, 42, 0.8)",
        border: "1px solid rgba(255, 255, 255, 0.1)",
        borderRadius: "1.75rem",
        padding: "2.75rem",
        boxShadow: "0 25px 60px -15px rgba(0, 0, 0, 0.6)",
        backdropFilter: "blur(24px)",
        position: "relative",
        zIndex: 2
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", marginBottom: "1.5rem" }}>
          <div style={{
            width: "2.5rem",
            height: "2.5rem",
            borderRadius: "0.75rem",
            background: "linear-gradient(135deg, #0d9488 0%, #0284c7 100%)",
            display: "grid",
            placeItems: "center",
            boxShadow: "0 4px 14px rgba(13, 148, 136, 0.4)"
          }}>
            <UserPlus size={20} color="#ffffff" />
          </div>
          <div>
            <h2 style={{ fontSize: "1.5rem", fontWeight: 800, letterSpacing: "-0.03em", color: "#ffffff", margin: 0 }}>
              Create Patient Profile
            </h2>
            <div style={{ fontSize: "0.75rem", color: "#2dd4bf", fontWeight: 600, letterSpacing: "0.06em", textTransform: "uppercase" }}>
              Instant Self-Registration
            </div>
          </div>
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
            <ShieldAlert size={18} />
            <span>{generalError}</span>
          </div>
        )}

        <form noValidate onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1.1rem" }}>
          <div>
            <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, color: "#cbd5e1", marginBottom: "0.35rem" }}>
              Full Legal Name
            </label>
            <input
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="e.g. Johnathan Doe"
              required
              style={{
                width: "100%",
                minHeight: "2.75rem",
                borderRadius: "0.75rem",
                border: fieldErrors.fullName ? "1.5px solid #e11d48" : "1.5px solid rgba(255, 255, 255, 0.12)",
                background: "rgba(30, 41, 59, 0.6)",
                padding: "0.6rem 1rem",
                color: "#ffffff",
                fontSize: "0.92rem",
                outline: "none"
              }}
            />
            {fieldErrors.fullName && <span style={{ fontSize: "0.78rem", color: "#fb7185" }}>{fieldErrors.fullName}</span>}
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
            <div>
              <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, color: "#cbd5e1", marginBottom: "0.35rem" }}>
                Email Address
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="patient@domain.com"
                required
                style={{
                  width: "100%",
                  minHeight: "2.75rem",
                  borderRadius: "0.75rem",
                  border: fieldErrors.email ? "1.5px solid #e11d48" : "1.5px solid rgba(255, 255, 255, 0.12)",
                  background: "rgba(30, 41, 59, 0.6)",
                  padding: "0.6rem 1rem",
                  color: "#ffffff",
                  fontSize: "0.92rem",
                  outline: "none"
                }}
              />
              {fieldErrors.email && <span style={{ fontSize: "0.78rem", color: "#fb7185" }}>{fieldErrors.email}</span>}
            </div>

            <div>
              <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, color: "#cbd5e1", marginBottom: "0.35rem" }}>
                Contact Phone
              </label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+1 555-0199"
                required
                style={{
                  width: "100%",
                  minHeight: "2.75rem",
                  borderRadius: "0.75rem",
                  border: fieldErrors.phone ? "1.5px solid #e11d48" : "1.5px solid rgba(255, 255, 255, 0.12)",
                  background: "rgba(30, 41, 59, 0.6)",
                  padding: "0.6rem 1rem",
                  color: "#ffffff",
                  fontSize: "0.92rem",
                  outline: "none"
                }}
              />
              {fieldErrors.phone && <span style={{ fontSize: "0.78rem", color: "#fb7185" }}>{fieldErrors.phone}</span>}
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
            <div>
              <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, color: "#cbd5e1", marginBottom: "0.35rem" }}>
                Date of Birth
              </label>
              <input
                type="date"
                value={dateOfBirth}
                onChange={(e) => setDateOfBirth(e.target.value)}
                required
                style={{
                  width: "100%",
                  minHeight: "2.75rem",
                  borderRadius: "0.75rem",
                  border: fieldErrors.dateOfBirth ? "1.5px solid #e11d48" : "1.5px solid rgba(255, 255, 255, 0.12)",
                  background: "rgba(30, 41, 59, 0.6)",
                  padding: "0.6rem 1rem",
                  color: "#ffffff",
                  fontSize: "0.92rem",
                  outline: "none"
                }}
              />
              {fieldErrors.dateOfBirth && <span style={{ fontSize: "0.78rem", color: "#fb7185" }}>{fieldErrors.dateOfBirth}</span>}
            </div>

            <div>
              <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, color: "#cbd5e1", marginBottom: "0.35rem" }}>
                Password (min 8 chars)
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="StrongPassword@123"
                required
                style={{
                  width: "100%",
                  minHeight: "2.75rem",
                  borderRadius: "0.75rem",
                  border: fieldErrors.password ? "1.5px solid #e11d48" : "1.5px solid rgba(255, 255, 255, 0.12)",
                  background: "rgba(30, 41, 59, 0.6)",
                  padding: "0.6rem 1rem",
                  color: "#ffffff",
                  fontSize: "0.92rem",
                  outline: "none"
                }}
              />
              {fieldErrors.password && <span style={{ fontSize: "0.78rem", color: "#fb7185" }}>{fieldErrors.password}</span>}
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: "100%",
              minHeight: "3rem",
              marginTop: "1rem",
              border: "none",
              borderRadius: "0.75rem",
              background: "linear-gradient(135deg, #0d9488 0%, #0284c7 100%)",
              color: "#ffffff",
              fontSize: "0.95rem",
              fontWeight: 700,
              cursor: loading ? "not-allowed" : "pointer",
              boxShadow: "0 6px 20px rgba(13, 148, 136, 0.35)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: "0.5rem"
            }}
          >
            {loading ? "Registering..." : <>Complete Registration <ArrowRight size={18} /></>}
          </button>
        </form>

        <div style={{ marginTop: "1.75rem", textAlign: "center", fontSize: "0.85rem", color: "#94a3b8" }}>
          Already have an account?{" "}
          <Link to="/login" style={{ color: "#38bdf8", fontWeight: 700, textDecoration: "none" }}>
            Return to Sign In
          </Link>
        </div>
      </div>
    </div>
  );
}
