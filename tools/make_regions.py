"""
Builds src/main/resources/medieval/geo/regions.bin.gz from Natural Earth (public domain, naturalearthdata.com):
climate regions (deserts, steppes, tundra), modern reservoirs (removed from the world of 1050) and a few hand-drawn
regions (great marshes of the 11th century).

Usage: python3 tools/make_regions.py <folder with the Natural Earth geojson files>
Format (big-endian): int count; per polygon: byte kind, short nameLen, utf8 name, int rings; per ring: int n, n x (int lon*1e6, int lat*1e6)
"""
import gzip, json, struct, sys, os

DESERT, STEPPE, TUNDRA, RESERVOIR, MARSH, ARID = 1, 2, 3, 4, 5, 6
src = sys.argv[1]
out = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'medieval', 'geo', 'regions.bin.gz')

polys = []  # (kind, name, [rings])

def add_geom(kind, name, g):
    if g is None:
        return
    if g['type'] == 'Polygon':
        polys.append((kind, name, g['coordinates']))
    elif g['type'] == 'MultiPolygon':
        for p in g['coordinates']:
            polys.append((kind, name, p))

regions = json.load(open(os.path.join(src, 'ne_10m_geography_regions_polys.geojson')))
STEPPES = {'PONTIC STEPPE', 'KAZAKH STEPPE', 'KAZAKH UPLAND', 'TURAN LOWLAND', 'SAHEL', 'MONGOLIAN PLATEAU', 'Masai Steppe',
           'Iwembere Steppe', 'GREAT PLAINS', 'PAMPAS', 'Alföld', 'Stavropol’ Plateau', 'Ustyurt Plateau', 'Hasa Plain'}
ARIDS = {'PLATEAU OF TIBET', 'Kavir Desert', 'LUT DESERT', 'COLORADO PLATEAU', 'Loess Plateau', 'Great Karroo', 'Northern Karroo'}
for f in regions['features']:
    p = f['properties']
    cla, name = p.get('FEATURECLA'), p.get('NAME')
    if cla == 'Desert':
        add_geom(DESERT, name, f['geometry'])
    elif cla == 'Tundra':
        add_geom(TUNDRA, name, f['geometry'])
    elif name in STEPPES:
        add_geom(STEPPE, name, f['geometry'])
    elif name in ARIDS:
        add_geom(ARID, name, f['geometry'])

for fn in ('ne_10m_lakes.geojson', 'ne_10m_lakes_europe.geojson'):
    for f in json.load(open(os.path.join(src, fn)))['features']:
        if f['properties'].get('featurecla') == 'Reservoir':
            add_geom(RESERVOIR, f['properties'].get('name') or 'reservoir', f['geometry'])

# Hand-drawn: the great marshes of the 11th century (lon, lat).
MARSHES = {
    'Polesia (Pinsk Marshes)': [(23.6, 51.6), (24.6, 52.5), (26.2, 52.6), (27.8, 52.4), (29.4, 52.0), (30.2, 51.5), (29.0, 51.1),
                                (27.6, 51.3), (26.2, 51.2), (24.8, 51.2)],
    'Meshchera': [(39.5, 55.6), (40.6, 55.9), (41.8, 55.6), (41.3, 55.0), (40.0, 54.9)],
    'Fens (East Anglia)': [(-0.4, 52.9), (0.3, 52.95), (0.5, 52.5), (0.2, 52.2), (-0.3, 52.4)],
}
for name, ring in MARSHES.items():
    polys.append((MARSH, name, [[list(p) for p in ring] + [list(ring[0])]]))

with gzip.open(out, 'wb') as o:
    o.write(struct.pack('>i', len(polys)))
    pts = 0
    for kind, name, rings in polys:
        nb = (name or '').encode('utf-8')
        o.write(struct.pack('>bh', kind, len(nb)))
        o.write(nb)
        o.write(struct.pack('>i', len(rings)))
        for r in rings:
            o.write(struct.pack('>i', len(r)))
            pts += len(r)
            o.write(b''.join(struct.pack('>ii', int(round(x * 1e6)), int(round(y * 1e6))) for x, y, *_ in r))
print(len(polys), 'polygons', pts, 'points ->', out, os.path.getsize(out), 'bytes')
