#!/usr/bin/env python3
"""Generates the district interior templates for the capital generation engine.

Committed tooling, not a one-off: the templates are code so they are
regeneratable and reviewable. Run from the repository root:

    python tools/gen_district_templates.py

Writes gzip NBT structure files under src/main/resources/data/
<ns>/structure/district/ - the interiors the planner drops onto each district
platform (Option C hybrid: the planner owns layout/roads/terrain; authored
pieces fill the interiors).

All pieces are 15x9x15 interiors with a hollow hall, built from full cubes
only, designed to sit on the planner's 16x16 district platform.
"""

from __future__ import annotations

import gzip
import struct
from pathlib import Path

TAG_BYTE = 1
TAG_INT = 3
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10

DATA_VERSION = 3955  # 1.21.1
NAMESPACE = "capital_core"
OUT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data" / NAMESPACE / "structure" / "district"


# --- minimal NBT writer -----------------------------------------------------


def _string(value: str) -> bytes:
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _payload(tag: int, value):
    if tag == TAG_BYTE:
        return struct.pack(">b", value)
    if tag == TAG_INT:
        return struct.pack(">i", value)
    if tag == TAG_STRING:
        return _string(value)
    if tag == TAG_LIST:
        element_type, items = value
        out = bytes([element_type]) + struct.pack(">i", len(items))
        for item in items:
            out += _payload(element_type, item)
        return out
    if tag == TAG_COMPOUND:
        out = b""
        for name, item_tag, item_value in value:
            out += bytes([item_tag]) + _string(name) + _payload(item_tag, item_value)
        return out + b"\x00"
    raise ValueError(f"unsupported tag {tag}")


def int_list(values) -> tuple[int, list]:
    return (TAG_INT, list(values))


def encode_root(body) -> bytes:
    # mtime=0 keeps regeneration byte-deterministic (no phantom diffs on rebuild).
    return gzip.compress(bytes([TAG_COMPOUND]) + _string("") + _payload(TAG_COMPOUND, body), mtime=0)


# --- template builder -------------------------------------------------------


class Template:
    def __init__(self, width: int, height: int, depth: int):
        self.size = (width, height, depth)
        self.palette: list[tuple[str, dict]] = []
        self.palette_index: dict[tuple, int] = {}
        self.blocks: dict[tuple[int, int, int], int] = {}

    def state(self, name: str, **props) -> int:
        key = (name, tuple(sorted(props.items())))
        if key not in self.palette_index:
            self.palette_index[key] = len(self.palette)
            self.palette.append((name, props))
        return self.palette_index[key]

    def set(self, x: int, y: int, z: int, name: str, **props) -> None:
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.blocks[(x, y, z)] = self.state(name, **props)

    def fill(self, x0, y0, z0, x1, y1, z1, name: str, **props) -> None:
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, **props)

    def walls(self, x0, z0, x1, z1, y0, y1, name: str, **props) -> None:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, z0, name, **props)
                self.set(x, y, z1, name, **props)
            for z in range(z0, z1 + 1):
                self.set(x0, y, z, name, **props)
                self.set(x1, y, z, name, **props)

    def to_nbt(self) -> bytes:
        blocks = []
        for (x, y, z) in sorted(self.blocks):
            blocks.append([("pos", TAG_LIST, int_list([x, y, z])),
                           ("state", TAG_INT, self.blocks[(x, y, z)])])
        palette = []
        for name, props in self.palette:
            fields = [("Name", TAG_STRING, name)]
            if props:
                fields.append(("Properties", TAG_COMPOUND,
                               [(k, TAG_STRING, v) for k, v in sorted(props.items())]))
            palette.append(fields)
        body = [
            ("size", TAG_LIST, int_list(list(self.size))),
            ("entities", TAG_LIST, (TAG_COMPOUND, [])),
            ("blocks", TAG_LIST, (TAG_COMPOUND, blocks)),
            ("palette", TAG_LIST, (TAG_COMPOUND, palette)),
            ("DataVersion", TAG_INT, DATA_VERSION),
        ]
        return encode_root(body)


