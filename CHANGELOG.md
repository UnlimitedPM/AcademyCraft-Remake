# Changelog

## 0.1.5 - l'imag phase liquide / the imag phase liquid

### English

All of 0.1.4, plus the imag phase liquid rebuilt on the original's own renderer, a reworked
loot table, and the whole game in four languages.

- **The liquid's sheets are back, and they show like the original's.** The port drew three
  translucent quads per liquid block, but they were painted *before* the fluid's own layer, so
  the black fluid covered them; then, once they drew after it, they stayed hidden inside it.
  Both causes are gone: the sheets now draw right after the world's translucent layer, and the
  depth test is truly off, as in 1.12.2's `RenderImagPhaseLiquid`, so they are visible through
  the liquid and even through walls. Their textures and their opacity are the original's own,
  and they follow the slope of a flowing liquid corner by corner.
- **The liquid is a liquid.** It emits light 8, as the original's fluid did; entering it tints
  the screen and closes the fog like water; and it sinks at the 1.12.2 speed, four times faster
  than 1.20.1's own water, which divides gravity by sixteen. You can also swim in it, with the
  swimming pose arriving on sprint — a request of the player's, the 1.12.2 mod could not.
- **Four factor items in the old structures' chests**, one per stack, as the original's loot
  table had them.
- **The mod speaks French, German, Spanish and Chinese**, 418 keys each, checked against the
  English file by a test that compares the keys, the placeholders and the line breaks.
- **No more red chat messages.** A refusal — power off, not learnt, not enough points,
  overloaded, jammed, on cooldown — is silent now, as is a machine's wireless link.
- **And it costs less.** The sheets' renderer allocates nothing per frame any more (the layer
  geometry and their box are computed twice a second), skips what the camera cannot see, and
  the liquid block no longer carries an empty block entity per block: the renderer finds the
  blocks through the chunk's palette.
- **JEI knows the machines.** Two recipe categories of its own — the metal former (one row per
  mode, with the mode's name between the input and the output) and the imag fusor (the phase a
  fusion drinks, in millibuckets) — plus the generic refining, where every ore that smelts gives
  twice its furnace result. Both pages are drawn on **the original's own NEI sheets**
  (`nei_metalformer.png`, `nei_fusor.png`, shipped but never wired): the machine's schematic, with
  IN, MODE and OUT, its slots in their place. The metal former adds a panel with the mode's icon **at
  its native size** and its name: these icons are 48-pixel drawings that the machine reduces to 24,
  where nothing is left but a white bell. Both machines are
  registered as catalysts, so the "uses" key on a metal former opens its recipes. JEI stays
  **optional**: it is a soft dependency, and the mod loads without it.
- **CraftTweaker speaks to the machines.** `mods.academy.MetalFormer.addRecipe(<in>, <out>, "incise")`,
  `removeRecipe(<out>)`, `removeRecipeByInput(<in>, "plate")`, `removeAllRecipes()`, and the same
  for `mods.academy.ImagFusor` with the number of millibuckets. Scripts are **replayable**: a
  recipe replaces the one that already handled the same input, so a `/reload` never duplicates
  anything. Soft dependency as well.
- **The two developer recipes name a red bed** instead of the `minecraft:beds` tag. The tag came
  from the port (1.12.2 had a single bed item); in JEI it turned the slot into a tag, which told the
  player nothing about what to put there.
- **Tests**: 170 in-game tests, 866 unit tests.

### Francais

Tout le 0.1.4, plus l'imag phase liquide refaite sur le rendu de l'original, une table de butin
retravaillee, et tout le jeu en quatre langues.

- **Les nappes du liquide sont revenues, et se voient comme dans l'original.** Le port dessinait
  trois quads translucides par bloc de liquide, mais ils etaient peints *avant* la couche du
  fluide : le noir les recouvrait. Une fois dessines apres, ils restaient enfermes dedans. Les
  deux causes sont parties : les nappes passent juste apres la couche translucide du monde, et le
  test de profondeur est vraiment coupe, comme le `RenderImagPhaseLiquid` de 1.12.2 — on les voit
  a travers le liquide et meme a travers les murs. Leurs textures et leur opacite sont celles de
  l'original, et elles suivent la pente d'un liquide qui coule, coin par coin.
