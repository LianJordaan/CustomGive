"""Freeze exact-version release JARs and write their audited SHA-512 manifest."""

from __future__ import annotations

import datetime as dt
import hashlib
import json
import shutil
import subprocess
import zipfile
from pathlib import Path


PROJECT = Path(__file__).resolve().parents[1]
WORKSPACE = PROJECT.parent.parent
VERSIONS = json.loads((PROJECT / "versions.json").read_text(encoding="utf-8"))
CANDIDATES = WORKSPACE / "testing" / "customgive" / "candidates"


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=PROJECT, text=True).strip()


def main() -> None:
    source_status = git("status", "--porcelain")
    if source_status:
        raise RuntimeError("Commit source and test harness changes before freezing JARs")
    source_commit = git("rev-parse", "HEAD")
    if not (PROJECT / "gradle.properties").read_text(encoding="utf-8").find(
            "mod_version=1.1.0") >= 0:
        raise RuntimeError("The project version is not 1.1.0")
    CANDIDATES.mkdir(parents=True, exist_ok=True)
    manifest = {
        "frozen_at_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "source_commit": source_commit,
        "source_clean_at_freeze": True,
        "build_command": "JAVA_HOME=JDK25 gradlew.bat build --console=plain",
        "minecraft_versions": {},
    }
    for config in VERSIONS.values():
        version = config["minecraft"]
        built = PROJECT / "versions" / version / "build" / "libs" / \
            f"CustomGive-fabric-{version}-1.1.0.jar"
        if not built.is_file():
            raise FileNotFoundError(built)
        digest = hashlib.sha512(built.read_bytes()).hexdigest()
        destination = CANDIDATES / f"CustomGive-fabric-{version}-1.1.0-{digest[:12]}.jar"
        with zipfile.ZipFile(built) as jar:
            metadata = json.loads(jar.read("fabric.mod.json"))
            names = set(jar.namelist())
        if metadata["version"] != "1.1.0" or metadata["depends"]["minecraft"] != version:
            raise RuntimeError(f"Incorrect Fabric metadata in {built}")
        if any("CustomGiveLiveGameTest" in name or
               "CustomGiveLegacyLiveTest" in name for name in names):
            raise RuntimeError(f"Private test classes leaked into {built}")
        if destination.exists() and hashlib.sha512(destination.read_bytes()).hexdigest() != digest:
            raise RuntimeError(f"Frozen candidate changed: {destination}")
        shutil.copyfile(built, destination)
        manifest["minecraft_versions"][version] = {
            "path": str(destination.relative_to(WORKSPACE)).replace("\\", "/"),
            "sha512": digest,
            "fabric_metadata": {
                "id": metadata["id"],
                "version": metadata["version"],
                "minecraft_dependency": metadata["depends"]["minecraft"],
                "fabric_loader_dependency": metadata["depends"].get("fabricloader"),
                "java_dependency": metadata["depends"].get("java"),
            },
            "client_pin": {
                "minecraft": version,
                "fabric_loader": "0.19.5",
                "fabric_api": config["fabricApi"],
                "java": config["java"],
            },
            "qualified": False,
        }
        print(f"{version} {digest[:12]} {destination.name}")
    target = CANDIDATES / "manifest-1.1.0.json"
    target.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(target)


if __name__ == "__main__":
    main()
