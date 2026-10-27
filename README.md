<p align="center">
  <img src="src/main/resources/academy_logo.png" alt="AcademyCraft" width="120">
</p>

<h1 align="center">AcademyCraft Remake</h1>

<p align="center">
  <b>AcademyCraft for Minecraft 1.20.1</b> — the 1.12.2 classic, rebuilt: superpowers, an Imag energy<br>
  network, ten machines and a skill tree.
</p>

<p align="center">
  Minecraft 1.20.1 &nbsp;·&nbsp; Forge 47.4.20+ &nbsp;·&nbsp; Java 17 &nbsp;·&nbsp; GPLv3 &nbsp;·&nbsp; <b>1.0.1-beta</b>
</p>

---

## English

**AcademyCraft Remake** is a port of [AcademyCraft](https://github.com/LambdaInnovation/AcademyCraft) (Minecraft 1.12.2) to **Minecraft 1.20.1 / Forge**. Superpowers, an energy network, machines, a skill tree: the mod is the same, the game under it is not.

The original drew its interfaces with LambdaLib2's own GUI engine, and the port re-implements that engine inside the mod. The original's layout documents are reused as they are, so the screens look like the real thing instead of a look-alike.

### Download and install

1. Install **Forge 47.4.20 or later** for Minecraft 1.20.1.
2. Download **`academy-1.0.1-beta.jar`** from the [Releases](../../releases) page.
3. Drop it into your `mods` folder and start the game.

Nothing else to install. The port works on a server too, but it is a **beta build**: keep a backup of your world.

### How to play

1. **Get an ability.** Open an **Ability Developer** (or the portable one) and press **LEARN**, or type **`/learn`** in chat: both do the same thing. If you carry an **induction factor**, it decides the kind and is consumed; without one, the kind is drawn at random. There are four: electromaster, meltdowner, teleporter, vecmanip.
2. **Learn skills.** The developer and its skill tree show each category's skills, what they cost, what they need, and the LEARN button that teaches them. Using your skills fills the level bar, and a full bar unlocks the next level.
3. **Put them on a key.** Open the **data terminal** (**Left Alt** by default) and its skill tree app: each learned skill can be placed on one of the four ability slots.
4. **Use them.** The four slots are **left click**, **right click**, **R** and **F** by default. Some are used in one press, others work while held, and a few grow if you hold them longer (the railgun charges, the thunder clap gathers, the meltdowner builds up).
5. **Everything else.** **C** cycles presets, **N** opens the preset editor, **V** turns your ability on and off, **F4** opens the debug console (skills, cooldowns, control points), and **Left Alt** opens the terminal. All of them can be changed in Minecraft's controls screen, under "AcademyCraft".
6. **Power a machine.** Place a **wireless node** and a **matrix**, open a machine (solar, wind or phase generator, imag fusor, metal former, ability interferer) and use its **wireless tab**: the list shows the nodes in range, nearest first. Click a node's connect icon, type its password if it has one, and the icon in the "Connected" frame disconnects you. A machine you disconnect stays disconnected — it will not link itself back on the next tick. The **node** works the same way, the other way round: its own wireless tab is where it picks the matrix it joins.
### What is in this version

| | |
|---|---|
| **Abilities** | the four categories and their 35 skills — electromaster (railgun, thunder clap, magnet manipulation...), meltdowner (rays, jet engine...), teleporter (marking, shift, flesh ripping...), vecmanip (directional shock, plasma cannon...) — plus the three generic courses, with control points, overload, per-skill experience, charges and cooldowns |
| **Skill tree** | the Ability Developer machine: the original's tree, its icons, its descriptions, its levels, its costs, and the magnetic coil that resets a category |
| **Energy** | the Imag network: solar, wind and phase generators, the wireless nodes and the matrix (every machine picks its node from its own wireless tab, and every node picks its network from its own), the imag fusor, the metal former with its four modes and the original's recipes (plus every ore that smelts) |
| **Terminal** | the data terminal and its apps (settings, skill tree, media player, about) plus the tutorial |
| **Interface** | the ability HUD, key hints, notifications, presets, the F4 debug console, and the ten machine screens on the rebuilt engine |
| **World** | the mod's ores, the Imag phase liquid, the advancements |
| **Integrations** | **JEI** (two recipe categories: the metal former's four modes, the imag fusor's phases, plus the generic refining of every ore that smelts) and **CraftTweaker** (`mods.academy.MetalFormer` and `mods.academy.ImagFusor` add or remove recipes from a script). Both are **optional**: without them the mod behaves exactly the same |
| **Configuration** | `config/academy-common.toml`: the four control point tables per level (`initCp`, `addCp`, `initOverload`, `addOverload`, the original's `init_cp` and friends), a multiplier on every cost, cooldown and experience gain, the list of disabled skills, world generation, the damage scale, the server's lock on block breaking and PvP. On top of that lock, every player chooses in the settings screen **which of their own skills may break blocks** — the server decides what is allowed, the player decides what they accept. Every option carries a comment in French then in English, and every number a bound |

### What is still missing

Honest list, so you know what you are testing:

- **Languages** — the mod speaks English, French, German, Spanish and Chinese. The original also had Japanese, Korean, Russian and Traditional Chinese.
- **Per-skill cost multipliers** — the 1.12.2 kept one file per skill (`cp_consume_speed`, `overload_consume_speed`, `exp_incr_speed`). Here the cost, cooldown and experience multipliers are global, and a skill is switched on or off from `ability.disabledSkills`. Damage and range stay in the code, where the original kept them too.
- **Taunts** — the original's in-game taunt lines are not ported yet.
- **EU (IC2) / RF conversion** blocks.

### Building from source

Needs **JDK 17** (Gradle 8.1 and Forge 1.20.1 refuse newer ones):

```
./gradlew build        # jar in build/libs/academy-<version>.jar
./gradlew runClient    # a development client
./gradlew test         # unit tests
./gradlew runGameTestServer   # in-game tests, headless
```

On Windows, run `gradlew.bat`. If Java 17 is not your default, set `JAVA_HOME` first — the project also pins it in `gradle.properties` (`org.gradle.java.home`).

### Credits

The original **AcademyCraft** was made by **WeAthFolD** and **Lambda Innovation**, with [LambdaLib2](https://github.com/LambdaInnovation/LambdaLib2). Its assets, its layout documents, its numbers and its behaviour are what this port is built from — thank you for a mod we are still playing.

### License

**GPLv3**, like the original. See `LICENSE.txt`.

---

## Français

**AcademyCraft Remake** est le portage d'[AcademyCraft](https://github.com/LambdaInnovation/AcademyCraft) (Minecraft 1.12.2) vers **Minecraft 1.20.1 / Forge**. Les superpouvoirs, le réseau d'énergie, les machines et l'arbre de compétences sont les mêmes ; le jeu en dessous, non.

L'original dessinait ses interfaces avec le moteur d'interface de LambdaLib2 ; le port réimplémente ce moteur dans le mod. Les documents de mise en page de l'original sont réutilisés tels quels, donc les écrans ressemblent au vrai mod et non à une imitation.

### Télécharger et installer

1. Installez **Forge 47.4.20 ou plus** pour Minecraft 1.20.1.
2. Téléchargez **`academy-1.0.1-beta.jar`** depuis la page [Releases](../../releases).
3. Posez-le dans votre dossier `mods` et lancez le jeu.

Rien d'autre à installer. Le port fonctionne aussi sur un serveur, mais c'est une **version bêta** : gardez une sauvegarde de votre monde.

### Comment jouer

1. **Obtenir une aptitude.** Ouvrez un **développeur d'aptitude** (ou le portable) et appuyez sur **LEARN**, ou tapez **`/learn`** dans le chat : les deux font la même chose. Si vous portez un **facteur d'induction**, c'est lui qui décide du type et il est consommé ; sans facteur, le type est tiré au hasard. Il y en a quatre : electromaster, meltdowner, teleporter, vecmanip.
2. **Apprendre des compétences.** Le développeur et son arbre montrent les compétences de la catégorie, ce qu'elles coûtent, ce qu'elles demandent, et le bouton LEARN qui les enseigne. Utiliser ses compétences remplit la barre de niveau ; le palier plein ouvre le niveau suivant.
3. **Les mettre sur une touche.** Ouvrez le **terminal de données** (**Alt gauche** par défaut) et son application d'arbre : chaque compétence apprise se range sur l'un des quatre emplacements d'aptitude.
4. **Les utiliser.** Les quatre emplacements sont le **clic gauche**, le **clic droit**, **R** et **F** par défaut. Certaines partent en une pression, d'autres agissent tant qu'on maintient, et quelques-unes montent si on maintient plus longtemps (le railgun se charge, le thunder clap s'accumule, le meltdowner monte en puissance).
5. **Le reste.** **C** fait défiler les presets, **N** ouvre l'éditeur de presets, **V** allume et éteint l'aptitude, **F4** ouvre la console de débogage (compétences, recharges, points de contrôle), et **Alt gauche** ouvre le terminal. Toutes se changent dans les commandes de Minecraft, catégorie « AcademyCraft ».
6. **Alimenter une machine.** Posez un **nœud sans fil** et une **matrice**, ouvrez une machine (générateur solaire, éolien ou de phase, fusor d'imag, formeur de métal, brouilleur d'aptitude) et passez par son **onglet sans fil** : la liste montre les nœuds à portée, du plus proche au plus loin. Cliquez l'icône de connexion d'un nœud, tapez son mot de passe s'il en a un, et l'icône du cadre « Connected » vous en déconnecte. Une machine que vous déconnectez reste déconnectée : elle ne se raccroche pas toute seule au tick suivant. Le **nœud** fait la même chose dans l'autre sens : c'est son propre onglet sans fil qui choisit la matrice qu'il rejoint.
### Ce que contient cette version

Les quatre catégories d'aptitudes et leurs 35 compétences (electromaster, meltdowner, teleporter, vecmanip) et les trois cursus génériques, avec points de contrôle, surcharge, expérience par compétence, charges et recharges ; l'arbre du développeur d'aptitude avec ses icônes, ses descriptions, ses niveaux et ses coûts ; le réseau d'énergie imag (générateurs solaire, éolien et de phase, nœuds et matrice sans fil, fusor d'imag, le formeur de métal avec ses quatre modes et les recettes de l'original, dont tout minerai qui fond) ; l'onglet sans fil de chaque machine, où l'on choisit le nœud qui l'alimente, et celui de chaque nœud, où l'on choisit le réseau qu'il rejoint ; le terminal de données et ses applications, plus le tutoriel ; l'affichage tête haute, les préset, la console F4, et les dix écrans de machine sur le moteur reconstruit ; les minerais du mod, le fluide de phase imag et les succès ; les deux intégrations facultatives, **JEI** (les recettes du formeur de métal et du fusor d'imag dans leurs propres catégories, raffinage générique des minerais compris) et **CraftTweaker** (`mods.academy.MetalFormer` et `mods.academy.ImagFusor` pour ajouter ou retirer des recettes depuis un script) ; et la **configuration** (`config/academy-common.toml`) : les quatre tables de points de contrôle par niveau (`initCp`, `addCp`, `initOverload`, `addOverload`, les `init_cp` de l'original), un multiplicateur sur chaque coût, chaque recharge et chaque gain d'expérience, la liste des compétences désactivées, la génération du monde, l'échelle des dégâts, le cassage de blocs et le PvP — chaque réglage commenté en français puis en anglais, et chaque nombre borné. Par-dessus ce verrou du serveur, chaque joueur choisit dans l'écran de réglage **lesquelles de ses propres compétences peuvent casser des blocs** : le serveur dit ce qui est permis, le joueur dit ce qu'il accepte.

### Ce qu'il manque encore

- **Les langues** : le mod parle anglais, français, allemand, espagnol et chinois. L'original en avait aussi quatre autres (japonais, coréen, russe, chinois traditionnel).
- **Les multiplicateurs par compétence** : la 1.12.2 tenait un fichier par compétence (`cp_consume_speed`, `overload_consume_speed`, `exp_incr_speed`). Ici les multiplicateurs de coût, de recharge et d'expérience sont globaux, et une compétence s'allume ou s'éteint dans `ability.disabledSkills`. Les dégâts et les portées restent dans le code, comme dans l'original.
- **Les taunts** : les répliques de l'original ne sont pas encore portées.
- **Les blocs de conversion EU (IC2) / RF**.

### Compiler depuis les sources

Il faut **JDK 17** (Gradle 8.1 et Forge 1.20.1 refusent les plus récents). Les commandes sont celles de la section anglaise : `gradlew build`, `gradlew runClient`, `gradlew test`, `gradlew runGameTestServer`. Sous Windows, `gradlew.bat` ; si Java 17 n'est pas votre Java par défaut, posez `JAVA_HOME` (le projet l'épingle aussi dans `gradle.properties`).

### Crédits

L'**AcademyCraft** d'origine a été fait par **WeAthFolD** et **Lambda Innovation**, avec [LambdaLib2](https://github.com/LambdaInnovation/LambdaLib2). Ses ressources, ses documents de mise en page, ses nombres et son comportement sont ce sur quoi ce portage est bâti — merci pour un mod auquel on joue encore.

### Licence

**GPLv3**, comme l'original. Voir `LICENSE.txt`.
