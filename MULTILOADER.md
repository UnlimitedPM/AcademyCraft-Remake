# AcademyCraft multiloader — état et feuille de route

Branche : `feat/multiloader`.
Version de Minecraft : 1.20.1. Version du mod : 1.0.4 (inchangée).

## Objectif

Produire un jar pour chaque famille de chargeurs à partir d'un seul dépôt :

| Module | Chargeur | Artefact | État |
|---|---|---|---|
| `forge/` | Forge 47.4.20 | `academy-forge-1.0.4.jar` | Complet : tout le portage 1.12.2 → 1.20.1 |
| `neoforge/` | NeoForge 47.1.106 | `academy-neoforge-1.0.4.jar` | Complet : même code, adapté aux APIs NeoForge 47.1 |
| `fabric/` | Fabric Loader 0.16.14 + Fabric API 0.92.12 | `academy-fabric-1.0.4.jar` | Démarrage : 20 objets + 5 blocs + onglet créatif |
| `common/` | — | aucun (sources partagées) | Une classe neutre, à remplir |

Quilt n'a pas de module : Quilt Loader lit `fabric.mod.json` et charge les mods Fabric. Le jar
`academy-fabric-1.0.4.jar` sert donc aussi pour Quilt.

## Arborescence

```
AcademyCraft/
├── build.gradle                 racine : Java 17, dépôts, plugins apply false
├── settings.gradle              sous-projets + plateforme Loom de :fabric
├── gradle/minecraft-module.gradle   mappings officiels pour les modules Loom
├── common/                      code et ressources neutres
├── forge/                       code Forge actuel (ancien src/ déplacé ici)
├── neoforge/                    point d'entrée NeoForge
└── fabric/                      point d'entrée Fabric + fabric.mod.json
```

Les dossiers `run/`, `run-data/`, `libs/`, `scripts/` sont restés à la racine. Les anciens mondes
et options de développement sont donc retrouvés tels quels.

## Outillage

- **Architectury Loom 1.4.370** (Gradle 8.3) pour `common` et `fabric`.
- **ForgeGradle 6.0.24** pour `forge` et `neoforge`.
- **Shadow 8.1.1** : `fabric` fusionne le jar de `common` dans son jar final.
- **Stonecutter non installé** : il gère plusieurs versions de Minecraft, pas plusieurs chargeurs.
  Il deviendra utile le jour où une 1.21.x devra cohabiter avec 1.20.1. La disposition par modules
  (branched setup) est déjà celle que Stonecutter attend.

Pourquoi ce mélange :

1. Architectury Loom 1.4 compile bien `forge`, mais son lancement en développement casse sous
   Forge 47.4 : le Minecraft remappé par Loom n'a pas de `META-INF/versions`, et Forge meurt au
   scan des mods (`UnionFileSystem$NoSuchFileException: /META-INF/versions`).
2. Les versions plus récentes de Loom (1.6, 1.7) échouent sur cette machine au démarrage de
   Minecraft (`Unexpected IllegalAccessException ... Gson 2.9.1`), quel que soit le JDK 17 utilisé.
3. Neoforge 1.20.1 conserve les packages `net.minecraftforge` : son artefact userdev est consommé
   directement par ForgeGradle, comme Forge.

Architectury API n'existe pas pour NeoForge 1.20.1 (les premiers artefacts `architectury-neoforge`
sont en 10.x, donc 1.20.4+). Le code partagé ne doit donc pas dépendre d'Architectury API : les
abstractions de chargeur seront écrites dans `common/`.

## Commandes

Toujours définir Java 17 :

```powershell
$env:JAVA_HOME='C:\Users\Paul\.jdks\temurin-17.0.20.1'
```