- **Le liquide est un liquide.** Il eclaire, niveau 8, comme le fluide de l'original ; y entrer
  teinte l'ecran et referme le brouillard comme l'eau ; et on y coule a la vitesse de la 1.12.2,
  quatre fois plus vite que dans l'eau de la 1.20.1, qui divise la gravite par seize. On peut
  aussi y nager, la position de nage arrivant en sprintant — une demande du joueur, le mod de
  1.12.2 ne le pouvait pas.
- **Quatre facteurs d'induction dans les coffres des vieilles structures**, un par pile, comme les
  avait la table de butin de l'original.
- **Le mod parle francais, allemand, espagnol et chinois**, 418 cles chacun, verifiees contre le
  fichier anglais par un test qui compare les cles, les jetons et les sauts de ligne.
- **Plus aucun message rouge.** Un refus — pouvoir eteint, competence non apprise, points
  insuffisants, surcharge, brouillage, recharge — est silencieux, et le raccordement sans fil
  d'une machine aussi.
- **Et ca coute moins cher.** Le rendu des nappes n'alloue plus rien par image (la geometrie des
  nappes et leur boite sont calculees deux fois par seconde), ecarte ce que la camera ne voit pas,
  et le bloc de liquide ne porte plus un block entity vide par bloc : le rendu retrouve les blocs
  par la palette du chunk.
- **JEI connait les machines.** Deux categories de recettes a lui — le formeur de metal (une ligne
  par mode, avec le nom du mode entre l'entree et la sortie) et le fusor d'imag (la phase qu'une
  fusion boit, en millibassins) — plus le raffinage generique, ou tout minerai qui fond donne deux
  fois son resultat de four. Les deux pages sont dessinees sur **les planches NEI de l'original**
  (`nei_metalformer.png`, `nei_fusor.png`, livrees mais jamais branchees) : le schema de la
  machine, avec IN, MODE et OUT, ses emplacements a leur place. Le formeur ajoute une case pour
  l'icone de son mode, **a sa taille native**, et son nom : ces icones sont des dessins de 48
  pixels que la machine reduit a 24, ou il ne reste qu'une cloche blanche. Les deux machines sont declarees comme catalyseurs : la touche "utilisations" sur un formeur ouvre
  ses recettes. JEI reste **facultatif**, en soft depend : le mod se lance sans lui.
- **CraftTweaker parle aux machines.** `mods.academy.MetalFormer.addRecipe(<entree>, <sortie>, "incise")`,
  `removeRecipe(<sortie>)`, `removeRecipeByInput(<entree>, "plate")`, `removeAllRecipes()`, et la
  meme chose pour `mods.academy.ImagFusor` avec le nombre de millibassins. Les scripts sont
  **rejouables** : une recette remplace celle qui traitait deja la meme entree, donc un `/reload`
  ne duplique jamais rien. Soft depend lui aussi.
- **Les deux recettes du developpeur nomment un lit rouge** au lieu du tag `minecraft:beds`. Le
  tag venait du port (la 1.12.2 n'avait qu'un seul lit) ; dans JEI il transformait l'emplacement en
  tag, ce qui ne disait rien de ce qu'il fallait y mettre.
- **Des tests** : 170 tests en jeu, 866 tests unitaires.

## 0.1.4 - l'arbre de competences et les deux developpeurs / the skill tree and the two developers

### English

All of 0.1.3, plus the Ability Developer, its screen and its two machines.

- **The skill tree draws like the original.** Its lines were invisible: the port asked the graphics card for a program that reads its vertices in one order and handed them over in another, so nothing came out and nothing complained. They now fade at both ends instead of stopping on a hard edge, pass under the nodes' discs, and their enlarged end follows the hover of its own node, as in 1.12.2 where the line lived inside the node's box.
- **The card over the tree, and the text on it**, come from the original's own layout: the tree stays behind the card, the icon is clipped on its disc and keeps its brightness, the text falls where the original put it, a click counts anywhere on a node, and the line and the disc are smooth instead of stair-stepped.
- **The two machines are 3D again, drawn the original's way.** The port drew them as a block model, and a translucent block-model layer does not write depth: the rear glass pane painted over the front one and hid the world behind. They now go through a block entity renderer, as in 1.12.2, with a translucent pass that writes depth, so the glass shows the world behind it. The vanilla outline is gone, the inside of the open shell is visible, and the glass alpha is the original's.
- **And they sit where they should.** The block-model transform put them half a block down, half a block forward and half a block right; the original's placement (a half-block offset, scale from the model's origin) gave back the height and the depth. The foot still hung to one side, a flaw of the real mod, measured on both models at 0.072, so it is recentred on its block: the foot is exactly one block wide. Nothing else moved.
- **The jammer jams.** Its placer is no longer spared, a power already running stops on entering the field, its whitelist reaches the server, and its radius is set by 5 or 10.
- **The Matrix's reach** is counted from the centre of its two-by-two-by-two cube, so a node reaches equally far from any of the four sides; nothing joins a network by itself any more, and Escape leaves a field.
- **The former speaks the original's language**: its modes carry the names of 1.12.2, a hovered mode shows its name in a box, and its ores go through the blast furnace. The solar panel's screen no longer shows a frozen buffer, the fusor no longer forgets what it was doing and its recipe stops flickering between 8000 and IDLE, a hidden page no longer takes clicks, and the inventory's frames sit on their slots again.
- **Tests**: 167 in-game tests, 850 unit tests.

