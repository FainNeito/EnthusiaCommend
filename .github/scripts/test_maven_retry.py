"""Checks that rate limiting retries never conceal a failed build."""

import subprocess
import unittest
from unittest.mock import patch

import maven_retry


class MavenRetryTest(unittest.TestCase):
    limited = subprocess.CompletedProcess(
        ["mvn"], 1,
        "[ERROR] Could not transfer artifact plugin:pom:1 from/to central: status code: 429\n",
    )

    @patch("maven_retry.time.sleep")
    @patch("maven_retry.subprocess.run")
    def test_retries_transfer_and_returns_success(self, execute, sleep):
        execute.side_effect = [self.limited, subprocess.CompletedProcess(["mvn"], 0, "Success\n")]
        self.assertEqual(0, maven_retry.run(["mvn", "-U", "verify"]))
        self.assertEqual(2, execute.call_count)
        sleep.assert_called_once_with(20)

    @patch("maven_retry.time.sleep")
    @patch("maven_retry.subprocess.run")
    def test_test_failure_is_not_retried(self, execute, sleep):
        execute.return_value = subprocess.CompletedProcess(["mvn"], 7, "[ERROR] Tests failed: HTTP 429\n")
        self.assertEqual(7, maven_retry.run(["mvn"]))
        execute.assert_called_once()
        sleep.assert_not_called()

    @patch("maven_retry.time.sleep")
    @patch("maven_retry.subprocess.run")
    def test_persistent_rate_limit_still_fails(self, execute, sleep):
        execute.return_value = self.limited
        self.assertEqual(1, maven_retry.run(["mvn"]))
        self.assertEqual(3, execute.call_count)
        self.assertEqual(2, sleep.call_count)
