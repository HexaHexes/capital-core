# The district interiors gain rotation (capital-core)

## Changeset

The authored district interiors now place with per-district rotation - the
planner's four-by-four grid cycles through none / clockwise-90 / 180 /
counterclockwise-90 so adjacent districts never repeat the same orientation,
and the placement offset compensates for the rotated bounding box so every
interior stays centered on its platform.

- `TemplateDistricts.placeInterior` takes the rotation, computes the rotated
  extent, and centers accordingly; the ordinal still selects the template
  (crash-resume deterministic).
- The gametest now places the hall interior at all four rotations and asserts
  the template's floor block lands at each platform's center row - the
  centering math is verified per rotation, not assumed.
- `expectedTests` 4 -> 5.

## Checklist

- [x] One logical changeset
- [x] `./gradlew build` passes locally (JUnit green)
- [x] `./gradlew runGameTestServer` passes locally (all 5 required tests passed)
- [x] README updated in this PR - n/a (test/geometry only)
- [x] No logs committed

## Breaking change

- [ ] Breaking

## Version bump

- No version change - the post-merge bump design owns the train.
