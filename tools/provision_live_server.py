"""Create one isolated app-managed backend for a CustomGive client test."""

from __future__ import annotations

import json
import sys
from pathlib import Path


WORKSPACE = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(WORKSPACE))

from dashboard.servers.catalog import load_catalog, runtime_for  # noqa: E402
from dashboard.servers.manager import ServerManager, server_settings  # noqa: E402
from dashboard.services.modrinth import get_json  # noqa: E402


def main(version: str) -> None:
    manager = ServerManager()
    config = server_settings()
    if not config.get("eula_accepted"):
        raise RuntimeError("Minecraft EULA acceptance is not recorded in the app")
    existing = manager.list()
    if any(row["port"] in range(27210, 27231) and
           row["version"] == version and
           row["name"].startswith("CustomGive live") for row in existing):
        raise RuntimeError(f"A CustomGive {version} instance already exists")
    catalog = load_catalog()
    runtime = runtime_for(catalog, "PAPER", version, allow_experimental=True)
    operator = get_json("https://api.minecraftservices.com/minecraft/profile/lookup/name/" +
                        config["operator"])
    record = manager.transport.call(
        "create", name=f"CustomGive live {version}", loader="PAPER", version=version,
        memory_gb=4, cpus=4, operator=operator, artifacts=[], eula_accepted=True,
        connection_mode="standalone_offline", unattended_testing=True,
        bind_ip="127.0.0.1", join_host="127.0.0.1", first_port=27210, last_port=27230,
        **runtime,
    )
    print(json.dumps({"created": record["id"], "port": record["port"],
                      "version": version, "paper_build": record.get("paper_build"),
                      "build_channel": record.get("build_channel"), "java": record["java"]}))
    manager.action("start", record["id"])
    print(json.dumps({"started": record["id"], "port": record["port"]}))


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: provision_live_server.py <minecraft-version>")
    main(sys.argv[1])
