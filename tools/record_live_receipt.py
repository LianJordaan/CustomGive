"""Save a verified, exact-JAR CustomGive client/server test receipt.

The private client harness must have completed both phases before this tool runs.
It refuses a PASS receipt if a phase marker, runtime JAR hash, or server-denial
message is missing from the logs.
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import re
import shutil
import sys
import zipfile
from pathlib import Path


PROJECT = Path(__file__).resolve().parents[1]
WORKSPACE = PROJECT.parent.parent
sys.path.insert(0, str(WORKSPACE))

from dashboard.servers.manager import ServerManager  # noqa: E402


def match(pattern: str, text: str) -> str:
    found = re.search(pattern, text, re.MULTILINE)
    if not found:
        raise ValueError(f"Missing required client evidence: {pattern}")
    return found.group(1)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version")
    parser.add_argument("candidate", type=Path)
    parser.add_argument("creative_run", type=Path)
    parser.add_argument("survival_run", type=Path)
    parser.add_argument("server_id")
    parser.add_argument("receipt_dir", type=Path)
    parser.add_argument("--source-revision", required=True)
    parser.add_argument("--note", action="append", default=[])
    args = parser.parse_args()

    candidate = args.candidate.resolve(strict=True)
    creative_path = args.creative_run / "logs" / "latest.log"
    survival_path = args.survival_run / "logs" / "latest.log"
    creative = creative_path.read_text(encoding="utf-8", errors="replace")
    survival = survival_path.read_text(encoding="utf-8", errors="replace")
    sha = hashlib.sha512(candidate.read_bytes()).hexdigest()
    for phase in (creative, survival):
        if f"sha512={sha}" not in phase:
            raise ValueError("Client did not load the exact candidate JAR")
    if "CUSTOMGIVE_LIVE creative PASS:" not in creative:
        raise ValueError("Creative phase did not pass")
    if "Sent 1 minecraft:stone" not in creative or "Sent 2 minecraft:diamond" not in creative:
        raise ValueError("Command success messages are absent from Creative log")
    if "CUSTOMGIVE_LIVE verify_survival PASS:" not in survival:
        raise ValueError("Relog/Survival phase did not pass")
    if "CustomGive requires Creative mode" not in survival:
        raise ValueError("Server-authoritative Survival denial is absent")

    minecraft = match(r"Loading Minecraft ([^ ]+) with Fabric Loader", creative)
    if minecraft != args.version or minecraft != match(
            r"Loading Minecraft ([^ ]+) with Fabric Loader", survival):
        raise ValueError("Client Minecraft version differs between phases")
    fabric_loader = match(r"fabricloader ([^\s]+)", creative)
    fabric_api = match(r"fabric-api ([^\s]+)", creative)
    java = int(match(r"\t- java (\d+)", creative))
    with zipfile.ZipFile(candidate) as jar:
        metadata = json.loads(jar.read("fabric.mod.json"))
    if metadata["depends"]["minecraft"] != args.version:
        raise ValueError("Packaged Minecraft dependency is not exact")

    manager = ServerManager()
    server = next((row for row in manager.list() if row["id"] == args.server_id), None)
    if server is None or server["version"] != args.version:
        raise ValueError("Server identity or Minecraft version does not match")
    if server.get("status") not in (None, "stopped", "exited") and \
            server.get("state") != "stopped":
        raise ValueError("Stop the test server before recording its final state")

    destination = (WORKSPACE / args.receipt_dir).resolve()
    if destination.exists():
        raise FileExistsError(destination)
    destination.mkdir(parents=True)
    shutil.copyfile(creative_path, destination / "creative.log")
    shutil.copyfile(survival_path, destination / "relog-survival.log")
    record = {
        "recorded_at_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "status": "passed",
        "qualification_scope": f"Minecraft {minecraft} Fabric client against "
            f"{server['loader']} server in standalone offline mode",
        "production_jar": str(candidate.relative_to(WORKSPACE)).replace("\\", "/"),
        "production_sha512": sha,
        "production_mod_version": metadata["version"],
        "source_revision": args.source_revision,
        "source_provenance_note": "Source revision records the candidate source state; "
            "the runtime SHA-512 is the exact artifact verified by this test.",
        "client": {
            "minecraft": minecraft,
            "fabric_loader": fabric_loader,
            "fabric_api": fabric_api,
            "java": java,
        },
        "server": {
            "id": server["id"],
            "loader": server["loader"],
            "minecraft": server["version"],
            "build": server.get("paper_build"),
            "build_channel": server.get("build_channel"),
            "java": server["java"],
            "mode": server["connection_mode"],
            "port": server["port"],
            "state_after_test": "stopped",
        },
        "phases": [
            {"name": "creative", "result": "PASS", "log": "creative.log",
             "checks": ["exact packaged JAR loaded", "no-argument stone", 
                        "namespaced two-diamond custom data", "server inventory sync"]},
            {"name": "relog-survival", "result": "PASS", "log": "relog-survival.log",
             "checks": ["inventory persisted", "server changed game mode",
                        "Survival command denied and no emerald added"]},
        ],
        "limits": ["Only this exact Minecraft version and JAR hash were tested",
                   "Standalone offline-mode server; authenticated multiplayer untested",
                   *args.note],
    }
    (destination / "receipt.json").write_text(
        json.dumps(record, indent=2) + "\n", encoding="utf-8")
    print(destination / "receipt.json")


if __name__ == "__main__":
    main()