### Francais

Tout le 0.1.3, plus le developpeur de competences, son ecran et ses deux machines.

- **L'arbre se dessine comme l'original.** Ses traits etaient invisibles : le port demandait a la carte graphique un programme qui lit ses sommets dans un ordre et les lui donnait dans l'autre, donc rien ne sortait et rien ne se plaignait. Ils s'effacent maintenant a leurs deux bouts au lieu de s'arreter net, passent sous les disques des noeuds, et leur bout grossi suit le survol de son propre noeud, comme en 1.12.2 ou le trait vivait dans la boite du noeud.
- **La fiche au-dessus de l'arbre, et son texte**, viennent de la disposition de l'original : l'arbre reste derriere la fiche, l'icone est ecretee sur son disque et garde son eclat, le texte tombe ou l'original le mettait, un clic compte n'importe ou sur un noeud, et le trait comme le disque sont lisses au lieu d'etre en escalier.
- **Les deux machines sont de nouveau en 3D, dessinees comme l'original.** Le port les dessinait en modele de bloc, et une couche de bloc translucide n'ecrit pas la profondeur : la vitre arriere repeignait celle de devant et cachait le monde derriere. Elles passent maintenant par un rendu de block entity, comme en 1.12.2, avec une couche translucide qui ecrit la profondeur : a travers la vitre on voit le monde. Le lisere vanilla a disparu, l'interieur de la coque ouverte se voit, et l'alpha de la vitre est celui de l'original.
- **Et elles sont a leur place.** La transformation du modele de bloc les posait un demi-bloc plus bas, un demi-bloc en avant et un demi-bloc a droite ; la mise en place de l'original (un demi-bloc d'ecart, echelle depuis l'origine du modele) a rendu la hauteur et la profondeur. Le pied debordait encore d'un cote, un defaut du vrai mod, mesure sur les deux modeles a 0,072 : il est donc recentre sur sa brique, et il fait pile un bloc de large. Rien d'autre n'a bouge.
- **Le brouilleur brouille.** Son poseur n'est plus epargne, un pouvoir deja en cours s'arrete quand on entre dans le champ, sa liste blanche arrive au serveur, et son rayon se regle par 5 ou 10.
- **La portee du Matrix** se compte depuis le centre de son cube de deux blocs, donc un noeud porte aussi loin des quatre cotes ; plus rien ne se raccorde tout seul a un reseau, et Echap sort d'un champ.
- **Le formeur parle la langue de l'original** : ses modes portent les noms de 1.12.2, un mode survole montre son nom dans sa boite, et ses minerais passent au haut-fourneau. L'ecran du panneau solaire ne montre plus un tampon fige, le fusor n'oublie plus ce qu'il faisait et sa recette ne bat plus entre 8000 et IDLE, une page cachee ne prend plus les clics, et les cadres de l'inventaire retombent sur leurs emplacements.
- **Des tests** : 167 tests en jeu, 850 tests unitaires.

