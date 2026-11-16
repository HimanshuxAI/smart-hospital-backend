import { useState, useRef, useEffect } from "react";
import { Bell, Menu, LogOut, CheckCircle2, AlertTriangle, Clock } from "lucide-react";
import { useNavigate } from "react-router-dom";

import type { AuthUser } from "../../api/types";
import { Avatar } from "../ui/avatar";
import { useAuth } from "../../auth/auth-context";

const DEMO_NOTIFICATIONS = [
  {
    id: 1,
    title: "Queue Position Update",
    desc: "Your token #14 is now 2 patients away from consultation.",
    time: "3m ago",
    type: "info"
  },
  {
    id: 2,
    title: "Appointment Confirmed",
    desc: "Dr. Sarah Jenkins confirmed your Cardiology slot tomorrow.",
    time: "25m ago",
    type: "success"
  },
  {
    id: 3,
    title: "Triage Alert",
    desc: "Emergency arrival classified as High Priority by ML engine.",
    time: "1h ago",
    type: "warning"
  }
];

export function TopBar({
  user,
  onOpenNavigation,
  demoMode = false,
}: {
  user: AuthUser;
  onOpenNavigation(): void;
  demoMode?: boolean;
}) {
  const { signOut } = useAuth();
  const navigate = useNavigate();

  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const notifRef = useRef<HTMLDivElement>(null);
  const profileRef = useRef<HTMLDivElement>(null);

  const handleSignOut = async () => {
    await signOut();
    navigate("/login", { replace: true });
  };

  useEffect(() => {
    const handleOutsideClick = (e: MouseEvent) => {
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
        setNotificationsOpen(false);
      }
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) {
        setProfileOpen(false);
      }
    };
    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  return (
    <header className="topbar">
      <div className="topbar-leading">
        <button
          className="icon-button"
          type="button"
          aria-label="Toggle navigation"
          onClick={onOpenNavigation}
          title="Toggle Navigation Menu"
        >
          <Menu size={18} aria-hidden="true" />
        </button>
        {demoMode ? <span className="demo-badge">Demo data</span> : null}
      </div>

      <div className="topbar-actions">
        {/* Notifications Dropdown */}
        <div ref={notifRef} style={{ position: "relative" }}>
          <button
            className="icon-button"
            type="button"
            aria-label="Notifications"
            onClick={() => setNotificationsOpen(prev => !prev)}
            style={{ position: "relative" }}
          >
            <Bell size={17} aria-hidden="true" />
            <span style={{
              position: "absolute",
              top: "4px",
              right: "4px",
              width: "7px",
              height: "7px",
              borderRadius: "50%",
              background: "var(--accent-vermilion)",
              border: "1px solid var(--color-ink)"
            }} />
          </button>

          {notificationsOpen && (
            <div style={{
              position: "absolute",
              top: "calc(100% + 8px)",
              right: 0,
              width: "320px",
              background: "#ffffff",
              border: "2px solid var(--color-ink)",
              borderRadius: "6px",
              boxShadow: "var(--shadow-hard-md)",
              zIndex: 50,
              padding: "1rem"
            }}>
              <div style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                paddingBottom: "0.5rem",
                marginBottom: "0.75rem",
                borderBottom: "1.5px dashed var(--color-border)",
                fontFamily: "var(--font-mono)",
                fontSize: "0.75rem",
                fontWeight: 700
              }}>
                <span>NOTIFICATIONS (3)</span>
                <span style={{ color: "var(--accent-vermilion)", cursor: "pointer" }} onClick={() => setNotificationsOpen(false)}>Close</span>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: "0.6rem" }}>
                {DEMO_NOTIFICATIONS.map((n) => (
                  <div key={n.id} style={{
                    padding: "0.6rem",
                    border: "1px solid var(--color-border)",
                    borderRadius: "4px",
                    background: "var(--bg-canvas)"
                  }}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.78rem", fontWeight: 700 }}>
                      <span>{n.title}</span>
                      <span style={{ fontSize: "0.68rem", color: "var(--color-muted)" }}>{n.time}</span>
                    </div>
                    <div style={{ fontSize: "0.75rem", color: "var(--color-ink-subtle)", marginTop: "0.2rem", lineHeight: 1.3 }}>
                      {n.desc}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* User Account & Logout Menu */}
        <div ref={profileRef} style={{ position: "relative" }}>
          <div
            className="user-summary"
            onClick={() => setProfileOpen(prev => !prev)}
            style={{ cursor: "pointer", userSelect: "none" }}
          >
            <Avatar name={user.email.split("@")[0] ?? user.email} size="sm" />
            <div>
              <strong>{user.email}</strong>
              <small>{user.role.toLowerCase()}</small>
            </div>
          </div>

          {profileOpen && (
            <div style={{
              position: "absolute",
              top: "calc(100% + 8px)",
              right: 0,
              width: "220px",
              background: "#ffffff",
              border: "2px solid var(--color-ink)",
              borderRadius: "6px",
              boxShadow: "var(--shadow-hard-md)",
              zIndex: 50,
              padding: "0.75rem"
            }}>
              <div style={{
                paddingBottom: "0.5rem",
                marginBottom: "0.5rem",
                borderBottom: "1.5px dashed var(--color-border)",
                fontSize: "0.78rem"
              }}>
                <div style={{ fontWeight: 700, color: "var(--color-ink)" }}>{user.email}</div>
                <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.68rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
                  ROLE: {user.role}
                </div>
              </div>

              <button
                type="button"
                onClick={handleSignOut}
                className="button button-secondary"
                style={{
                  width: "100%",
                  minHeight: "2.25rem",
                  padding: "0.35rem 0.6rem",
                  fontSize: "0.8rem",
                  justifyContent: "flex-start",
                  gap: "0.5rem",
                  color: "#dc2626",
                  borderColor: "var(--color-ink)"
                }}
              >
                <LogOut size={14} color="#dc2626" />
                <span>Log Out</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
