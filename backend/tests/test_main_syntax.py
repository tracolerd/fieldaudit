"""Small, dependency-light regression test for the backend syntax blocker."""

import ast
from pathlib import Path
import unittest


MAIN_MODULE = Path(__file__).resolve().parents[1] / "main.py"


class BackendSyntaxTests(unittest.TestCase):
    def test_main_module_parses(self) -> None:
        source = MAIN_MODULE.read_text(encoding="utf-8")
        ast.parse(source, filename=str(MAIN_MODULE))


if __name__ == "__main__":
    unittest.main()
