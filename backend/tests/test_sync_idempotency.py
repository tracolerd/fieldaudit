"""Regression coverage for retry-safe inspection synchronization."""

from fastapi.testclient import TestClient

from backend.main import app, server_inspection_store

client = TestClient(app)


def setup_function() -> None:
    server_inspection_store.clear()


def test_exact_retry_does_not_increment_server_revision() -> None:
    payload = {
        "id": "inspection-1",
        "title": "Initial inspection",
        "status": "DRAFT",
        "local_version": 4,
    }

    first = client.post("/api/v1/inspections/sync", json=payload)
    retry = client.post("/api/v1/inspections/sync", json=payload)

    assert first.status_code == 200
    assert retry.status_code == 200
    assert first.json()["server_version"] == 1
    assert retry.json()["server_version"] == 1
    assert "Duplicate sync acknowledged" in retry.json()["message"]


def test_same_local_revision_with_different_content_conflicts() -> None:
    first = client.post(
        "/api/v1/inspections/sync",
        json={
            "id": "inspection-2",
            "title": "Original title",
            "status": "DRAFT",
            "local_version": 1,
        },
    )
    changed_retry = client.post(
        "/api/v1/inspections/sync",
        json={
            "id": "inspection-2",
            "title": "Different title without a version bump",
            "status": "DRAFT",
            "local_version": 1,
        },
    )

    assert first.status_code == 200
    assert changed_retry.status_code == 409


def test_new_local_revision_increments_server_revision_and_stale_one_conflicts() -> None:
    initial = client.post(
        "/api/v1/inspections/sync",
        json={
            "id": "inspection-3",
            "title": "Initial",
            "status": "DRAFT",
            "local_version": 2,
        },
    )
    update = client.post(
        "/api/v1/inspections/sync",
        json={
            "id": "inspection-3",
            "title": "Updated",
            "status": "SUBMITTED",
            "local_version": 3,
        },
    )
    stale = client.post(
        "/api/v1/inspections/sync",
        json={
            "id": "inspection-3",
            "title": "Old data",
            "status": "DRAFT",
            "local_version": 2,
        },
    )

    assert initial.json()["server_version"] == 1
    assert update.status_code == 200
    assert update.json()["server_version"] == 2
    assert stale.status_code == 409
