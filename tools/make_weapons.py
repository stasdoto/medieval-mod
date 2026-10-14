"""
Draws the 16x16 item pictures of the medieval weapons (pixel art, drawn here from scratch).
Run: python3 tools/make_weapons.py   (writes src/main/resources/assets/medieval/textures/item/*.png)
Each weapon is drawn diagonally like vanilla tools: the hilt at the bottom-left, the point at the top-right.
"""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'medieval', 'textures', 'item')
os.makedirs(OUT, exist_ok=True)

STEEL = (196, 202, 210, 255)
STEEL_D = (120, 126, 136, 255)
STEEL_L = (236, 240, 245, 255)
IRON = (96, 98, 104, 255)
WOOD = (120, 82, 46, 255)
WOOD_D = (84, 56, 30, 255)
LEATHER = (92, 58, 36, 255)
BRASS = (204, 160, 60, 255)
BRASS_D = (150, 108, 34, 255)
RED = (140, 36, 30, 255)


def img():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def diag(im, x0, y0, x1, y1, color, width=1):
    """Pixels along the line from (x0,y0) to (x1,y1)."""
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        x = round(x0 + (x1 - x0) * i / max(1, n))
        y = round(y0 + (y1 - y0) * i / max(1, n))
        for w in range(width):
            if 0 <= x + w < 16 and 0 <= y < 16:
                im.putpixel((x + w, y), color)


def put(im, pts, color):
    for x, y in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            im.putpixel((x, y), color)


def sword(name, guard, pommel, grip, blade_len=11, edge=STEEL, fuller=STEEL_D):
    im = img()
    # blade from (4,11) to (4+L, 11-L)
    for i in range(blade_len):
        x, y = 4 + i, 11 - i
        put(im, [(x, y)], fuller)
        put(im, [(x + 1, y)], edge)
        put(im, [(x, y - 1)], STEEL_L)
    put(im, [(4 + blade_len, 11 - blade_len)], STEEL_L)
    # cross-guard perpendicular to the blade
    put(im, [(2, 10), (3, 11), (4, 12), (5, 13)], guard)
    # grip
    put(im, [(2, 13), (3, 12)], grip)
    put(im, [(1, 14)], grip)
    # pommel
    put(im, pommel, BRASS if guard == BRASS else IRON)
    im.save(os.path.join(OUT, name + '.png'))


sword('carolingian_sword', BRASS, [(0, 15), (1, 15), (0, 14)], LEATHER)
sword('norman_sword', IRON, [(0, 15), (0, 14), (1, 15)], WOOD_D, blade_len=11)


def sabre():
    im = img()
    # a curved blade
    pts = [(4, 12), (5, 11), (6, 10), (7, 9), (8, 9), (9, 8), (10, 7), (11, 6), (12, 5), (12, 4), (13, 3), (13, 2), (14, 1)]
    for x, y in pts:
        put(im, [(x, y)], STEEL)
        put(im, [(x, y + 1)], STEEL_D)
    put(im, [(14, 1)], STEEL_L)
    put(im, [(3, 11), (4, 13), (2, 12)], BRASS)
    put(im, [(2, 13), (1, 14)], LEATHER)
    put(im, [(0, 15)], BRASS_D)
    im.save(os.path.join(OUT, 'sabre.png'))


sabre()


def haft(im, x0, y0, x1, y1):
    diag(im, x0, y0, x1, y1, WOOD)
    diag(im, x0 + 1, y0, x1 + 1, y1, WOOD_D)


def bearded_axe():
    im = img()
    haft(im, 1, 15, 11, 5)
    # head: bearded blade hanging below the edge
    put(im, [(9, 3), (10, 3), (11, 4), (12, 4), (12, 5), (12, 6), (13, 6), (13, 7), (12, 8), (11, 9)], STEEL)
    put(im, [(13, 3), (14, 4), (14, 5), (14, 6), (14, 7), (14, 8), (13, 9), (12, 10)], STEEL_L)
    put(im, [(11, 5), (11, 6), (11, 7), (10, 8)], STEEL_D)
    put(im, [(10, 5), (9, 6)], IRON)
    im.save(os.path.join(OUT, 'bearded_axe.png'))


bearded_axe()


