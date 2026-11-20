import { Link } from "react-router-dom";
import { Card } from "../../components/ui/card";

export function NotFoundPage() {
  return (
    <div style={{ display: "flex", minHeight: "60vh", alignItems: "center", justifyContent: "center", padding: "1rem" }}>
      <Card title="404 - Page Not Found" style={{ width: "100%", maxWidth: "400px", textAlign: "center" }}>
        <p style={{ margin: "1rem 0", color: "var(--color-muted)" }}>The page you are looking for does not exist.</p>
        <Link to="/login" style={{ color: "var(--color-primary-strong)", textDecoration: "underline" }}>Return to Sign In</Link>
      </Card>
    </div>
  );
}
