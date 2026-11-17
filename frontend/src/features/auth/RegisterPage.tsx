import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/auth-context";
import { Button } from "../../components/ui/button";
import { Card } from "../../components/ui/card";
import { TextField } from "../../components/ui/field";
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
    <div style={{ display: "flex", minHeight: "80vh", alignItems: "center", justifyContent: "center", padding: "1rem" }}>
      <Card title="Patient Registration" style={{ width: "100%", maxWidth: "440px" }}>
        {generalError && (
          <div role="alert" style={{ padding: "0.75rem", marginBottom: "1rem", borderRadius: "4px", backgroundColor: "#fee2e2", color: "#991b1b", fontSize: "0.875rem" }}>
            {generalError}
          </div>
        )}
        <form noValidate onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <TextField
            label="Full Name"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            error={fieldErrors.fullName}
            required
          />
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
            autoComplete="new-password"
            required
          />
          <TextField
            label="Phone"
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            error={fieldErrors.phone}
            required
          />
          <TextField
            label="Date of Birth"
            type="date"
            value={dateOfBirth}
            onChange={(e) => setDateOfBirth(e.target.value)}
            error={fieldErrors.dateOfBirth}
            required
          />
          <Button type="submit" loading={loading} loadingLabel="Registering..." style={{ width: "100%", marginTop: "0.5rem" }}>
            Register
          </Button>
        </form>
        <div style={{ marginTop: "1rem", textAlign: "center", fontSize: "0.875rem" }}>
          Already have an account? <Link to="/login" style={{ color: "#0f766e", textDecoration: "underline" }}>Sign In</Link>
        </div>
      </Card>
    </div>
  );
}
