# Android AArch64 Raylib aggregate ABI matrix

## Result

The pinned Jolt direct aggregate implementation is numerically correct on
x86_64 Linux for the selected Raylib aggregate shapes, including by-value
arguments and Jolt's destination-pointer aggregate-return convention.

The original Android ARM64 built-library invocation exited cleanly (status
255) because source-mode `jolt build --library` skipped the Clojure half of
`jolt.ffi`: `layout-size` was interned but unbound in the distinct app image.
The fix snapshots the runtime-image namespace set before the CLI loads
`jolt.main`, and emits a regression gate for this source-mode ordering. With
that workaround applied to the pinned Jolt revision, the translated API-35
Android run reports `layout-size-bound=1`, `Color` size `4`, and the complete
aggregate matrix succeeds. No unsafe pointer workaround was adopted. The upstream fix is tracked in
[Jolt PR #787](https://github.com/jolt-lang/jolt/pull/787), “Fix source-mode
builds omitting CLI-preloaded namespace code.”

## Pinned source declarations and literal Jolt layouts

| Aggregate | Pinned Raylib C shape | Jolt literal layout | C size / relevant offsets | Linux direct result | Android ARM64 result |
| --- | --- | --- | --- | --- | --- |
| `Color` | four `unsigned char` fields | `[:struct [[:r :uint8] ... [:a :uint8]]]` | 4; `r=0`, `a=3` | argument score `4321` | size 4; argument score `4321` |
| `Vector2` | two `float` fields | `[:struct [[:x :float] [:y :float]]]` | 8; `x=0`, `y=4` | argument `21.0`; return `[7.0 8.0]` | argument `21.0`; return `[7.0 8.0]` |
| `Vector3` | three `float` fields | `[:struct [[:x :float] [:y :float] [:z :float]]]` | 12; `z=8` | argument score `321.0` | argument score `321.0` |
| `Rectangle` | four `float` fields | `[:struct [[:x :float] [:y :float] [:width :float] [:height :float]]]` | 16; `width=8`, `height=12` | argument score `4321.0` | argument score `4321.0` |
| `Camera2D` | two nested `Vector2`, two `float` | literal nested structs | 24; target=8, rotation=16, zoom=20 | argument score `21.0` | argument score `21.0` |
| `Camera3D` | three nested `Vector3`, `float`, `int` | literal nested structs | 44; target=12, up=24, fovy=36, projection=40 | argument score `15.0` | argument score `15.0` |
| `Texture2D` | `uint` plus four `int` | `[:struct [[:id :uint] ... [:format :int]]]` | 20; `id=0`, `format=16` | argument `15`; return `[7 8 9 10 11]` | argument `15`; return `[7 8 9 10 11]` |

The static assertions in
[`aggregate_oracle.c`](../../raylib/abi/aggregate_oracle.c) are compiled into
`libmain.so`, so an ABI size or offset change in the pinned header fails the
Android C build. `poc.raylib.abi` contains the exact direct literal layouts;
the C oracle accepts/returns those values without Raylib rendering or assets.

## Reproduction

### Linux direct matrix

```sh
nix --extra-experimental-features 'nix-command flakes' develop -c \
  ./scripts/raylib-abi-verify-linux
```

This builds a small host `.so` against `$RAYLIB_SOURCE/src/raylib.h`, then uses
Jolt `ffi/foreign-fn`/`defcfn` direct by-value declarations. It writes the
observed map to stdout.

### Android reduced failure and workaround validation

The original run compiled `aggregate_oracle.c` into the `libmain.so` process
image, included `poc.raylib.abi` in the pinned `tarm64le` Jolt shared library,
and called `abi/verify!` on the established NativeActivity/Jolt owner thread.
Its stage instrumentation emitted:

```text
verification-start
oracle=1
stage=layouts
stage=layout-color
```

The process then exited cleanly before entering any C aggregate function. A
minimal exported probe showed ordinary map access and the compiled layout value
worked, while `bound? #'jolt.ffi/layout-size` was false. The C oracle symbol
table confirms all `jolt_raylib_abi_*` symbols are exported by `libmain.so`.
After applying the workaround patch, the same probe sequence emits
`layout-size-bound=1`, `color-size=4`, and `aggregate matrix result=1`; see
[`evidence/android-workaround-logcat.txt`](evidence/android-workaround-logcat.txt).

## Boundaries

- A desktop pass does **not** establish Android AArch64 ABI behavior.
- The API-35 x86_64 emulator runs ARM64 through Berberis translation; it is not
  native ARM64 hardware.
- The original failure occurred before aggregate argument/return execution and
  was not an AArch64 calling-convention failure; it was an omitted source
  namespace in the generated image.
- The workaround is preserved as a patch until Jolt PR #787 is incorporated by
  the pinned dependency.
- With the workaround, direct aggregate calls and returns pass on the tested
  translated Android environment. Native ARM64 hardware remains untested.