# --- the three district interiors --------------------------------------------

HALL_WALL = "minecraft:stone_bricks"
HALL_TRIM = "minecraft:andesite"
HALL_LIGHT = "minecraft:lantern"
MARKET_STALL = "minecraft:stripped_oak_wood"
MARKET_CANOPY = "minecraft:red_terracotta"
GARDEN_SOIL = "minecraft:grass_block"
GARDEN_HEDGE = "minecraft:spruce_leaves"


def build_hall() -> Template:
    """A walled district hall: stone-brick walls, andesite floor, lanterns."""
    t = Template(15, 9, 15)
    t.fill(0, 0, 0, 14, 0, 14, HALL_TRIM)
    t.walls(0, 0, 14, 14, 1, 4, HALL_WALL)
    for x in range(2, 13):
        if x % 4 == 2:
            t.set(x, 3, 0, HALL_LIGHT)
            t.set(x, 3, 14, HALL_LIGHT)
            t.set(0, 3, x, HALL_LIGHT)
            t.set(14, 3, x, HALL_LIGHT)
    for c in ((0, 0), (14, 0), (0, 14), (14, 14)):
        t.fill(c[0], 1, c[1], c[0], 5, c[1], HALL_TRIM)
    t.fill(5, 1, 5, 9, 1, 9, HALL_TRIM)
    t.fill(6, 1, 0, 8, 2, 0, "minecraft:air")
    t.fill(5, 5, 0, 9, 5, 14, HALL_WALL)
    return t


def build_market() -> Template:
    """An open market row: stall counters under terracotta canopies."""
    t = Template(15, 9, 15)
    t.fill(0, 0, 0, 14, 0, 14, "minecraft:oak_planks")
    stalls = ((2, 2), (8, 2), (2, 9), (8, 9))
    for (sx, sz) in stalls:
        t.fill(sx, 1, sz, sx + 3, 1, sz + 2, MARKET_STALL)
        t.fill(sx, 2, sz, sx + 3, 2, sz + 2, MARKET_CANOPY)
        t.fill(sx, 3, sz, sx, 3, sz + 2, MARKET_STALL)
        t.fill(sx + 3, 3, sz, sx + 3, 3, sz + 2, MARKET_STALL)
    for x in range(0, 15, 2):
        t.set(x, 1, 7, "minecraft:oak_log")
    t.fill(0, 0, 7, 14, 0, 7, "minecraft:cobblestone")
    return t


def build_garden() -> Template:
    """A terraced garden: grass terraces, hedges, a central pond."""
    t = Template(15, 9, 15)
    t.fill(0, 0, 0, 14, 0, 14, GARDEN_SOIL)
    t.fill(3, 1, 3, 11, 1, 11, GARDEN_SOIL)
    t.fill(5, 2, 5, 9, 2, 9, GARDEN_SOIL)
    for i in range(3, 12):
        t.set(i, 2, 3, GARDEN_HEDGE)
        t.set(i, 2, 11, GARDEN_HEDGE)
        t.set(3, 2, i, GARDEN_HEDGE)
        t.set(11, 2, i, GARDEN_HEDGE)
    t.fill(6, 2, 6, 8, 2, 8, "minecraft:water")
    for x, z in ((4, 4), (10, 4), (4, 10), (10, 10)):
        t.fill(x, 3, z, x, 4, z, "minecraft:azalea")
        t.set(x, 5, z, GARDEN_HEDGE)
    t.fill(0, 0, 7, 14, 0, 7, "minecraft:coarse_dirt")
    return t


PIECES = {
    "hall": build_hall,
    "market": build_market,
    "garden": build_garden,
}


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, builder in PIECES.items():
        template = builder()
        path = OUT / f"{name}.nbt"
        path.write_bytes(template.to_nbt())
        print(f"wrote data/{NAMESPACE}/structure/district/{name}.nbt "
              f"({len(template.blocks)} blocks, {len(template.palette)} palette entries)")


if __name__ == "__main__":
    main()
