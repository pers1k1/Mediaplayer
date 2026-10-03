from pathlib import Path

import numpy as np
from PIL import Image

SIZE = 512
SUPERSAMPLE = 4
OUTPUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/glassmediaplayer/icon.png"

CAPSULE = (24.0, 140.0, 488.0, 372.0)
TILE = (56.0, 188.0, 192.0, 324.0)
TILE_RADIUS = 36.0
PLAY_POINTS = ((106.0, 222.0), (106.0, 290.0), (162.0, 256.0))
PLAY_RADIUS = 9.0
BAR_LEFT = 232.0
BAR_WIDTH = 20.0
BAR_GAP = 13.0
BAR_MAX_HEIGHT = 156.0
BAR_LEVELS = (0.34, 0.62, 0.92, 0.7, 1.0, 0.56, 0.3)

CAPSULE_TOP = np.array([36.0, 36.0, 47.0])
CAPSULE_BOTTOM = np.array([17.0, 17.0, 25.0])
TILE_TOP = np.array([100.0, 100.0, 124.0])
TILE_BOTTOM = np.array([78.0, 78.0, 98.0])
PLAY_COLOR = np.array([238.0, 240.0, 250.0])
BAR_FIRST = np.array([226.0, 224.0, 248.0])
BAR_LAST = np.array([178.0, 158.0, 238.0])
RIM_COLOR = np.array([255.0, 255.0, 255.0])


def grid() -> tuple[np.ndarray, np.ndarray]:
    steps = (np.arange(SIZE * SUPERSAMPLE) + 0.5) / SUPERSAMPLE
    return np.meshgrid(steps, steps)


def rounded_box(x: np.ndarray, y: np.ndarray, box: tuple[float, float, float, float], radius: float) -> np.ndarray:
    left, top, right, bottom = box
    half_x, half_y = (right - left) / 2.0 - radius, (bottom - top) / 2.0 - radius
    qx = np.abs(x - (left + right) / 2.0) - half_x
    qy = np.abs(y - (top + bottom) / 2.0) - half_y
    outside = np.hypot(np.maximum(qx, 0.0), np.maximum(qy, 0.0))
    return outside + np.minimum(np.maximum(qx, qy), 0.0) - radius


def polygon(x: np.ndarray, y: np.ndarray, points: tuple[tuple[float, float], ...]) -> np.ndarray:
    nearest = np.full(x.shape, np.inf)
    inside = np.zeros(x.shape, dtype=bool)
    for (ax, ay), (bx, by) in zip(points, points[1:] + points[:1]):
        ex, ey = bx - ax, by - ay
        wx, wy = x - ax, y - ay
        along = np.clip((wx * ex + wy * ey) / (ex * ex + ey * ey), 0.0, 1.0)
        nearest = np.minimum(nearest, np.hypot(wx - ex * along, wy - ey * along))
        crosses = ((ay > y) != (by > y)) & (x < ax + (y - ay) * ex / np.where(ey == 0.0, 1.0, ey))
        inside ^= crosses
    return np.where(inside, -nearest, nearest)


def coverage(distance: np.ndarray) -> np.ndarray:
    return np.clip(0.5 - distance * SUPERSAMPLE, 0.0, 1.0)


def vertical_gradient(y: np.ndarray, top: float, bottom: float, start: np.ndarray, end: np.ndarray) -> np.ndarray:
    share = np.clip((y - top) / (bottom - top), 0.0, 1.0)[..., None]
    return start + (end - start) * share


def over(canvas: np.ndarray, color: np.ndarray, alpha: np.ndarray) -> None:
    alpha = alpha[..., None]
    canvas[..., :3] = color * alpha + canvas[..., :3] * (1.0 - alpha)
    canvas[..., 3:] = alpha + canvas[..., 3:] * (1.0 - alpha)


def capsule_layer(canvas: np.ndarray, x: np.ndarray, y: np.ndarray) -> None:
    radius = (CAPSULE[3] - CAPSULE[1]) / 2.0
    distance = rounded_box(x, y, CAPSULE, radius)
    over(canvas, vertical_gradient(y, CAPSULE[1], CAPSULE[3], CAPSULE_TOP, CAPSULE_BOTTOM), coverage(distance))
    rim_band = np.clip(1.0 - np.abs(distance + 1.5) / 1.5, 0.0, 1.0)
    rim_strength = 0.06 + 0.16 * np.clip((CAPSULE[3] - y) / (CAPSULE[3] - CAPSULE[1]), 0.0, 1.0)
    over(canvas, RIM_COLOR, rim_band * rim_strength * coverage(distance))


def tile_layer(canvas: np.ndarray, x: np.ndarray, y: np.ndarray) -> None:
    distance = rounded_box(x, y, TILE, TILE_RADIUS)
    over(canvas, vertical_gradient(y, TILE[1], TILE[3], TILE_TOP, TILE_BOTTOM), coverage(distance))
    play = polygon(x, y, PLAY_POINTS) - PLAY_RADIUS
    over(canvas, PLAY_COLOR, coverage(play))


def bars_layer(canvas: np.ndarray, x: np.ndarray, y: np.ndarray) -> None:
    centre_y = (CAPSULE[1] + CAPSULE[3]) / 2.0
    last = len(BAR_LEVELS) - 1
    for index, level in enumerate(BAR_LEVELS):
        left = BAR_LEFT + index * (BAR_WIDTH + BAR_GAP)
        half_height = max(BAR_WIDTH, BAR_MAX_HEIGHT * level) / 2.0
        box = (left, centre_y - half_height, left + BAR_WIDTH, centre_y + half_height)
        color = BAR_FIRST + (BAR_LAST - BAR_FIRST) * (index / last)
        over(canvas, color, coverage(rounded_box(x, y, box, BAR_WIDTH / 2.0)))


def downsample(canvas: np.ndarray) -> np.ndarray:
    blocks = canvas.reshape(SIZE, SUPERSAMPLE, SIZE, SUPERSAMPLE, 4).mean(axis=(1, 3))
    alpha = blocks[..., 3:]
    blocks[..., :3] = np.where(alpha > 0.0, blocks[..., :3] / np.maximum(alpha, 1e-6), 0.0)
    blocks[..., 3:] *= 255.0
    return np.clip(np.rint(blocks), 0, 255).astype(np.uint8)


def main() -> None:
    x, y = grid()
    canvas = np.zeros(x.shape + (4,))
    capsule_layer(canvas, x, y)
    tile_layer(canvas, x, y)
    bars_layer(canvas, x, y)
    Image.fromarray(downsample(canvas), "RGBA").save(OUTPUT, optimize=True)
    print("icon written:", OUTPUT, OUTPUT.stat().st_size, "bytes")


main()