def dane_axe():
    im = img()
    haft(im, 0, 15, 12, 3)
    put(im, [(11, 0), (12, 0), (13, 1), (14, 1), (15, 2), (15, 3), (15, 4), (15, 5), (14, 6), (13, 7)], STEEL_L)
    put(im, [(11, 1), (12, 1), (12, 2), (13, 2), (14, 2), (13, 3), (14, 3), (14, 4), (13, 4), (14, 5), (13, 5), (13, 6), (12, 6)], STEEL)
    put(im, [(11, 2), (12, 3), (12, 4), (12, 5), (11, 6)], STEEL_D)
    im.save(os.path.join(OUT, 'dane_axe.png'))


dane_axe()


def mace(name, flanged):
    im = img()
    haft(im, 1, 15, 9, 7)
    if flanged:
        # шестопёр: six flanges
        put(im, [(9, 4), (10, 3), (11, 4), (12, 5), (13, 6), (12, 7), (11, 8), (10, 9), (9, 8), (8, 7), (9, 6), (10, 5)], STEEL)
        put(im, [(10, 4), (11, 5), (12, 6), (11, 7), (10, 8), (9, 7), (10, 6), (11, 6), (10, 7)], STEEL_D)
        put(im, [(10, 2), (14, 6), (10, 10), (7, 6)], STEEL_L)
    else:
        # булава: a round head with knobs
        put(im, [(9, 4), (10, 4), (11, 4), (8, 5), (12, 5), (8, 6), (12, 6), (8, 7), (12, 7), (9, 8), (10, 8), (11, 8)], IRON)
        put(im, [(9, 5), (10, 5), (11, 5), (9, 6), (11, 6), (9, 7), (10, 7), (11, 7)], STEEL_D)
        put(im, [(10, 6)], STEEL)
        put(im, [(10, 3), (13, 6), (10, 9), (7, 6), (12, 3), (12, 8)], STEEL_L)
    im.save(os.path.join(OUT, name + '.png'))


mace('mace', False)
mace('shestoper', True)


def flail():
    im = img()
    haft(im, 1, 15, 7, 9)
    # chain then a weight
    put(im, [(8, 8), (9, 7), (10, 7), (11, 6), (12, 6)], IRON)
    put(im, [(12, 3), (13, 3), (11, 4), (12, 4), (13, 4), (14, 4), (12, 5), (13, 5)], STEEL_D)
    put(im, [(13, 2), (15, 4), (12, 6), (11, 3)], STEEL_L)
    im.save(os.path.join(OUT, 'flail.png'))


flail()


def spear(name, length, head):
    im = img()
    haft(im, 0, 15, length, 15 - length)
    x, y = length + 1, 14 - length
    put(im, [(x, y), (x + 1, y - 1), (x, y - 1), (x + 1, y)], STEEL)
    put(im, [(x + 2, y - 2)], STEEL_L)
    if head:
        put(im, [(x - 1, y), (x, y + 1)], STEEL_D)
        put(im, [(x + 2, y - 1), (x + 1, y - 2)], STEEL_L)
    im.save(os.path.join(OUT, name + '.png'))


spear('spear', 11, True)
spear('sulitsa', 9, False)


def knight_lance():
    im = img()
    diag(im, 0, 15, 13, 2, (210, 210, 200, 255))
    diag(im, 1, 15, 14, 2, (170, 40, 34, 255))
    put(im, [(14, 1), (15, 0)], STEEL_L)
    put(im, [(3, 13), (4, 12), (2, 12), (3, 11)], IRON)
    im.save(os.path.join(OUT, 'lance.png'))


knight_lance()

# The silver grivna: a silver ingot bar, the money of Rus.
im = img()
SIL = (210, 214, 220, 255)
SIL_D = (150, 156, 166, 255)
SIL_L = (246, 248, 250, 255)
for x in range(3, 13):
    for y in range(6, 10):
        im.putpixel((x, y), SIL)
for x in range(3, 13):
    im.putpixel((x, 9), SIL_D)
    im.putpixel((x, 6), SIL_L)
im.putpixel((2, 7), SIL_D)
im.putpixel((2, 8), SIL_D)
im.putpixel((13, 7), SIL_D)
im.putpixel((13, 8), SIL_D)
for x in (5, 8, 11):
    im.putpixel((x, 7), SIL_D)
im.save(os.path.join(OUT, 'grivna.png'))

print('ok', sorted(os.listdir(OUT)))
