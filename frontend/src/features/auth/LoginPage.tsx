import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/auth-context";
import { Button } from "../../components/ui/button";
import { Card } from "../../components/ui/card";
import { TextField } from "../../components/ui/field";
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

  const isDemo = import.meta.env.VITE_API_MODE === "demo";

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
    <div style={{ display: "flex", minHeight: "80vh", alignItems: "center", justifyContent: "center", padding: "1rem" }}>
      <Card title="Sign In" style={{ width: "100%", maxWidth: "400px" }}>
        {generalError && (
          <div role="alert" style={{ padding: "0.75rem", marginBottom: "1rem", borderRadius: "4px", backgroundColor: "#fee2e2", color: "#991b1b", fontSize: "0.875rem" }}>
            {generalError}
          </div>
        )}
        <form noValidate onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <TextField
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={fieldErrors.email}
            autoComplete="email"
            required
          />
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={fieldErrors.password}
            autoComplete="current-password"
            required
          />
          <Button type="submit" loading={loading} loadingLabel="Signing in..." style={{ width: "100%", marginTop: "0.5rem" }}>
            Sign In
          </Button>
        </form>
        {isDemo && (
          <div style={{ marginTop: "1rem", padding: "0.75rem", backgroundColor: "#f1f5f9", borderRadius: "4px", fontSize: "0.75rem", color: "#334155" }}>
            <span style={{ fontWeight: 600 }}>Demo Credentials:</span>
            <div>Patient: patient@hospital.local / Patient123!</div>
            <div>Doctor: doctor@hospital.local / Doctor123!</div>
            <div>Admin: admin@hospital.local / Admin123!</div>
          </div>
        )}
        <div style={{ marginTop: "1rem", textAlign: "center", fontSize: "0.875rem" }}>
          Don't have an account? <Link to="/register" style={{ color: "#0f766e", textDecoration: "underline" }}>Register</Link>
        </div>
      </Card>
    </div>
  );
}
