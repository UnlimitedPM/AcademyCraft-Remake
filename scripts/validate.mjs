#!/usr/bin/env node
/**
 * AcademyCraft :: validateur offline.
 *
 * Analyse STATIQUEMENT les sources Java (registres Forge) et les ressources
 * (assets / data) pour detecter ce qui manque ou est casse, SANS lancer Minecraft.
 *
 *   node scripts/validate.mjs            -> rapport lisible
 *   node scripts/validate.mjs --strict   -> code de sortie 1 s'il y a un probleme
 *   node scripts/validate.mjs --json     -> sortie JSON (pour CI)
 *
 * Zero dependance externe.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const MOD_ROOT = path.resolve(__dirname, '..');

const ARGS = new Set(process.argv.slice(2));
const STRICT = ARGS.has('--strict');
const AS_JSON = ARGS.has('--json');

const SRC = path.join(MOD_ROOT, 'src/main/java/cn/academy');
const RES = path.join(MOD_ROOT, 'src/main/resources');
const ASSETS = path.join(RES, 'assets/academy');
const DATA = path.join(RES, 'data/academy');
const MODID = 'academy';

// ---------------------------------------------------------------------------
// Utilitaires
// ---------------------------------------------------------------------------
const findings = [];
const add = (level, category, name, detail = '') =>
  findings.push({ level, category, name, detail });

const readText = (p) => {
  try {
    return fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, '');
  } catch {
    return null;
  }
};

const readJson = (p) => {
  const raw = readText(p);
  if (raw === null) return { missing: true };
  try {
    return { value: JSON.parse(raw), raw };
  } catch (e) {
    return { broken: e.message, raw };
  }
};

const exists = (p) => fs.existsSync(p);
const norm = (p) => p.replace(/\\/g, '/');

function walk(dir, filter = () => true, out = []) {
  if (!exists(dir)) return out;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, e.name);
    if (e.isDirectory()) walk(full, filter, out);
    else if (filter(full)) out.push(full);
  }
  return out;
}

// ---------------------------------------------------------------------------
// 1. Extraction des identifiants enregistres dans les sources Java
// ---------------------------------------------------------------------------
const REGISTER_CALL =
  /\b(?:register|registerBlock|registerItem|registerSimpleItem|registerBlockItem|registerNoItem|registerBasicItem|registerMachineItem|registerMachineBlock|registerBlockEntity|registerMenu|registerFluid|registerType|blockItem|simpleItem|machineItem)\s*\(\s*"([a-z][a-z0-9_]*)"/g;

// registerBlock(...) cree implicitement un BlockItem (voir ModBlocks.registerBlockItem).
const AUTO_ITEM_HELPERS = new Set(['registerBlock', 'registerMachineBlock', 'registerBlockItem']);
const AUTO_ITEM_CALL =
  /\b(?:registerBlock|registerMachineBlock|registerBlockItem)\s*\(\s*"([a-z][a-z0-9_]*)"/g;

function scanJava(fileName) {
  const src = readText(path.join(SRC, fileName)) ?? '';
  const names = new Set();
  const autoItems = new Set();
  for (const line of src.split(/\r?\n/)) {
    if (/^\s*(?:\/\/|\*|\/\*)/.test(line)) continue; // ignore les commentaires
    for (const m of line.matchAll(REGISTER_CALL)) names.add(m[1]);
    for (const m of line.matchAll(AUTO_ITEM_CALL)) autoItems.add(m[1]);
  }
  return { names, autoItems, src };
}

const blocksSrc = scanJava('ModBlocks.java');
const itemsSrc = scanJava('ModItems.java');

const blocks = blocksSrc.names;
const items = itemsSrc.names;
const autoBlockItems = new Set([...blocksSrc.autoItems, ...itemsSrc.autoItems]);
const blockEntities = scanJava('ModBlockEntities.java').names;
const menus = scanJava('ModMenus.java').names;
const fluidRegs = scanJava('ModFluids.java').names;
const tabs = scanJava('ModCreativeTabs.java').names;

// FLUIDS.register(...) et FLUID_TYPES.register(...) sont dans le meme fichier :
// seuls les seconds portent une cle de langue.
const fluidTypes = new Set();
{
  const fluidsSrc = readText(path.join(SRC, 'ModFluids.java')) ?? '';
  for (const line of fluidsSrc.split(/\r?\n/)) {
    const m = line.match(/\bFLUID_TYPES\.register\(\s*"([a-z][a-z0-9_]*)"/);
    if (m) fluidTypes.add(m[1]);
  }
}

// Bloc de fluide : pas d'item, pas de blockstate, pas de loot table.
const blocksNeedingNoItem = new Set(['phase_liquid']);

// Blocs qui n'ont pas d'item associe (probablement un oubli dans ModItems)
const blockItems = new Set([...items, ...autoBlockItems]);
const isBlockItem = (id) => blockItems.has(id) && blocks.has(id);
for (const b of [...blocks].sort()) {
  if (blocksNeedingNoItem.has(b)) continue;
  if (!blockItems.has(b)) {
    add('INFO', 'bloc sans item', b, 'aucun BlockItem detecte (ni ModItems, ni registerBlock)');
  }
}

// Tout item a besoin d'un modele d'item, y compris les BlockItem crees
// implicitement par registerBlock : models/item/<bloc>.json avec un parent vers
// academy:block/<bloc>.
const needsItemModel = new Set([...items, ...autoBlockItems]);


// ---------------------------------------------------------------------------
// 2. blockstates
// ---------------------------------------------------------------------------
const blockstateDir = path.join(ASSETS, 'blockstates');
for (const b of [...blocks].sort()) {
  const p = path.join(blockstateDir, `${b}.json`);
  if (!exists(p)) add('MANQUANT', 'blockstate', b, p);
  else add('OK', 'blockstate', b);
}

// ---------------------------------------------------------------------------
// 3. models/item
// ---------------------------------------------------------------------------
const itemModelDir = path.join(ASSETS, 'models/item');
for (const i of [...needsItemModel].sort()) {
  const p = path.join(itemModelDir, `${i}.json`);
  if (!exists(p)) add('MANQUANT', 'model/item', i, p);
  else add('OK', 'model/item', i);
}

// Blockstates orphelins : un blockstate qui ne correspond a aucun bloc enregistre.
for (const file of walk(blockstateDir, (f) => f.endsWith('.json'))) {
  const name = path.basename(file, '.json');
  if (!blocks.has(name)) add('INFO', 'blockstate orphelin', name);
}

// ---------------------------------------------------------------------------
// 4. Resolution des modeles + textures
// ---------------------------------------------------------------------------
const modelRefs = new Set(); // "academy:block/foo" -> deja resolus
const textureRefs = new Map(); // chemin texture -> Set de sources
const unresolved = [];

function resolveModelRef(refPath, fromFile, depth = 0) {
  // refPath deja sans namespace academy
  if (depth > 12) return;
  const file = path.join(ASSETS, 'models', `${refPath}.json`);
  if (!exists(file)) {
    unresolved.push({ ref: refPath, from: fromFile });
    return;
  }
  const key = refPath;
  if (modelRefs.has(key)) return;
  modelRefs.add(key);

  const parsed = readJson(file);
  if (parsed.broken) {
    add('CASSE', 'json', norm(path.relative(MOD_ROOT, file)), parsed.broken);
    return;
  }
  const model = parsed.value;

  // Le parent, mais aussi les sous-modeles references ailleurs : les entrees
  // "overrides" d'un modele d'item pointent vers d'autres modeles
  // (icone pleine / a moitie / vide, face avant / arriere d'une piece...).
  const referenced = [];
  const collectModels = (node) => {
    if (!node || typeof node !== 'object') return;
    if (Array.isArray(node)) return node.forEach(collectModels);
    for (const [key, value] of Object.entries(node)) {
      if (key === 'model' && typeof value === 'string') referenced.push(value);
      else collectModels(value);
    }
  };
  collectModels(model);
  if (typeof model.parent === 'string') referenced.push(model.parent);

  for (const ref of referenced) {
    if (ref.startsWith(`minecraft:`) || ref.startsWith('forge:')) continue;
    // Les modeles OBJ de Forge sont references avec leur extension
    // ("academy:models/ip_gen.obj") : ce ne sont pas des .json a resoudre.
    if (/\.(obj|json)$/i.test(ref)) continue;
    if (ref.startsWith(`${MODID}:`)) resolveModelRef(ref.slice(MODID.length + 1), file, depth + 1);
  }

  if (model.textures && typeof model.textures === 'object') {
    for (const [slot, value] of Object.entries(model.textures)) {
      const v = String(value);
      if (!v || v.startsWith('#')) continue;
      if (v.startsWith('minecraft:') || v.startsWith('forge:')) continue;
      if (!v.startsWith(`${MODID}:`)) {
        add('INFO', 'texture sans namespace', slot, `${v} (dans ${norm(path.relative(MOD_ROOT, file))})`);
        continue;
      }
      const rel = v.slice(MODID.length + 1);
      if (!textureRefs.has(rel)) textureRefs.set(rel, new Set());
      textureRefs.get(rel).add(norm(path.relative(MOD_ROOT, file)));
    }
  }
}

// Les blockstates des blocs enregistres sont les points d'entree des modeles de
// bloc. Ceux qui ne correspondent a aucun bloc sont signales a part
// ("blockstate orphelin") : ce sont des restes, Minecraft les ignore.
for (const file of walk(blockstateDir, (f) => f.endsWith('.json'))) {
  const stem = path.basename(file, '.json');
  const parsed = readJson(file);
  const name = path.basename(file);
  if (parsed.broken) {
    add('CASSE', 'json', name, parsed.broken);
    continue;
  }
  if (!blocks.has(stem)) continue; // orphelin : deja signale au §2

  const models = [];
  const collect = (node) => {
    if (!node || typeof node !== 'object') return;
    if (Array.isArray(node)) return node.forEach(collect);
    if (typeof node.model === 'string') models.push(node.model);
    Object.values(node).forEach(collect);
  };
  collect(parsed.value);
  for (const m of models) {
    if (m.startsWith(`minecraft:`) || m.startsWith('forge:')) continue;
    if (m.startsWith(`${MODID}:`)) resolveModelRef(m.slice(MODID.length + 1), file);
    else add('INFO', 'model sans namespace', m, `${name}`);
  }
}

// Les modeles d'item sont les points d'entree cote item : ils ne sont
// references par aucun JSON, mais par le registre d'items.
for (const i of [...needsItemModel].sort()) {
  resolveModelRef(`item/${i}`, path.join(itemModelDir, `${i}.json`));
}

// Modeles orphelins : fichiers jamais atteints depuis un blockstate ou un item.
// A calculer AVANT la passe de validation du §4ter, qui elle parcourt tout.
const orphans = [];
{
  const modelsRoot = path.join(ASSETS, 'models');
  for (const file of walk(modelsRoot, (f) => f.endsWith('.json'))) {
    const rel = norm(path.relative(modelsRoot, file)).replace(/\.json$/, '');
    if (!modelRefs.has(rel)) orphans.push(rel);
  }
}
for (const o of orphans.sort()) add('INFO', 'modele orphelin', o);

// Passes separees, uniquement pour valider le contenu : un modele mort dont la
// texture est cassee reste un probleme a signaler.
for (const file of walk(path.join(ASSETS, 'models'), (f) => f.endsWith('.json'))) {
  const rel = norm(path.relative(path.join(ASSETS, 'models'), file)).replace(/\.json$/, '');
  if (!modelRefs.has(rel)) resolveModelRef(rel, file);
}

for (const { ref, from } of unresolved) {
  add(
    'MANQUANT',
    'model reference',
    ref,
    `reference depuis ${norm(path.relative(MOD_ROOT, from))}`,
  );
}

const textureDirs = [ASSETS];
for (const [rel, sources] of [...textureRefs].sort()) {
  const suffixes = ['.png', '.png.mcmeta'];
  const found = suffixes.some((s) => exists(path.join(ASSETS, 'textures', rel + s)));
  if (found) add('OK', 'texture', rel);
  else
    add(
      'MANQUANT',
      'texture',
      rel,
      `textures/${rel}.png  <- ${[...sources].join(', ')}`,
    );
}

// ---------------------------------------------------------------------------
// 5. Langue
// ---------------------------------------------------------------------------
const langFile = path.join(ASSETS, 'lang/en_us.json');
const lang = readJson(langFile);
if (lang.missing) {
  add('MANQUANT', 'lang', 'en_us.json', langFile);
} else if (lang.broken) {
  add('CASSE', 'lang', 'en_us.json', lang.broken);
} else {
  const keys = new Set(Object.keys(lang.value));
  // Un BlockItem utilise "block.<id>.<name>" comme cle de traduction.
  for (const b of [...blocks].sort()) {
    const k = `block.${MODID}.${b}`;
    if (keys.has(k)) add('OK', 'lang block', b);
    else add('MANQUANT', 'lang block', b, k);
  }
  for (const i of [...needsItemModel].sort()) {
    // Un BlockItem tire sa cle de traduction du bloc ("block.academy.xxx"),
    // pas de l'item ("item.academy.xxx").
    const k = isBlockItem(i) ? `block.${MODID}.${i}` : `item.${MODID}.${i}`;
    if (keys.has(k)) add('OK', 'lang item', i);
    else add('MANQUANT', 'lang item', i, k);
  }
  // Seuls les FluidType ont une cle de langue (les ids de Fluid n'en ont pas),
  // et le code peut remplacer cette cle via descriptionId("...").
  const fluidsSrcText = readText(path.join(SRC, 'ModFluids.java')) ?? '';
  const declaredDescriptionIds = [...fluidsSrcText.matchAll(/descriptionId\(\s*"([^"]+)"/g)]
    .map((m) => m[1]);
  for (const f of [...fluidTypes].sort()) {
    const k = `fluid_type.${MODID}.${f}`;
    if (keys.has(k)) add('OK', 'lang fluid', f);
    else if (declaredDescriptionIds.some((d) => keys.has(d))) {
      add('OK', 'lang fluid', f);
    } else {
      add('INFO', 'lang fluid', f, `${k} (ou descriptionId explicite)`);
    }
  }
  for (const t of [...tabs].sort()) {
    // La cle reelle est celle passee a Component.translatable("...") dans
    // ModCreativeTabs.java : on la lit au lieu de la deviner.
    const tabSource = readText(path.join(SRC, 'ModCreativeTabs.java')) ?? '';
    const declaredKeys = [...tabSource.matchAll(/Component\.translatable\(\s*"([^"]+)"/g)].map((m) => m[1]);
    const candidates = [...new Set([
      ...declaredKeys,
      `itemGroup.${MODID}.${t}`,
      `itemGroup.${t}`,
    ])];
    if (candidates.some((c) => keys.has(c))) add('OK', 'lang tab', t);
    else add('INFO', 'lang tab', t, candidates.join(' | '));
  }
  for (const k of [...keys].sort()) {
    const m = k.match(/^(?:item|block)\.academy\.(.+)$/);
    if (m && !blocks.has(m[1]) && !items.has(m[1])) {
      add('INFO', 'lang orpheline', m[1], k);
    }
  }
}

// ---------------------------------------------------------------------------
// 6. Loot tables
// ---------------------------------------------------------------------------
const lootDir = path.join(DATA, 'loot_tables/blocks');
const defaultLoot = new Set(['phase_liquid']);
const lootFiles = new Set(
  walk(lootDir, (f) => f.endsWith('.json')).map((f) =>
    path.basename(f, '.json'),
  ),
);
for (const b of [...blocks].sort()) {
  if (defaultLoot.has(b)) continue;
  if (lootFiles.has(b)) add('OK', 'loot table', b);
  else add('MANQUANT', 'loot table', b, norm(path.relative(MOD_ROOT, path.join(lootDir, `${b}.json`))));
}
for (const l of lootFiles) {
  if (!blocks.has(l)) add('INFO', 'loot table orpheline', l);
  const parsed = readJson(path.join(lootDir, `${l}.json`));
  if (parsed.broken) add('CASSE', 'json', `${l}.json (loot)`, parsed.broken);
}

// ---------------------------------------------------------------------------
// 7. Recipes
// ---------------------------------------------------------------------------
const recipeDir = path.join(DATA, 'recipes');
const knownIds = new Set([...blocks, ...items, ...fluidRegs]);
let recipeCount = 0;
for (const file of walk(recipeDir, (f) => f.endsWith('.json'))) {
  const name = path.basename(file);
  const parsed = readJson(file);
  if (parsed.broken) {
    add('CASSE', 'recipe', name, parsed.broken);
    continue;
  }
  recipeCount++;
  const idPart = name.replace(/\.json$/, '');
  if (idPart.startsWith(MODID + '_')) {
    const target = idPart.slice(MODID.length + 1).replace(/_(from|crafting|smelting|blasting).*$/, '');
    if (!knownIds.has(target)) add('INFO', 'recipe (nom)', name, `aucun item/bloc "${target}"`);
  }
  for (const m of parsed.raw.matchAll(/"academy:([a-z][a-z0-9_]*)"/g)) {
    if (!knownIds.has(m[1])) {
      add('CASSE', 'recipe (ref)', name, `reference inconnue : academy:${m[1]}`);
    }
  }
  add('OK', 'recipe', name);
}

// ---------------------------------------------------------------------------
// 8. Worldgen (les minerais doivent etre generes)
// ---------------------------------------------------------------------------
const oreBlocks = [...blocks].filter((b) => b.endsWith('_ore'));
const worldgenDir = path.join(RES, 'data/academy/worldgen');
const biomeModifierDir = path.join(RES, 'data/academy/forge/biome_modifier');
const worldgenFiles = walk(path.join(RES, 'data'), (f) =>
  /(worldgen|biome_modifier|placed_feature|configured_feature)/.test(norm(f)),
);
if (oreBlocks.length && worldgenFiles.length === 0) {
  add(
    'MANQUANT',
    'worldgen',
    oreBlocks.join(', '),
    'aucun configured/placed feature ni biome_modifier -> les minerais ne se generent pas dans le monde',
  );
} else {
  for (const o of oreBlocks) {
    const referenced = worldgenFiles.some((f) => readText(f)?.includes(`${MODID}:${o}`));
    if (referenced) add('OK', 'worldgen', o);
    else add('MANQUANT', 'worldgen', o, 'aucun feature/biome_modifier ne reference ce minerai');
  }
}

// ---------------------------------------------------------------------------
// 9. Enregistrement des DeferredRegister (oubli frequent : le registre est
//    declare mais jamais branche sur le mod event bus).
// ---------------------------------------------------------------------------
const mainSrc = readText(path.join(SRC, 'AcademyCraft.java')) ?? '';
if (!mainSrc) {
  add('CASSE', 'AcademyCraft.java', 'introuvable', path.join(SRC, 'AcademyCraft.java'));
} else {
  for (const [label, cls] of [
    ['ModFluids', 'ModFluids'],
    ['ModBlocks', 'ModBlocks'],
    ['ModItems', 'ModItems'],
    ['ModBlockEntities', 'ModBlockEntities'],
    ['ModCreativeTabs', 'ModCreativeTabs'],
    ['ModMenus', 'ModMenus'],
    ['ConfigurableFeatureBiomeModifier', 'ConfigurableFeatureBiomeModifier'],
  ]) {
    if (new RegExp(`${cls}\\.(?:SERIALIZERS\\.)?register\\s*\\(`).test(mainSrc)) add('OK', 'registre branche', label);
    else add('CASSE', 'registre branche', label, `${cls}.register(...) absent de AcademyCraft.java -> le contenu est invisible en jeu`);
  }
  if (/Config\.SPEC/.test(mainSrc)) add('OK', 'registre branche', 'Config.SPEC');
  else add('CASSE', 'registre branche', 'Config.SPEC', 'Config.SPEC non enregistre -> aucun fichier de config cree');
}

// ---------------------------------------------------------------------------
// 9bis. Hooks publiques jamais appeles. Meme famille de bug que ci-dessus :
//       une methode d'initialisation existe mais personne ne l'appelle.
//       Les methodes @SubscribeEvent sont appelees par Forge par reflexion :
//       elles ne comptent pas.
// ---------------------------------------------------------------------------
const HOOK_METHOD = /^\s*public\s+static\s+void\s+([a-zA-Z][a-zA-Z0-9_]*)\s*\(/;
// Les tests sont invoques par le framework via reflexion, pas par du code.
const hookFiles = walk(SRC, (f) => /\.java$/.test(f) && !/[\\/]gametest[\\/]/.test(f));

const allSourceFiles = walk(SRC, (f) => f.endsWith('.java'));

for (const file of hookFiles) {
  const src = readText(file) ?? '';
  const fileLabel = norm(path.relative(SRC, file));
  const lines = src.split(/\r?\n/);

  for (let i = 0; i < lines.length; i++) {
    const m = lines[i].match(HOOK_METHOD);
    if (!m) continue;
    const name = m[1];
    if (name === 'register') continue; // deja couvert par le test 9

    // Remonte les annotations juste au-dessus pour detecter @SubscribeEvent.
    let annotated = false;
    for (let j = i - 1; j >= 0; j--) {
      const prev = lines[j].trim();
      if (prev === '') continue;
      if (prev.startsWith('@')) {
        if (/@SubscribeEvent/.test(prev)) annotated = true;
        continue;
      }
      break;
    }
    if (annotated) continue;

    // Appel direct (X.name() ) ou reference de methode (X::name).
    const outside = allSourceFiles
      .filter((f) => f !== file)
      .map((f) => readText(f) ?? '')
      .join('\n');
    const called =
      new RegExp(`(?:\\.|::)${name}\\s*(?:\\(|[,;)])`).test(outside) ||
      new RegExp(`\\b${name}\\s*\\(`).test(outside);

    if (called) add('OK', 'hook appele', `${fileLabel}#${name}`);
    else
      add(
        'CASSE',
        'hook jamais appele',
        `${fileLabel}#${name}`,
        "methode publique d'initialisation que personne n'appelle (code mort en jeu)",
      );
  }
}

// ---------------------------------------------------------------------------
// 10. mods.toml / gradle.properties
// ---------------------------------------------------------------------------
for (const toml of walk(RES, (f) => f.endsWith('mods.toml'))) {
  const txt = readText(toml) ?? '';
  // On ignore les lignes commentees (le MDK en laisse beaucoup).
  const effective = txt
    .split(/\r?\n/)
    .filter((l) => !/^\s*#/.test(l))
    .join('\n');
  const m = effective.match(/modId\s*=\s*"([^"]+)"/);
  const id = m ? m[1] : '(absent)';
  if (id === MODID) add('OK', 'mods.toml', id);
  else add('CASSE', 'mods.toml', id, `modId doit valoir "${MODID}" (AcademyCraft.MOD_ID)`);
  if (/displayName\s*=\s*"([^"]*)"/.test(effective)) {
    const dn = effective.match(/displayName\s*=\s*"([^"]*)"/)[1];
    if (/example/i.test(dn)) add('CASSE', 'mods.toml', dn, 'displayName contient encore "example"');
  }
}
const gradleProps = readText(path.join(MOD_ROOT, 'gradle.properties'));
if (gradleProps) {
  const gid = gradleProps.match(/^mod_id\s*=\s*(.+)$/m);
  if (!gid) add('CASSE', 'gradle.properties', 'mod_id', 'propriete absente');
  else if (gid[1].trim() !== MODID) {
    add('CASSE', 'gradle.properties', 'mod_id', `vaut "${gid[1].trim()}" alors que MOD_ID = "${MODID}" (nom du jar incorrect)`);
  } else add('OK', 'gradle.properties', 'mod_id');
  if (/^mod_group_id\s*=\s*com\.example/m.test(gradleProps)) {
    add('CASSE', 'gradle.properties', 'mod_group_id', 'vaut encore "com.example.examplemod"');
  }
}

// ---------------------------------------------------------------------------
// Rapport
// ---------------------------------------------------------------------------
const summary = {
  registries: {
    blocks: blocks.size,
    items: items.size,
    blockEntities: blockEntities.size,
    menus: menus.size,
    fluids: fluidRegs.size,
    tabs: tabs.size,
  },
  recipes: recipeCount,
  texturesReferenced: textureRefs.size,
  problems: findings.filter((f) => f.level === 'MANQUANT' || f.level === 'CASSE').length,
  findings,
};

if (AS_JSON) {
  process.stdout.write(JSON.stringify(summary, null, 2));
} else {
  const C = {
    reset: '\x1b[0m',
    red: '\x1b[31m',
    yellow: '\x1b[33m',
    green: '\x1b[32m',
    cyan: '\x1b[36m',
    gray: '\x1b[90m',
  };
  const out = [];
  out.push('');
  out.push(`${C.cyan}=== AcademyCraft :: validation offline ===${C.reset}`);
  out.push(
    `${C.gray}Racine : ${MOD_ROOT}${C.reset}`,
  );
  out.push(
    `Registres : ${blocks.size} blocs | ${items.size} items | ${blockEntities.size} block entities | ${menus.size} menus | ${fluidRegs.size} fluides | ${tabs.size} onglets | ${recipeCount} recipes`,
  );

  const byLevel = {};
  for (const f of findings) byLevel[f.level] = (byLevel[f.level] ?? 0) + 1;
  out.push('');
  out.push(`${C.cyan}=== Synthese ===${C.reset}`);
  for (const lvl of ['OK', 'INFO', 'MANQUANT', 'CASSE']) {
    if (byLevel[lvl]) out.push(`  ${lvl.padEnd(9)} : ${byLevel[lvl]}`);
  }

  const problems = findings.filter((f) => f.level === 'MANQUANT' || f.level === 'CASSE');
  if (!problems.length) {
    out.push('');
    out.push(`${C.green}  Aucun probleme detecte.${C.reset}`);
  } else {
    out.push('');
    out.push(`${C.yellow}=== Problemes ===${C.reset}`);
    const byCat = {};
    for (const p of problems) (byCat[p.category] ??= []).push(p);
    for (const cat of Object.keys(byCat).sort()) {
      out.push(`${C.yellow}[${cat}] (${byCat[cat].length})${C.reset}`);
      for (const p of byCat[cat].sort((a, b) => a.name.localeCompare(b.name))) {
        out.push(`   - ${p.name}${p.detail ? `   ${C.gray}${p.detail}${C.reset}` : ''}`);
      }
    }
  }

  const infos = findings.filter((f) => f.level === 'INFO');
  if (infos.length) {
    out.push('');
    out.push(`${C.gray}=== Infos (non bloquant) ===${C.reset}`);
    for (const p of infos.sort((a, b) => (a.category + a.name).localeCompare(b.category + b.name))) {
      out.push(`${C.gray}  [${p.category}] ${p.name}${p.detail ? `  ${p.detail}` : ''}${C.reset}`);
    }
  }
  out.push('');
  process.stdout.write(out.join('\n') + '\n');
}

if (STRICT && summary.problems > 0) process.exit(1);
process.exit(0);