## 0.1.3 - les machines, la police et le reseau / the machines, the font and the network

### English

All of 0.1.2, plus the whole machine interface, rebuilt on the original's own documents.

- **Every machine screen is the original's one.** The port drew them by hand; they are now read from the mod's own layout files, with the original's geometry kept to the pixel — or rather to the half pixel: where a document asks for a bar of 6.4 pixels, the card is asked for 6.4 and nothing is rounded on the way. Four rounds of "one pixel off" came from that rounding, and the remedy was to hand the exact size to the graphics card.
- **The system's font, at the original's size.** The mod draws with the machine's own font (Microsoft YaHei, the one the 1.12.2 client used), rasterised at the original's cell size, with its bold drawing, its coverage curve and its margins. What you read is what the original showed.
- **Text you can write in.** A field keeps what you typed when you move to the other field, the caret waits before the first letter, a password shows its asterisks, Enter and the keypad's Enter both validate, and Delete empties a field — the only gesture the original had for replacing a value.
- **The wireless tab, on seven machines and on the node.** A machine picks its node, a node picks its Matrix, and a network asking for a password opens with it.
- **The Matrix works.** Its network is created from its own panel, its reach is counted from the centre of the two-by-two-by-two cube — so a node reaches equally far from any of the four sides — and a protected network no longer joins by itself: the password is typed in the tab, where it means something.
- **Tests**: 168 in-game tests, 841 unit tests.

### Francais

Tout le 0.1.2, plus toute l'interface des machines, refaite sur les documents de l'original.

- **Tous les ecrans de machine sont ceux de l'original.** Le port les dessinait a la main ; ils se lisent maintenant dans ses propres fichiers de disposition, avec sa geometrie gardee au pixel pres — ou plutot au demi-pixel pres : la ou le document demande une barre de 6,4 pixels, la carte en recoit 6,4, et rien n'est arrondi en chemin. Quatre tours de « un pixel a cote » venaient de cet arrondi, et le remede a ete de donner la taille exacte a la carte graphique.
- **La police du systeme, a la taille de l'original.** Le mod dessine avec la police de la machine (Microsoft YaHei, celle que le client 1.12.2 utilisait), gravee a la cellule de l'original, avec son dessin gras, sa courbe de couverture et ses marges. Ce qu'on lit est ce que l'original montrait.
- **Du texte ou l'on peut ecrire.** Un champ garde ce qu'on tape quand on passe a l'autre champ, le curseur est la avant la premiere lettre, un mot de passe montre ses etoiles, Entree et l'Entree du pave valident tous les deux, et Suppr vide un champ — le seul geste que l'original avait pour remplacer une valeur.
- **L'onglet sans fil, sur sept machines et sur le noeud.** Une machine choisit son noeud, un noeud choisit son Matrix, et un reseau qui demande un mot de passe s'ouvre avec lui.
- **Le Matrix fonctionne.** Son reseau se cree depuis son propre panneau, sa portee se compte depuis le centre du cube de deux blocs — donc un noeud porte aussi loin des quatre cotes — et un reseau protege ne se rejoint plus tout seul : le mot de passe se tape dans l'onglet, la ou il veut dire quelque chose.
- **Des tests** : 168 tests en jeu, 841 tests unitaires.

