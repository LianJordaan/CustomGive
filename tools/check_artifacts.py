"""Validate the five deployable Fabric JARs after ``gradlew build``."""

from __future__ import annotations

import hashlib
import json
import struct
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TARGETS = {
    target["minecraft"]: target["java"] + 44
    for target in json.loads((ROOT / "versions.json").read_text(encoding="utf-8")).values()
}


def check_jar(version: str, class_major: int) -> str:
    jars = list((ROOT / "versions" / version / "build" / "libs").glob("*.jar"))
    jars = [jar for jar in jars if not jar.name.endswith("-sources.jar")]
    if len(jars) != 1:
        raise AssertionError(f"{version}: expected exactly one deployable JAR, got {jars}")
    jar = jars[0]
    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        metadata = json.loads(archive.read("fabric.mod.json"))
        if metadata["id"] != "lian-customgive":
            raise AssertionError(f"{jar}: wrong mod ID")
        if metadata["depends"]["minecraft"] != version:
            raise AssertionError(f"{jar}: wrong Minecraft dependency")
        if metadata["environment"] != "client":
            raise AssertionError(f"{jar}: not client-only")
        if metadata["entrypoints"]["client"] != ["io.github.lianjordaan.CustomGiveClient"]:
            raise AssertionError(f"{jar}: wrong client entrypoint")
        if not any(name.startswith("LICENSE") for name in names):
            raise AssertionError(f"{jar}: missing license")
        if any("template" in name.lower() or "examplemod" in name.lower() for name in names):
            raise AssertionError(f"{jar}: template content remains")
        if any("/test/" in name or "LiveTest" in name for name in names):
            raise AssertionError(f"{jar}: private live-test code entered the release JAR")
        classes = [name for name in names if name.endswith(".class")]
        if not classes:
            raise AssertionError(f"{jar}: no classes")
        for name in classes:
            header = archive.read(name)[:8]
            if len(header) != 8 or header[:4] != b"\xca\xfe\xba\xbe":
                raise AssertionError(f"{jar}: invalid class {name}")
            major = struct.unpack(">H", header[6:8])[0]
            if major != class_major:
                raise AssertionError(f"{jar}: {name} has Java class major {major}, expected {class_major}")
    digest = hashlib.sha512(jar.read_bytes()).hexdigest()
    return f"{version}: {jar.name} sha512={digest}"


if __name__ == "__main__":
    for target, major in TARGETS.items():
        print(check_jar(target, major))
