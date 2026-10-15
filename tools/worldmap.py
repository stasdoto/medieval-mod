#!/usr/bin/env python3
"""
Renders the map of the Old World for the in-game atlas and the online map: relief from the same elevation tiles the
mod uses (Terrarium, AWS Open Data), coasts, lakes and rivers from Natural Earth, drawn like an old parchment map, in the
mod's own projection (equirectangular, longitudes squeezed by cos 47 degrees, north up) so that a pixel maps straight
to block coordinates.

Usage: python3 worldmap.py OUT_DIR   (run in CI: it downloads ~40 MB)
"""
import io
import json
import math
import os
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor

import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy import ndimage

OUT = sys.argv[1] if len(sys.argv) > 1 else "worldmap"
LON0, LON1, LAT0, LAT1 = -26.0, 92.0, 4.0, 72.0
REF = math.cos(math.radians(47.0))
WIDTH = 5600
HEIGHT = int(round(WIDTH * (LAT1 - LAT0) / ((LON1 - LON0) * REF)))
Z = 6
NE = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/"


def get(url, tries=4):
    for t in range(tries):
        try:
            with urllib.request.urlopen(url, timeout=120) as r:
                return r.read()
        except Exception as e:  # noqa: BLE001
            err = e
    raise err


def tile(x, y):
    data = get(f"https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{Z}/{x}/{y}.png")
    a = np.asarray(Image.open(io.BytesIO(data)).convert("RGB"), dtype=np.float32)
    return (x, y, a[:, :, 0] * 256 + a[:, :, 1] + a[:, :, 2] / 256 - 32768)


def merc_y(lat):
    s = math.radians(lat)
    return (1 - math.log(math.tan(s) + 1 / math.cos(s)) / math.pi) / 2 * (1 << Z)