| But | Commande |
|---|---|
| Tout construire | `.\gradlew.bat build` |
| Compiler Forge | `.\gradlew.bat :forge:compileJava` |
| Tests unitaires Forge | `.\gradlew.bat :forge:test` |
| Client Forge | `.\gradlew.bat :forge:runClient` |
| Serveur Forge | `.\gradlew.bat :forge:runServer` |
| GameTests Forge | `.\gradlew.bat :forge:runGameTestServer` |
| DataGen Forge | `.\gradlew.bat :forge:runData` |
| Client Fabric | `.\gradlew.bat :fabric:runClient` |
| Client NeoForge | `.\gradlew.bat :neoforge:runClient` |
| Validateur d'assets | `node scripts/validate.mjs` |

Les tâches VS Code (`MC: …`) pointent déjà sur les tâches `:forge:*`.

## Feuille de route du portage

### 1. NeoForge — fait

NeoForge 1.20.1 est un fork de Forge 47 : packages identiques, donc le code Forge compile presque
tel quel. Ce qui a dû changer :

1. `ResourceLocation.fromNamespaceAndPath` et `withDefaultNamespace` n'existent pas dans NeoForge
   47.1. Le module `neoforge` compile une copie filtrée de `forge/src/main/java`
   (`prepareNeoForgeSources`) qui remplace ces fabriques par `new ResourceLocation(...)`.
2. Le constructeur du mod est sans argument : Forge 47.4 n'accepte que `FMLJavaModLoadingContext`
   ou zéro argument, NeoForge 47.1 n'accepte pas `FMLJavaModLoadingContext`. Le bus se récupère
   par `FMLJavaModLoadingContext.get()`.
3. `registerConfig` se fait par `ModLoadingContext.get()`, disponible chez les deux.
4. JEI et CraftTweaker compilent via leurs artefacts Forge 1.20.1 (packages identiques).

Le filtre de sources est une étape temporaire : quand les classes neutres passeront dans
`common/`, il disparaîtra.

Validé : 182 GameTests NeoForge verts, 975 tests unitaires verts, client NeoForge jusqu'au monde.

### 2. Fabric puis Quilt

Le portage Fabric est en cours. Audit détaillé, ordre des domaines et règles : `FABRIC_PORT.md`.

Le jar Fabric charge déjà le mod, enregistre les matériaux, les minerais et un onglet créatif.
Le reste demande des abstractions de capacités, réseau, config, fluides, énergie et hooks client.

Le chantier est plus profond ; les points suivants n'ont pas d'équivalent direct :

- capacités Forge (`Capability`, `LazyOptional`, `IItemHandler`, énergie, fluides) ;
- réseau Forge (`SimpleChannel`, `NetworkEvent`) ;
- configuration (`ForgeConfigSpec`) ;
- événements et cycles de vie Forge ;
- overlays HUD et rendu Forge ;
- registres (`DeferredRegister`, `RegistryObject`).

Méthode recommandée : extraire d'abord dans `common/` une interface de service par domaine
(`IPlatform`, `INetwork`, `IConfig`, `IEnergyStorage`, `IFluidStorage`, capacités d'items),
implémenter la version Forge dans `forge/`, puis la version Fabric dans `fabric/`.

### 3. Quilt

Rien à faire côté code : livrer le jar Fabric. Vérifier seulement les métadonnées et la
compatibilité Fabric API avec Quilt Loader.

## Vérifications faites

- `gradlew build` : `common`, `fabric`, `forge`, `neoforge` construisent.
- Forge : 973 tests unitaires et 182 GameTests verts ; client jusqu'à l'écran titre puis monde.
- NeoForge : 975 tests unitaires et 182 GameTests verts ; client jusqu'à un monde (`New World`).
- Le jar Forge contient les ressources (45 Mo) et `cn/academy/AcademyCraftCommon.class`.
- Le jar NeoForge contient les mêmes 960 fichiers d'assets et son `META-INF/mods.toml` NeoForge.
- Le jar Fabric contient `fabric.mod.json` et `cn/academy/AcademyCraftCommon.class`.
