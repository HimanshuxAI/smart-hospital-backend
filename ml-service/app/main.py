from fastapi import FastAPI

app = FastAPI(
    title="Smart Hospital ML Service",
    version="1.0.0",
    description="Waiting-time prediction service for Smart Hospital.",
)


@app.get("/health", tags=["Health"], summary="Service health")
def health() -> dict[str, str]:
    return {"status": "UP", "service": "smart-hospital-ml"}