def main():
    os.makedirs(OUT, exist_ok=True)
    n = 1 << Z
    tx0, tx1 = int((LON0 + 180) / 360 * n), int((LON1 + 180) / 360 * n)
    ty0, ty1 = int(merc_y(LAT1)), int(merc_y(LAT0))
    jobs = [(x, y) for x in range(tx0, tx1 + 1) for y in range(ty0, ty1 + 1)]
    print(f"{len(jobs)} tiles, map {WIDTH}x{HEIGHT}", flush=True)
    mosaic = np.zeros(((ty1 - ty0 + 1) * 256, (tx1 - tx0 + 1) * 256), dtype=np.float32)
    with ThreadPoolExecutor(16) as ex:
        for x, y, a in ex.map(lambda j: tile(*j), jobs):
            mosaic[(y - ty0) * 256:(y - ty0 + 1) * 256, (x - tx0) * 256:(x - tx0 + 1) * 256] = a
    # Resample into the mod's projection.
    lons = LON0 + (np.arange(WIDTH) + 0.5) / WIDTH * (LON1 - LON0)
    lats = LAT1 - (np.arange(HEIGHT) + 0.5) / HEIGHT * (LAT1 - LAT0)
    px = ((lons + 180) / 360 * n - tx0) * 256 - 0.5
    rad = np.radians(lats)
    py = ((1 - np.log(np.tan(rad) + 1 / np.cos(rad)) / math.pi) / 2 * n - ty0) * 256 - 0.5
    gy, gx = np.meshgrid(py, px, indexing="ij")
    elev = ndimage.map_coordinates(mosaic, [gy, gx], order=1, mode="nearest").astype(np.float32)
    del mosaic

    def to_px(lon, lat):
        return ((lon - LON0) / (LON1 - LON0) * WIDTH, (LAT1 - lat) / (LAT1 - LAT0) * HEIGHT)

    def polys(geom):
        if geom["type"] == "Polygon":
            yield geom["coordinates"]
        elif geom["type"] == "MultiPolygon":
            yield from geom["coordinates"]

    def lines(geom):
        if geom["type"] == "LineString":
            yield geom["coordinates"]
        elif geom["type"] == "MultiLineString":
            yield from geom["coordinates"]

    def inside(coords):
        xs = [c[0] for c in coords]
        ys = [c[1] for c in coords]
        return max(xs) >= LON0 - 5 and min(xs) <= LON1 + 5 and max(ys) >= LAT0 - 5 and min(ys) <= LAT1 + 5

    # Land and lakes.
    land = Image.new("L", (WIDTH, HEIGHT), 0)
    d = ImageDraw.Draw(land)
    for f in json.loads(get(NE + "ne_10m_land.geojson"))["features"]:
        for poly in polys(f["geometry"]):
            if not inside(poly[0]):
                continue
            d.polygon([to_px(*c[:2]) for c in poly[0]], fill=255)
            for hole in poly[1:]:
                d.polygon([to_px(*c[:2]) for c in hole], fill=0)
    for f in json.loads(get(NE + "ne_10m_lakes.geojson"))["features"]:
        name = (f["properties"].get("name") or "")
        # Modern reservoirs did not exist in 1050.
        if "Reservoir" in name or "Vodokhranilishche" in name or "Vdkhr" in name or f["properties"].get("featurecla") == "Reservoir":
            continue
        for poly in polys(f["geometry"]):
            if inside(poly[0]):
                d.polygon([to_px(*c[:2]) for c in poly[0]], fill=0)
    land = np.asarray(land) > 127
    water = ~land

    # Shading.
    e = np.where(land, np.maximum(elev, 0), elev)
    gyy, gxx = np.gradient(ndimage.gaussian_filter(e, 1.0))
    cell = (LAT1 - LAT0) / HEIGHT * 111320
    slope = np.arctan(np.hypot(gxx, gyy) / cell * 6.0)
    aspect = np.arctan2(-gxx, gyy)
    az, alt = math.radians(315), math.radians(42)
    shade = np.sin(alt) * np.cos(slope) + np.cos(alt) * np.sin(slope) * np.cos(az - aspect)
    shade = np.clip(shade, 0, 1)

    # Parchment land: lowland straw, hills ochre, mountains umber, snowy peaks.
    stops = [(-500, (214, 204, 170)), (0, (224, 212, 172)), (150, (219, 203, 158)), (400, (207, 186, 139)), (900, (188, 160, 116)),
             (1800, (160, 132, 98)), (2800, (176, 160, 140)), (4200, (236, 232, 224))]
    h = np.clip(e, -500, 4200)
    rgb = np.zeros((HEIGHT, WIDTH, 3), dtype=np.float32)
    for i in range(len(stops) - 1):
        a0, c0 = stops[i]
        a1, c1 = stops[i + 1]
        m = (h >= a0) & (h <= a1)
        t = ((h - a0) / (a1 - a0))[m][:, None]
        rgb[m] = np.array(c0) * (1 - t) + np.array(c1) * t
    light = 0.55 + 0.6 * shade
    rgb *= light[:, :, None]

    # Sea: grey-blue, deeper is darker, with old-map ripple lines along the coast.
    depth = np.clip(-elev, 0, 5000)
    sea = np.stack([158 - depth / 5000 * 46, 178 - depth / 5000 * 40, 180 - depth / 5000 * 26], axis=-1)
    dist = ndimage.distance_transform_edt(water)
    for k, r in enumerate([3.0, 7.0, 12.0]):
        ring = (np.abs(dist - r) < 0.7) & water
        sea[ring] *= 0.84 + k * 0.04
    rgb = np.where(water[:, :, None], sea, rgb)
    coast = water & (dist <= 1.2)
    rgb[coast] = (72, 70, 62)

    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB")
    dr = ImageDraw.Draw(img)
    # Rivers.
    for src in ["ne_10m_rivers_lake_centerlines.geojson", "ne_10m_rivers_europe.geojson"]:
        try:
            feats = json.loads(get(NE + src))["features"]
        except Exception as ex:  # noqa: BLE001
            print("skip", src, ex)
            continue
        for f in feats:
            rank = f["properties"].get("scalerank", 6) or 6
            w = 3 if rank <= 3 else 2 if rank <= 6 else 1
            for ln in lines(f["geometry"]):
                if len(ln) > 1 and inside(ln):
                    dr.line([to_px(*c[:2]) for c in ln], fill=(84, 112, 124), width=w, joint="curve")
    # Graticule.
    for lon in range(-20, 91, 10):
        x = to_px(lon, 0)[0]
        dr.line([(x, 0), (x, HEIGHT)], fill=(120, 110, 90), width=1)
    for lat in range(10, 71, 10):
        y = to_px(0, lat)[1]
        dr.line([(0, y), (WIDTH, y)], fill=(120, 110, 90), width=1)
    img = img.filter(ImageFilter.UnsharpMask(radius=1.2, percent=40, threshold=2))
    img.save(os.path.join(OUT, "relief.jpg"), quality=86, optimize=True, progressive=True)
    img.resize((WIDTH // 4, HEIGHT // 4), Image.LANCZOS).save(os.path.join(OUT, "relief-small.jpg"), quality=85)
    with open(os.path.join(OUT, "meta.json"), "w") as fh:
        json.dump({"lon0": LON0, "lon1": LON1, "lat0": LAT0, "lat1": LAT1, "width": WIDTH, "height": HEIGHT}, fh)
    print("done", os.path.getsize(os.path.join(OUT, "relief.jpg")))


if __name__ == "__main__":
    main()
