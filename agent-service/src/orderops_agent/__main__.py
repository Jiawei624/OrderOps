"""Command-line entry point for the Agent package."""

import argparse
from importlib.metadata import version


def main() -> None:
    parser = argparse.ArgumentParser(description="OrderOps Agent service and workers")
    parser.add_argument("--version", action="version", version=version("orderops-agent"))
    parser.parse_args()
    parser.print_help()


if __name__ == "__main__":
    main()
