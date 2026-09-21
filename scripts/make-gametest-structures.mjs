#!/usr/bin/env node
/**
 * Genere les structures NBT minimales utilisees par les GameTests
 * (`gradlew runGameTestServer`).
 *
 * Les GameTests de Forge ont besoin d'un fichier de structure sous
 * `data/<modid>/structures/<nom>.nbt`. Minecraft n'en fournit aucun
 * d'utilisable, on les fabrique donc ici (format NBT gzip, ecrit a la main :
 * aucune dependance externe).
 *
 *   node scripts/make-gametest-structures.mjs
 */
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const MOD_ROOT = path.resolve(__dirname, '..');
const OUT_DIR = path.join(MOD_ROOT, 'src/main/resources/data/academy/structures');

/** DataVersion de Minecraft 1.20.1. */
const DATA_VERSION = 3465;

// --- petit ecrivain NBT -----------------------------------------------------
class NbtWriter {
  constructor() {
    this.chunks = [];
  }

  #u8(v) {
    const b = Buffer.alloc(1);
    b.writeUInt8(v & 0xff);
    this.chunks.push(b);
  }

  #i16(v) {
    const b = Buffer.alloc(2);
    b.writeInt16BE(v);
    this.chunks.push(b);
  }

  #i32(v) {
    const b = Buffer.alloc(4);
    b.writeInt32BE(v);
    this.chunks.push(b);
  }

  #name(s) {
    const utf8 = Buffer.from(s, 'utf8');
    this.#i16(utf8.length);
    this.chunks.push(utf8);
  }

  rootCompoundStart() {
    this.#u8(0x0a); // TAG_Compound
    this.#name(''); // nom racine vide
  }

  rootCompoundEnd() {
    this.#u8(0x00); // TAG_End
  }

  int(name, value) {
    this.#u8(0x03);
    this.#name(name);
    this.#i32(value);
  }

  string(name, value) {
    this.#u8(0x08);
    this.#name(name);
    this.#name(value);
  }

  /** TAG_List de TAG_Int. */
  intList(name, values) {
    this.#u8(0x09);
    this.#name(name);
    this.#u8(0x03); // type des elements : TAG_Int
    this.#i32(values.length);
    for (const v of values) this.#i32(v);
  }

  /** TAG_List de TAG_Compound, construite a partir de callbacks. */
  compoundList(name, elements) {
    this.#u8(0x09);
    this.#name(name);
    this.#u8(0x0a); // type des elements : TAG_Compound
    this.#i32(elements.length);
    for (const build of elements) {
      build(this); // le callback ecrit les champs...
      this.#u8(0x00); // ...puis on ferme le compound
    }
  }

  /** TAG_Compound imbrique (hors racine). */
  compound(name, build) {
    this.#u8(0x0a);
    this.#name(name);
    build(this);
    this.#u8(0x00);
  }

  toBuffer() {
    return Buffer.concat(this.chunks);
  }
}

/**
 * Structure de test carree, entierement en air.
 * @param {number} size taille du cube (X = Y = Z)
 */
function buildAirStructure(size) {
  const w = new NbtWriter();
  w.rootCompoundStart();
  w.int('DataVersion', DATA_VERSION);
  w.intList('size', [size, size, size]);
  w.compoundList('palette', [
    (w2) => w2.string('Name', 'minecraft:air'),
  ]);
  w.compoundList('blocks', []);
  w.compoundList('entities', []);
  w.rootCompoundEnd();
  return w.toBuffer();
}

/**
 * Structure de test avec un sol plein en pierre et de l'air au-dessus.
 * Utile si un test a besoin de poser des blocs.
 * @param {number} size largeur/profondeur
 * @param {number} height hauteur totale (le sol occupe y = 0)
 * @param {string} floorBlock id du bloc de sol
 */
function buildPlatformStructure(size, height, floorBlock) {
  const w = new NbtWriter();
  w.rootCompoundStart();
  w.int('DataVersion', DATA_VERSION);
  w.intList('size', [size, height, size]);
  w.compoundList('palette', [
    (w2) => w2.string('Name', 'minecraft:air'),
    (w2) => w2.string('Name', floorBlock),
  ]);
  const blocks = [];
  for (let x = 0; x < size; x++) {
    for (let z = 0; z < size; z++) {
      blocks.push((w2) => {
        w2.intList('pos', [x, 0, z]);
        w2.int('state', 1); // index 1 dans la palette = le sol
      });
    }
  }
  w.compoundList('blocks', blocks);
  w.compoundList('entities', []);
  w.rootCompoundEnd();
  return w.toBuffer();
}

// --- generation -------------------------------------------------------------
if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });

const structures = [
  // `empty` : le plus petit volume possible, suffisant pour les tests de donnees
  // (recipes, worldgen, loot tables) qui n'ont pas besoin de terrain.
  { name: 'empty', buffer: buildAirStructure(3) },
  // `platform` : sol en pierre 7x7, pour les futurs tests de blocs.
  { name: 'platform', buffer: buildPlatformStructure(7, 4, 'minecraft:stone') },
];

for (const { name, buffer } of structures) {
  const gz = zlib.gzipSync(buffer, { level: 9 });
  const file = path.join(OUT_DIR, `${name}.nbt`);
  fs.writeFileSync(file, gz);
  console.log(`ecrit : ${path.relative(MOD_ROOT, file)}  (${gz.length} octets)`);
}

// --- verification : on relit ce qu'on vient d'ecrire -------------------------
console.log('');
for (const { name } of structures) {
  const file = path.join(OUT_DIR, `${name}.nbt`);
  const raw = zlib.gunzipSync(fs.readFileSync(file));
  const magic = raw[0];
  const sizeIdx = raw.indexOf(Buffer.from('size', 'utf8'));
  const values = [];

  if (magic !== 0x0a) {
    console.log(`  FAIL ${name} : l'entete n'est pas un TAG_Compound (0x${magic.toString(16)})`);
    process.exitCode = 1;
    continue;
  }
  // juste apres le nom "size" : elemType(1) + count(4) puis les 3 ints
  const firstInt = sizeIdx + 'size'.length + 1 + 4;
  for (let i = 0; i < 3; i++) {
    values.push(raw.readInt32BE(firstInt + i * 4));
  }
  const ok = values.every((v) => v > 0);
  console.log(`  ${ok ? 'OK  ' : 'FAIL'} ${name} : size = [${values.join(', ')}]`);
  if (!ok) process.exitCode = 1;
}
