"""Retry Maven downloads rate-limited by a repository, preserving build failures."""

import re
import subprocess
import sys
import time


def run(command):
    """Allow three attempts only when Maven reports a rate-limited transfer."""
    for attempt in range(3):
        result = subprocess.run(command, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True, check=False)
        print(result.stdout, end="", flush=True)
        if result.returncode == 0:
            return 0
        limited = re.search(
            r"(?m)^\[ERROR\].*Could not transfer artifact.*(?:status code: 429|Too Many Requests)",
            result.stdout,
        )
        if not limited or attempt == 2:
            return result.returncode
        delay = 20 * (attempt + 1)
        print(f"Maven download rate-limited; retrying in {delay} seconds.", flush=True)
        time.sleep(delay)
    return 1


if __name__ == "__main__":
    sys.exit(run(sys.argv[1:]))
