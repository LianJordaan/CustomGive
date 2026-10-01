"""Switch a private live-test player to Survival after its client has joined.

Start this before the relog client run with a fresh run directory. It uses the
app's authenticated server console, never the test mod or production JAR.
"""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path


WORKSPACE = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(WORKSPACE))

from dashboard.servers.manager import ServerManager  # noqa: E402


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("instance_id")
    parser.add_argument("username")
    parser.add_argument("client_log", type=Path)
    parser.add_argument("--timeout", type=int, default=180)
    args = parser.parse_args()

    manager = ServerManager()
    deadline = time.monotonic() + args.timeout
    join_line = f"{args.username} joined the game"
    while time.monotonic() < deadline:
        joined = False
        if args.client_log.is_file():
            contents = args.client_log.read_text(encoding="utf-8", errors="replace")
            if join_line in contents:
                joined = True
        if not joined:
            # Newer clients can omit their own join announcement from chat.
            # The server's online-player list is authoritative for the switch.
            listing = str(manager.console(args.instance_id, "list"))
            joined = args.username in [name.strip() for name in
                                      listing.partition(":")[2].split(",")]
        if joined:
            result = manager.console(args.instance_id,
                                     f"gamemode survival {args.username}")
            if "Survival" not in str(result):
                raise RuntimeError(f"Server did not confirm Survival mode: {result}")
            print(f"{args.username}: {result}")
            return
        time.sleep(0.5)
    raise TimeoutError(f"Client did not join within {args.timeout}s: {args.client_log}")


if __name__ == "__main__":
    main()
