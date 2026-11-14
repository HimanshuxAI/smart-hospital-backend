from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP", "service": "smart-hospital-ml"}


def test_health_openapi_schema_exposes_exact_response_fields() -> None:
    openapi_response = client.get("/openapi.json")
    assert openapi_response.status_code == 200
    openapi_schema = openapi_response.json()
    response_schema = openapi_schema["paths"]["/health"]["get"]["responses"]["200"]["content"][
        "application/json"
    ]["schema"]

    assert "$ref" in response_schema
    schema_name = response_schema["$ref"].removeprefix("#/components/schemas/")
    health_schema = openapi_schema["components"]["schemas"][schema_name]

    assert set(health_schema["properties"]) == {"status", "service"}
    assert set(health_schema["required"]) == {"status", "service"}
    assert health_schema["properties"]["status"]["type"] == "string"
    assert health_schema["properties"]["service"]["type"] == "string"