## 0.1.2 - le formeur et ses recettes / the former and its recipes

### English

All of 0.1.1, plus two things.

- **The metal former's mode arrows work.** They were wired into nothing: the engine looked up a page widget through the view, and during the screen's build the view does not know the pages yet — they are only placed in the tree afterwards. Every screen that wires something at that moment was concerned, not just this one.
- **The metal former knows its recipes again.** It had the original's four; it now has all seventeen explicit ones — cutting silicon into wafers and pieces, etching a data chip into a calc chip, plating iron and constraint ingot, refining its four ores — plus the **refining rule**: every ore that smelts comes back at double. That covers the mod's ores, vanilla's, and any other mod's metals, because it reads the game's own smelting recipes instead of a hand-written list. The original's two exceptions are kept: redstone gives a block, lapis gives twelve.
- A flaky in-game test (a thrown iron block landing) was given a wider budget, so the suite stops reporting a failure that was only impatience.
- **Tests**: 158 in-game tests, 825 unit tests.

### Francais

Tout le 0.1.1, plus deux choses.

- **Les fleches de mode du formeur de metal fonctionnent.** Elles se cablaient dans le vide : le moteur cherchait un widget de page a travers la vue, et pendant la construction de l'ecran la vue ne connait pas encore les pages — elles ne sont posees dans l'arbre qu'apres. Tous les ecrans qui cablent quelque chose a ce moment-la etaient concernes, pas seulement celui-ci.
- **Le formeur connait ses recettes.** Il n'en avait que les quatre de l'original ; il a maintenant les dix-sept explicites — tailler le silicium en tranches et en morceaux, graver une puce de donnees en puce de calcul, plaquer le fer et le lingot de contrainte, raffiner ses quatre minerais — plus la **regle de raffinage** : tout minerai qui fond revient au double. Cela couvre les minerais du mod, ceux de vanilla et ceux des metaux des autres mods, parce qu'elle lit les recettes de fonte du jeu au lieu d'une liste ecrite a la main. Les deux exceptions de l'original sont gardees : le redstone donne un bloc, le lapis en donne douze.
- Un test en jeu instable (le lancer d'un bloc de fer qui retombe) a recu un budget plus large : la suite ne signale plus un echec qui n'etait que de l'impatience.
- **Des tests** : 158 tests en jeu, 825 tests unitaires.

## 0.1.1 - le developpeur ouvre la porte / the developer opens the door

Tout le 0.1.0, plus ce que le tester en vrai a fait sortir. Rien n'a ete retire.

### English

All of 0.1.0, plus what testing it as a real player brought out. Nothing was removed.

- **A first ability can be obtained now.** The port had no way in: the developer opened on an empty tree, and the only door was a debug command nobody knows. Its **LEARN** button gives a first ability outright, without waiting for the machine's energy — with an **induction factor** in the inventory, that factor decides the kind and is consumed; without one, the kind is drawn at random. `/learn` in chat does exactly the same thing.
- **The developer's console** says what the original's says: Academy OS booting, the failure, the verdict, and the command to type.
- **Two interface engine fixes**, both of which were hiding something the player could not see: a button that overflows its parent is clickable now — the LEARN button never was, because it is twice as wide as its panel — and a text is only drawn in a box that has a size, so every line of a skill's page (its name, its experience, its conditions, its cost) was invisible.
- **The README explains how to play**: how to get an ability, where to put the skills you learn, and which keys do what — left click, right click, R and F for the four slots, C for presets, N for the preset editor, V to turn the ability on and off, F4 for the debug panel, Left Alt for the terminal.
- **Tests**: 157 in-game tests and around 825 unit tests, two of which pin the new click rule.

### Francais

Tout le 0.1.0, plus ce que le tester en vrai a fait sortir. Rien n'a ete retire.

- **Une premiere aptitude s'obtient maintenant.** Le port n'avait aucune porte d'entree : le developpeur s'ouvrait sur un arbre vide, et la seule commande etait une commande de debogage que personne ne connait. Son bouton **LEARN** donne la premiere aptitude tout de suite, sans attendre l'energie de la machine — avec un **facteur d'induction** dans l'inventaire, c'est lui qui decide du type et il est consomme ; sans facteur, le type est tire au sort. La commande `/learn` fait exactement la meme chose.
- **La console du developpeur** dit ce que dit celle de l'original : le demarrage d'Academy OS, l'echec, le verdict, et la commande a taper.
- **Deux corrections du moteur d'interface**, qui cachaient toutes les deux quelque chose que le joueur ne pouvait pas voir : un bouton qui deborde de son parent se clique maintenant — celui de LEARN ne se cliquait pas, il fait deux fois la largeur de son panneau — et un texte ne se dessine que dans une boite qui a une taille, donc toutes les lignes d'une page de competence (son nom, son experience, ses conditions, son cout) etaient invisibles.
- **Le README explique comment jouer** : obtenir une aptitude, ranger les competences apprises, et ce que fait chaque touche — clic gauche, clic droit, R et F pour les quatre emplacements, C pour les presets, N pour l'editeur, V pour allumer et eteindre, F4 pour le panneau de debogage, Alt gauche pour le terminal.
- **Des tests** : 157 tests en jeu et environ 825 tests unitaires, dont deux qui figent la nouvelle regle du clic.

## 0.1.0 - premiere version de test / first test build

### English

First public build. It exists to be tested: everything below works, the rest is listed in the README.

- **The original's GUI engine**, re-implemented inside the mod, with the original's own layout documents reused as they are. The ten machine screens (solar, wind, phase, imag fusor, metal former, wireless node and matrix, ability interferer, developer) and the ability developer's skill tree are drawn by it.
- **The four ability categories and their 35 skills**, plus the three generic courses: control points, overload, per-skill experience, charges, cooldowns, key bindings, presets, and the ability HUD.
- **The Imag energy network**: solar, wind and phase generators, the wireless nodes and the matrix, the imag fusor, the metal former, the ability interferer.
- **The data terminal**, its applications (settings, skill tree, media player, about) and the tutorial.
- **The world content**: the mod's ores, the Imag phase liquid, and the advancements.
- **Seven bugs of the original fixed**, found while reading it: a knockback that was applied twice, a search that could pick the wrong target, and a few misspelled interface labels.
- **Tests**: around 820 unit tests, 156 in-game tests and an offline asset validator, so the numbers of the original stay the numbers of the original.

### Francais

Premiere version publique. Elle existe pour etre testee : tout ce qui suit fonctionne, le reste est liste dans le README.

- **Le moteur d'interface de l'original**, reimplemente dans le mod, avec les documents de mise en page de l'original reutilises tels quels. Les dix ecrans de machine (solaire, eolien, de phase, fusor d'imag, formeur de metal, noud sans fil et matrice, interfereur d'aptitude, developpeur) et l'arbre du developpeur d'aptitude sont dessines par lui.
- **Les quatre categories d'aptitudes et leurs 35 competences**, avec les trois cursus generiques : points de controle, surcharge, experience par competence, charges, recharges, touches, presets et affichage tete haute.
- **Le reseau d'energie imag** : generateurs solaire, eolien et de phase, nouds sans fil et matrice, fusor d'imag, formeur de metal, interfereur d'aptitude.
- **Le terminal de donnees**, ses applications (reglages, arbre de competences, lecteur media, a propos) et le tutoriel.
- **Le contenu de monde** : les minerais du mod, le fluide de phase imag et les succes.
- **Sept bugs de l'original corriges**, trouves en le lisant : un recul applique deux fois, une recherche qui pouvait viser la mauvaise cible, et quelques libelles d'interface mal orthographies.
- **Des tests** : environ 820 tests unitaires, 156 tests en jeu et un validateur de ressources, pour que les nombres de l'original restent ceux de l'original.