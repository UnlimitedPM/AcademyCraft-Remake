#!/usr/bin/env node
/**
 * Genere la boule de plasma du canon : un disque BLANC, a bord doux.
 *
 * L'original n'avait pas cette image : son nuanceur calculait la forme, une densite en
 * `1 / distance` au carre qui s'eteint doucement et n'a donc aucun bord. Le port, lui, dessine
 * ses boules avec une image, et il lui en faut une qui soit :
 *
 * - BLANCHE, parce qu'une couleur ne s'obtient qu'en la teintant, et qu'une image deja teintee ne
 *   peut pas devenir bleue PUIS rose ;
 * - DOUCE, parce qu'un disque a bord net se lit comme une pastille et non comme du plasma ;
 * - RADIALE, parce que la densite de l'original ne dependait que de la distance au centre.
 *
 * Le profil est `(1 - r) au carre` : plein au centre, et une queue douce jusqu'au bord.
 *
 * Aucune dependance, comme le reste des scripts du depot : le PNG est ecrit a la main (en-tete,
 * CRC32 et flux zlib, que Node fournit).
 *
 *   node scripts/make-plasma-ball.mjs
 */
import { deflateSync } from "node:zlib";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const OUT = resolve(ROOT, "src/main/resources/assets/academy/textures/effects/plasma_ball.png");

/** La taille de l'image : assez fine pour un bord doux, assez petite pour ne rien peser. */
const SIZE = 64;

/** Le profil d'opacite : plein au centre, nul au bord, et une queue douce entre les deux. */
function alphaAt(x, y) {
    const dx = ((x + 0.5) / SIZE) * 2 - 1;
    const dy = ((y + 0.5) / SIZE) * 2 - 1;
    const r = Math.hypot(dx, dy);
    return r >= 1 ? 0 : Math.round(255 * (1 - r) ** 2);
}

/** Les pixels : RVB a 255 partout, et l'opacite du profil. */
function pixels() {
    const rows = [];
    for (let y = 0; y < SIZE; y++) {
        const row = Buffer.alloc(1 + SIZE * 4);
        row[0] = 0; // filtre « aucun », ligne par ligne
        for (let x = 0; x < SIZE; x++) {
            const at = 1 + x * 4;
            row[at] = 255;
            row[at + 1] = 255;
            row[at + 2] = 255;
            row[at + 3] = alphaAt(x, y);
        }
        rows.push(row);
    }
    return Buffer.concat(rows);
}

const CRC_TABLE = (() => {
    const table = new Int32Array(256);
    for (let n = 0; n < 256; n++) {
        let c = n;
        for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
        table[n] = c;
    }
    return table;
})();

function crc32(buffer) {
    let c = 0xffffffff;
    for (const byte of buffer) c = CRC_TABLE[(c ^ byte) & 0xff] ^ (c >>> 8);
    return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
    const head = Buffer.alloc(4);
    head.writeUInt32BE(data.length);
    const body = Buffer.concat([Buffer.from(type, "ascii"), data]);
    const tail = Buffer.alloc(4);
    tail.writeUInt32BE(crc32(body));
    return Buffer.concat([head, body, tail]);
}

function png() {
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(SIZE, 0);
    ihdr.writeUInt32BE(SIZE, 4);
    ihdr[8] = 8; // huit bits par canal
    ihdr[9] = 6; // RGBA
    ihdr[10] = 0; // compression standard
    ihdr[11] = 0; // filtre standard
    ihdr[12] = 0; // pas d'entrelacement

    return Buffer.concat([
        Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
        chunk("IHDR", ihdr),
        chunk("IDAT", deflateSync(pixels(), { level: 9 })),
        chunk("IEND", Buffer.alloc(0)),
    ]);
}

mkdirSync(dirname(OUT), { recursive: true });
writeFileSync(OUT, png());
console.log(`ecrit ${OUT} (${SIZE}x${SIZE})`);
