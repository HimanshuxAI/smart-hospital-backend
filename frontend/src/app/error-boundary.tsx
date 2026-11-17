import { Component, type ErrorInfo, type ReactNode } from "react";
import { Button } from "../components/ui/button";
import { Card } from "../components/ui/card";

type Props = {
  children: ReactNode;
};

type State = {
  hasError: boolean;
};

export class AppErrorBoundary extends Component<Props, State> {
  public override state: State = {
    hasError: false,
  };

  public static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  public override componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Uncaught error:", error, errorInfo);
  }

  public override render() {
    if (this.state.hasError) {
      return (
        <div style={{ display: "flex", minHeight: "80vh", alignItems: "center", justifyContent: "center", padding: "1rem" }}>
          <Card title="Something went wrong" style={{ width: "100%", maxWidth: "440px", textAlign: "center" }}>
            <p style={{ margin: "1rem 0", color: "#64748b" }}>
              An unexpected error occurred. Please reload the page to continue.
            </p>
            <Button onClick={() => window.location.reload()} style={{ width: "100%" }}>
              Reload Page
            </Button>
          </Card>
        </div>
      );
    }

    return this.props.children;
  }
}
