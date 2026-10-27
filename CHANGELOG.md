# Changelog

## 1.0.3-beta - le bloc passe derriere la bete / the block goes past the creature

### English

Six fixes on the teleporter's **Shift Teleport** (throw a held block, up to 35 blocks away), all of
them found in play. Keep a backup of your world.

- **The thrown block goes past the creature.** Aiming at a creature threw the block *onto* it: it
  landed on the ground the creature stood on, so the block seemed to stop on it — "the block stops
  on the entity instead of going behind it even when there is room". It now carries on along the
  gesture's axis, leaving the creature behind, and lands where that line meets the terrain — or at
  the end of the range when it meets nothing.
- **The destination box tells the truth.** The grey box drawn while the key is held sat on the
  creature's own cell, so the gesture looked like it stopped there. It now shows the real
  destination — the one the block will reach — while the red box on each crossed creature keeps
  saying who takes the hit.
- **The thrust hits everything on the line.** Damage stopped at the first creature, because the
  line ended at the aimed cell. It now runs to the block's destination: every creature it crosses
  takes the hit — the aimed one, those before it, and those behind it in the axis.
- **Aiming at the feet hits, aiming at a block places.** A creature sitting right on the aimed point
  is still the target, but only on the *top* face of a block and inside its own footprint. A side
  face is a block you aim at, so the block is now placed there instead of being thrown behind the
  creature.
- **Aim stops at walls.** The skills that pick their target from the look (thunder clap, ray
  barrage, flesh ripping, threatening teleport, directed shock, the plasma bomb) no longer see
  through blocks: a creature behind a wall is out of sight, and flesh ripping would rather refuse
  than spend control points on nothing.
- **Tests**: 971 unit tests and 182 in-game tests (including the six above), plus the offline asset
  audit — all green.

### Francais

Six corrections sur le **depose au loin** du teleporteur (lancer un bloc tenu, jusqu'a trente-cinq
blocs), toutes trouvees en jouant. Gardez une sauvegarde de votre monde.

- **Le bloc passe derriere la bete.** Viser une creature lancait le bloc *sur* elle : il se posait
  sur le sol qu'elle occupait, donc le bloc avait l'air de s'arreter sur elle — « le bloc s'arrete
  sur l'entite au lieu d'aller derriere si il y a de la place ». Il continue maintenant dans l'axe du
  geste, laissant la creature derriere lui, et se pose la ou cette ligne rencontre le terrain — ou
  au bout de la portee quand elle ne rencontre rien.
- **La boite de destination dit la verite.** La boite grise dessinee pendant le maintien se tenait
  sur la case de la creature, donc le geste avait l'air de s'y arreter. Elle montre maintenant la
  vraie destination — celle que le bloc rejoindra — et la boite rouge de chaque creature traversee
  continue de dire qui prend le coup.
- **Le coup d'epieu frappe toute la ligne.** Les degats s'arretaient a la premiere creature, la ligne
  finissant sur la case visee. Elle va maintenant jusqu'a la destination du bloc : tout ce qu'elle
  croise prend le coup — la bete visee, celles d'avant, et celles qui se tiennent derriere elle dans
  l'axe.
- **Viser les pieds frappe, viser un bloc pose.** Une creature posee sur le point vise reste la
  cible, mais seulement sur le *dessus* d'un bloc et dans sa propre empreinte. Une face de cote,
  c'est un bloc qu'on vise : le bloc s'y pose maintenant, au lieu de partir derriere la creature.
- **La visee bute sur les murs.** Les competences qui prennent leur cible dans le regard (orage,
  barrage de rayons, arrachage de chair, teleportation menacante, onde dirigee, bombe a plasma) ne
  voient plus au travers des blocs : une creature derriere un mur n'est plus en vue, et l'arrachage
  de chair prefere refuser plutot que depenser des points pour rien.
- **Tests** : 971 tests unitaires et 182 tests en jeu (dont les six ci-dessus), plus l'audit
  d'assets hors ligne — tous verts.

## 1.0.2-beta - la case lit le joueur, le vide ne coute rien / the box reads the player, the void costs nothing

### English

Three bugs found in multiplayer right after 1.0.1-beta. Keep a backup of your world.

- **The breaking box reads the player, not the machine.** It also read the server's lock
  (`general.destroyBlocks`) — from the **client's own** configuration file, which the server never
  sends to it (common configs are not synced). One player therefore had a box that could not be
  ticked at all, another had a perfectly normal one, and a box could sit **unticked while the
  server kept breaking blocks**. The box now shows the player's own choice, which the server
  applies and sends back; the server's lock stays where it belongs, on the server. Single player
  keeps the write-through: there the player *is* the server, and a ticked box must mean something.
- **Flesh ripping into the void costs nothing.** The packet paid the cost *before* the effect ran,
  and the effect gave up when the aim found no one — so spamming it in the air filled the overload
  for free. The skill now refuses to start without a target (no control points, no overload, no
  cooldown, no experience) and pays inside its own effect, when the hit lands — the original's
  `MSG_ABORT` and `consumeWithForce`.
- **Tests**: 970 unit tests and 179 in-game tests (including flesh ripping into the void), plus the
  offline asset audit — all green.

### Francais

Trois bugs trouves en multi juste apres la 1.0.1-beta. Gardez une sauvegarde de votre monde.

- **La case de la casse lit le joueur, pas la machine.** Elle lisait aussi le verrou du serveur
  (`general.destroyBlocks`) — dans le fichier de configuration **du client**, que le serveur ne lui
  envoie jamais (les configs communes ne sont pas synchronisees). Un joueur avait donc une case
  impossible a cocher, un autre une case parfaitement normale, et une case pouvait rester
  **decochee pendant que le serveur cassait des blocs**. La case ne montre plus que le choix du
  joueur, que le serveur applique et renvoie ; le verrou du serveur reste cote serveur. En solo, la
  case leve toujours le verrou avec elle : la, le joueur *est* le serveur, et une case cochee doit
  vouloir dire quelque chose.
- **Le flesh ripping dans le vide ne coute rien.** Le paquet payait le cout *avant* l'effet, et
  l'effet sortait sans rien faire quand la visee ne trouvait personne : spammer dans l'air
  remplissait donc l'overload pour rien. La competence refuse maintenant de partir sans cible (ni
  points, ni surcout, ni recharge, ni experience) et paie dans son propre effet, quand le coup
  tombe — le `MSG_ABORT` et le `consumeWithForce` de l'original.
- **Tests** : 970 tests unitaires et 179 tests en jeu (dont le flesh ripping dans le vide), plus
  l'audit d'assets hors ligne — tous verts.

## 1.0.1-beta - la casse au joueur, le liquide qui eteint / player-side block breaking, the liquid that douses

### English

Follow-up to 1.0.0-beta: two features the player asked for and the fixes they pulled in. Keep a
backup of your world.

- **Breaking blocks is now the player's choice.** The server keeps its own lock
  (`general.destroyBlocks`): it alone says whether anything may break at all. On top of it, each
  player decides **what they accept** — one master switch and **one sub-option per skill that
  digs** (railgun, meltdowner, the three mine rays, directed blastwave, groundshock, plasma
  cannon, storm wing), all ticked by default. A player can only ever *remove*: their choice never
  opens what the server closed. In single player the master switch also lifts the server's
  setting, so a ticked box always means what it says. The choice is sent to the server, which
  applies it and sends it back — the screen never shows a state the game does not have.
- **The mine ray finally obeys the rule.** It dug every single time, without even looking at the
  server's setting; it now follows the same lock as the other six.
- **The Imag phase liquid puts fire out.** The fire blocks it touches, a fire that is born against
  it, and **the flames of a body that walks in** — with the game's own extinguish sound, played
  once, for the player who is burning.
- **The settings screen grows a fold.** The breaking line carries a small arrow that shows or
  hides the per-skill lines: it points at the label when folded and down when unfolded, and it is
  drawn by the interface font, so it is as smooth as the text next to it.
- **Tests**: 970 unit tests and 178 in-game tests (including a player who refuses to dig and a
  burning body in the liquid), plus the offline asset audit — all green.

### Francais

Suite du 1.0.0-beta : deux demandes du joueur, et les corrections qu'elles ont tirees. Gardez une
sauvegarde de votre monde.

- **La casse des blocs appartient au joueur.** Le serveur garde son verrou
  (`general.destroyBlocks`) : lui seul dit si l'on peut casser, un point c'est tout. Par-dessus,
  chaque joueur decide **ce qu'il accepte** — une case generale et **une sous-option par competence
  qui creuse** (railgun, meltdowner, les trois mine rays, directed blastwave, groundshock, plasma
  cannon, storm wing), toutes cochees par defaut. Le joueur ne peut que **retirer** : son choix
  n'ouvre jamais ce que le serveur a ferme. En solo, la case generale leve aussi le reglage du
  serveur, pour qu'une case cochee ne mente jamais. Le choix part au serveur, qui l'applique et le
  renvoie — l'ecran n'affiche jamais un etat que le jeu n'a pas.
- **La mine ray obeit enfin a la regle.** Elle creusait a chaque tir, sans meme regarder le reglage
  du serveur ; elle suit maintenant le meme verrou que les six autres.
- **L'imag phase liquide eteint le feu.** Les blocs de feu qu'il touche, un feu qui nait contre lui,
  et **les flammes d'un corps qui le traverse** — avec le son d'extinction du jeu, joue une seule
  fois, pour le joueur qui brule.
- **L'ecran de reglage se plie.** La ligne de la casse porte une petite fleche qui montre ou cache
  les lignes par competence : elle vise le libelle quand c'est plie, le bas quand c'est deplie, et
  elle est dessinee par la police de l'interface — donc aussi lisse que le texte d'a cote.
- **Tests** : 970 tests unitaires et 178 tests en jeu (dont un joueur qui refuse de creuser et un
  corps en feu dans le liquide), plus l'audit d'assets hors ligne — tous verts.

## 1.0.0-beta - le railgun qui creuse, l'eclat qui tient / the railgun digs, the flare holds

### English

First **1.0.0** milestone, published as a **beta**: everything of 0.2.1, plus fifty commits of
gameplay, interface and cleanup. Keep a backup of your world.

- **The railgun digs.** Its shot opens a tunnel whose **depth follows the ammunition**: the coin
  pierces the whole beam, the ingot two thirds of it, a block the whole beam again — thirty percent
  wider and longer, so about ninety blocks in the combined shot.
- **The material of a block decides the power.** A **full iron or copper block** keeps the full
  combined damage (150-200); any other block falls to **70-120**, less than a coin at the same
  experience, with the **same radius** but **a third of the range**. A block is only fired through
  magnetism now (50 % railgun and 80 % magnet manipulation), never from a pocket, and the magnetism
  goes on cooldown with the shot.
- **The coin is worth more than the ingot** (75-125 against 40-80), and it turns from orange to light
  blue when the shot is ready. The iron path arms for twenty ticks.
- **Mine detection became a toggle**: one press opens it, a second closes it, the whole flare is paid
  **tick by tick**, the overload stays pinned, and the cooldown only lands when it closes. The
  **blindness now lasts the whole flare** (the fog follows the effect's remaining time, so a very
  short refresh used to fade out after five seconds), and its **sound follows the player and repeats**
  for as long as the flare holds. A filter screen lets one ore type stand out in green over the blue.
- **Mine detection got three times cheaper**: the scan stops where the drawing stops (91 965 block
  reads per scan become 24 405), allocates nothing per block, and resolves the chosen ore once. An
  opt-in log (`AC_MD_STATS=1`) reports scans, block reads, ores and quads every five seconds.
- **Teleport crits** got their own damage type, with its death message in all five languages; vector
  reflection and deviation let teleport damage through instead of absorbing it.
- **Frequency transmitter rebuilt** as a real terminal app with its overlay; the **media player** has
  its covers and its progress bar.
- **Developer, skill card, tree and terminal**: card icons and requirement lines, console fixes, tree
  fade, pixel-perfect Customize UI, the CP emblem drawn by shader, and the tutorial intro.
- **Cleanup**: the thirty-three dead per-skill key bindings are gone (with their language keys), the
  unused railgun beam rendering is gone (some three hundred lines), and the five sound events that
  neither the port nor the original ever played are documented as such instead of looking missing.
- **Translations**: four hardcoded strings now go through the language files; the tutorial scan went
  from three ticks to ten per player on the server.
- **Tests**: 960 unit tests, 176 in-game tests and an offline asset audit, all green.

### Francais

Premier jalon **1.0.0**, publie en **beta** : tout le 0.2.1, plus cinquante commits de jeu,
d'interface et de menage. Gardez une sauvegarde de votre monde.

- **Le railgun creuse.** Son tir ouvre un tunnel dont la **profondeur suit la munition** : la piece
  perce le trait entier, le lingot les deux tiers, le bloc a nouveau le trait entier — trente pour
  cent plus large et plus long, soit une petite centaine de blocs en tir combine.
- **La matiere du bloc decide de la puissance.** Un **bloc de fer ou de cuivre complet** garde les
  degats du tir combine (150 a 200) ; tout autre bloc tombe a **70 a 120**, donc moins qu'une piece a
  experience egale, avec le **meme rayon** mais **le tiers de la portee**. Un bloc ne part plus que
  par l'aimantation (50 % de railgun et 80 % de mag manip), jamais d'une poche, et l'aimantation part
  en recharge avec le tir.
- **La piece vaut plus que le lingot** (75 a 125 contre 40 a 80), et elle passe de l'orange au bleu
  clair quand le tir est pret. La voie du fer s'arme en vingt ticks.
- **La detection de minerais est devenue une bascule** : un appui l'allume, un second l'eteint, tout
  l'eclat se paie **tick par tick**, le surcoût reste epingle, et la recharge ne tombe qu'a la
  fermeture. **L'aveuglement tient maintenant tout l'eclat** (le brouillard suit ce qui reste de
  l'effet, donc un rafraichissement tres court s'effacait au bout de cinq secondes), et **son son suit
  le joueur et se relance** tant que l'eclat dure. Un ecran de filtre fait ressortir un minerai en
  vert sur le bleu.
- **La detection coute trois fois moins** : le balayage s'arrete la ou le dessin s'arrete (91 965
  blocs lus par balayage deviennent 24 405), n'alloue plus rien par bloc, et resout le minerai choisi
  une seule fois. Un journal optionnel (`AC_MD_STATS=1`) donne, toutes les cinq secondes, les
  balayages, les blocs lus, les minerais et les quads.
- **Les critiques de teleportation** ont leur propre type de degats, avec son message de mort dans
  les cinq langues ; la reflexion et la deviation vectorielles laissent passer ces degats au lieu de
  les absorber.
- **L'emetteur de frequence refait** en vraie application du terminal, avec son overlay ; le
  **lecteur media** a ses pochettes et sa barre de progression.
- **Developpeur, fiche, arbre et terminal** : icones et lignes de condition sur la fiche, console
  corrigee, fondu de l'arbre, Customize UI au pixel pres, embleme de CP par shader, et l'introduction
  du tutoriel.
- **Menage** : les trente-trois touches mortes par competence sont parties (avec leurs cles de
  langue), le rendu inutilise du faisceau du railgun aussi (environ trois cents lignes), et les cinq
  evenements de son que ni le port ni l'original ne jouaient sont maintenant ecrits comme tels au lieu
  de passer pour un oubli.
- **Traductions** : quatre textes en dur passent par la langue ; le balayage du tutoriel passe de
  trois ticks a dix par joueur, cote serveur.
- **Tests** : 960 tests unitaires, 176 tests en jeu et un audit d'assets hors ligne, tous verts.

## 0.2.1 - le teleporteur au complet, l'eolienne juste / the teleporter complete, the wind generator right

### English

Everything of 0.2.0, plus the four teleportations that were still off, the list of marks redone, and
an eolienne whose blades no longer tremble.

- **Flashing lands where you look.** Its height is read on the hit point, not on the block, so aiming
  at the side of the block you stand on no longer puts you underground with an invisible ghost. The
  ghost is measured from the **player's feet** (it used to be from the camera, which put it behind
  you in third person), the heading is read on the **yaw** (looking straight down used to lose it and
  send you backwards), the feet never land in a block, and the arrival takes the one-block hop
  whenever the head is clear. The slow fall after a jump is the original's `GravityCancellor` (forty
  ticks, +0.072 a tick) with sparks around the player, and the hold now lasts **three to fifteen
  seconds** instead of three to seven and a half, growing with experience.
- **Mark teleport** lands on top of a free-standing block, and its ghost shows where you stand
  rather than where you fall from.
- **The far deposit hits creatures too.** The port traced blocks only: aiming at a shulker with no
  wall behind it missed. The original traced both in one ray.
- **The list of marks is the original's again**: translucent borders (the blend was cut by the
  previous fill), a list that rebuilds itself when the marks change and scrolls without spilling
  under the panel, the four answers in one colour on four lines in a box that grows to fit them, the
  `Add...` field with its cursor, unavailable marks greyed with the right grey and their icons in the
  original's blue-grey, and no more crash when a mark is created. **Twelve marks** at most.
- **Vecmanip**: the reflection absorbs every hit, even at 0 % experience, and veils play a single
  sound per instant; the deviation sounds when you are hit. **V now ends the running skills.**
- **The wind generator**: the blade area is the block in front of the nacelle with the hub excluded,
  the angle is a **function of time read on a clock** (no more trembling while walking), the blades
  draw in a single pass, and the area is checked every two ticks instead of twenty.
- **Models no longer vanish** at the edge of the camera's field.
- **Tests**: 175 in-game tests, 932 unit tests.

### Francais

Tout le 0.2.0, plus les quatre teleportations qui n'etaient pas encore justes, la liste des marques
refaite, et une eolienne dont les pales ne tremblent plus.

- **Le scintillement arrive ou l'on regarde.** Sa hauteur se lit sur le point d'impact et non sur le
  bloc : viser le cote du bloc sur lequel on se tient ne pose plus dans le sol avec un fantome
  invisible. Le fantome se mesure depuis les **pieds du joueur** (il se mesurait depuis la camera, ce
  qui le mettait derriere soi en troisieme personne), le cap se lit sur le **lacet** (regarder pile
  en bas le perdait et envoyait a l'oppose), les pieds ne tombent jamais dans un bloc, et l'arrivee
  prend le saut d'un bloc quand la tete est degagee. La retombee lente qui suit un saut est le
  `GravityCancellor` de l'original (quarante ticks, +0,072 par tick) avec des etincelles autour du
  joueur, et le maintien dure desormais **de trois a quinze secondes** au lieu de trois a sept et
  demie, en grandissant avec l'experience.
- **La teleportation au marqueur** se pose sur le dessus d'un bloc degage, et son fantome montre ou
  l'on se tient plutot que d'ou l'on tombe.
- **Le depose au loin frappe aussi les creatures.** Le port ne tracait que les blocs : viser un
  shulker sans mur derriere le manquait. L'original tracait les deux d'un seul rayon.
- **La liste des marques est celle de l'original** : bordures translucides (le melange etait coupe
  par l'aplat precedent), liste qui se refait quand les marques changent et defile sans deborder sous
  le panneau, les quatre renseignements d'une seule couleur sur quatre lignes dans une boite qui
  grandit pour les tenir, le champ `Add...` avec son curseur, les marques inabordables grisees du bon
  gris et leurs icones dans le bleu-gris de l'original, et plus de plantage a la creation d'une
  marque. **Douze marques** au plus.
- **Vecmanip** : la reflexion absorbe tous les coups, meme a 0 % d'experience, et les voiles ne font
  qu'un son par instant ; la deviation sonne quand on est frappe. **V termine les competences en
  cours.**
- **L'eolienne** : la zone des pales est le bloc devant la nacelle, moyeu exclu ; l'angle est une
  **fonction du temps lue sur une horloge** (plus de tremblement en marchant) ; les pales se dessinent
  en une seule passe ; et la zone se relit toutes les deux ticks au lieu de vingt.
- **Les modeles ne disparaissent plus** au bord du champ de la camera.
- **Tests** : 175 tests en jeu, 932 tests unitaires.

## 0.2.0 - le HUD au complet et tout le jeu traduit / the finished HUD, the whole game translated

### English

Everything of 0.1.5, plus the two display items of the player's list, the whole game reading in
four languages, and a media player that survives leaving a world.

- **The control hint is the original's.** Its rows carry the three states of `KeyHintUI`: at rest
  the icon sits at 70 %, a running skill gets a **blue** halo and breathes, a preparation gets an
  **orange** one — the original's own rule, which is the other way round from what a reader
  expects. The halo is its eight glow images, read at the nearest neighbour so their gradients
  keep their transparent pixels; skill icons are crisp again (the skill tree and the developer
  screen used to hand them, once and for all, to a linear filter the hint then inherited). The
  four direction keys of the flashing and of the storm wing open their own column on the left,
  in the cross' order, and only once the skill is running. The grey of a cooldown is the
  original's thirty per cent, and the whole block sits where the 1.12.2's sits, letters included.
- **The CP bar says which preset you are on, and carries your power's emblem.** A preset switch
  shows the original's four numbered squares under the bar for two seconds, the one in service
  ringed; and the category's icon is cut into the bar's fill as `cpbar_cp.frag` did, so the
  silhouette of your power shows through.
- **The F4 menu is the original's again**, word for word — internal names, the seven spaces after
  `CP:`, `CPData.*`, numbers written with the system's decimal separator. The translation that had
  been invented for it is gone, with its fourteen language keys.
- **Text that does not fit now scrolls** instead of shrinking: a long line in a machine, in a
  field or in a list runs leftwards inside its box, at its normal size.
- **Everything is translated.** The thirteen tutorial pages exist in French, German and Spanish
  as well, and the whole interface is keyed — a test compares the five language files key by
  key, placeholder by placeholder.
- **The media player remembers.** Leaving a world freezes its clock and the track comes back
  **paused** where it was, and its volume is kept in the client config.
- **A machine gives back what it holds when you break it**, through one helper shared by all of
  them (multiblocks included: only the block carrying the block entity drops).
- **No more red chat messages**: a refusal — power off, not learnt, too few points, overloaded,
  jammed, on cooldown — is silent.
- **The music is in the chests, and the factors go further.** The three tracks never dropped
  anywhere: the port had only carried the induction factor's pool. Both pools are there now, and
  the tracks fall in **every** structure of the game — the 41 chest tables of 1.20.1, village
  chests included, minus the spawn bonus chest and the jungle temple's dispenser. The factors keep
  their five original tables and gain the **End city**, the four **bastion** chests and the
  **Nether fortress**. The tracks keep the original's odds (30 % per chest) despite there being
  three of them: the empty entry weighs 70 against three at 10.
- **Tests**: 172 in-game tests, 911 unit tests.

### Francais

Tout le 0.1.5, plus les deux points d'affichage de la liste du joueur, tout le jeu en quatre
langues, et un lecteur de media qui survit a la sortie d'un monde.

- **Le rappel des touches est celui de l'original.** Ses lignes portent les trois etats de
  `KeyHintUI` : au repos l'icone reste a 70 %, une competence qui s'execute prend un halo **bleu**
  et respire, une preparation un halo **orange** — la regle de l'original, a l'inverse de ce qu'on
  attend. Le halo est fait de ses huit images de lueur, lues au plus proche voisin pour que leurs
  degrades gardent leurs pixels transparents ; les icones des competences redeviennent nettes
  (l'arbre et l'ecran du developpeur les passaient, une fois pour toutes, a un filtre lisse dont
  le rappel heritait). Les quatre touches de direction du scintillement et des ailes de tempete
  ouvrent leur propre colonne a gauche, dans l'ordre de la croix, et seulement quand la competence
  tourne. Le gris d'une recharge est le trente pour cent de l'original, et le bloc entier tombe ou
  tombe celui de la 1.12.2, lettres comprises.
- **La barre de CP dit sur quel prereglage on est, et porte l'embleme de votre pouvoir.** Un
  changement de prereglage affiche deux secondes les quatre carres numerotes de l'original sous
  la barre, celui en service entoure ; et l'icone de la categorie est decoupee dans le remplissage
  comme le faisait `cpbar_cp.frag`, donc la silhouette du pouvoir se lit a travers.
- **Le menu F4 est redevenu celui de l'original**, mot pour mot — noms internes, les sept espaces
  apres `CP:`, les `CPData.*`, les nombres ecrits avec le separateur decimal du systeme. La
  traduction inventee pour lui est partie, avec ses quatorze cles de langue.
- **Le texte qui ne rentre pas defile** au lieu de retrecir : une longue ligne dans une machine,
  un champ ou une liste court vers la gauche dans sa boite, a sa taille normale.
- **Tout est traduit.** Les treize pages de tutoriel existent aussi en francais, allemand et
  espagnol, et toute l'interface passe par des cles — un test compare les cinq fichiers de langue
  cle par cle et placeholder par placeholder.
- **Le lecteur de media se souvient.** Quitter un monde fige son horloge et le morceau revient
  **en pause** la ou il en etait ; son volume est garde dans la config du client.
- **Une machine rend son contenu quand on la casse**, par un seul helper partage par toutes
  (multiblocs compris : seul le bloc porteur du block entity jette).
- **Plus de messages rouges dans le chat** : un refus — pouvoir eteint, non appris, points trop
  justes, surcharge, brouillage, recharge — est silencieux.
- **La musique est dans les coffres, et les facteurs vont plus loin.** Les trois morceaux ne
  tombaient nulle part : le port n'avait porte que le pool des facteurs d'induction. Les deux pools
  sont la maintenant, et les morceaux tombent dans **toutes** les structures du jeu — les 41 tables
  de coffre de la 1.20.1, coffres de village compris, moins le coffre de depart et le distributeur
  du temple de la jungle. Les facteurs gardent leurs cinq tables d'origine et gagnent la **cite de
  l'End**, les quatre coffres des **bastions** et la **forteresse du Nether**. Les morceaux gardent
  la chance de l'original (30 % par coffre) malgre les trois objets : le vide pese 70 contre trois
  a 10.
- **Des tests** : 172 tests en jeu, 911 tests unitaires.

## 0.1.5 - l'imag phase liquide / the imag phase liquid

### English

All of 0.1.4, plus the imag phase liquid rebuilt on the original's own renderer, a reworked
loot table, the whole game in four languages, and a config file that tunes the powers.

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
  IN, MODE and OUT, its slots in their place, the mode's icon in the MODE case and its name under
  them. JEI's own **tag pages are hidden** — a list of internal names is not a recipe. Both machines are
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
- **The config file tunes the powers now.** The four tables the 1.12.2 kept in `ac_ability` are
  back — `initCp`, `addCp`, `initOverload`, `addOverload`, six values each, level 0 to level 5 — and
  they are read, which a unit test proves by changing them and watching a player's ceiling follow.
  Three multipliers sit on top: `cpCostScale` and `overloadCostScale` (applied where the payment
  happens, so no cost can slip past them) and `cooldownScale`, which the icon's grey follows because
  it is the real duration that is stored. `expScale` speeds experience up. A skill is turned off
  from `ability.disabledSkills`, by short name (`arc_gen`) or full name (`electromaster.arc_gen`):
  it can then neither be cast nor placed on a key — the 1.12.2's `enabled = false`, in one list
  instead of thirty files. Every option is commented in French then in English, every number is
  bounded, and a table without exactly six values falls back to the default instead of crashing.
- **The data terminal's key acts on release, as the original's did.** Holding it no longer opened
  and closed the page in a loop: vanilla hands a held key's *repeats* to the open screen, and every
  repetition that closed the page turned the next one into a click that reopened it — thirty
  milliseconds a round. The key is now read physically and toggled once per press, open on release
  and close on release, exactly like the 1.12.2 `onKeyUp`; pressing it while typing in the page's
  chat no longer closes the page either.
- **No text leaves its box any more — it scrolls.** In the machines a label's width comes from the
  original's own layout document, written for English; in French, "Mot de passe" is wider than
  "Password" and ran over the password field. A text that does not fit is now clipped to its box and
  **scrolls**: a pause to read the start, one pixel every 25 ms, a pause at the end, then back to the
  beginning. It keeps its size — a shrunk label reads badly, a scrolling one reads in full. A field
  being typed in never moves. The name column of an editable row also stops where its bracket
  starts, instead of running underneath it.
- **Every machine was measured in every language.** `scripts/balayage-langues.groovy` opens the
  nine machine screens in English, French, German, Spanish and Chinese and compares each text's
  width to its box. English and Chinese report none, French four ("Nom du nœud", "Mot de passe",
  "Bande passante"), German eight ("KAPAZITÄT", "Knotenname", "Sende-Reichw.", "FLÜSSIGKEIT",
  "Erzeug.-Geschw.", "Nicht verbunden"), Spanish seven ("CAPACIDAD", "Nombre del nodo", "Alcance
  trans.", "Ancho de banda", "Teletransportador"); all of them scroll inside their box instead of
  running over the neighbouring column.
- **The About screen and the F4 menu speak four languages.** The Donate tab only had text in
  English and Chinese — the original's two — so French, German and Spanish read English; it now has
  its own. The staff roles ("Programming", "Game Design"...) were English names in the credits
  file: a `roles` table per language translates them, and a role a language forgets stays English.
  The donator list gets its title from the language files ("Donateurs"), and the F4 menu —
  "AcademyCraft developer info", "Ability not acquired", "Skill status", "[not learned]",
  "Level" — is translated too, while its measurements keep the original's own field names (`CP:`,
  `Overload:`, `CPData.*`): that is what a developer compares against the sources.
- **Breaking a machine gives its contents back.** Every machine held an inventory and none of them
  dropped it: the block entity went away with whatever was inside. Each machine now empties its
  inventory on the ground in its `onRemove` — the hook vanilla's furnace uses, which also covers
  blocks pushed by pistons or replaced — through `MachineDrops`, and the multi-block machines drop
  once, from the part that carries the block entity. The wind generator's fan was lost the same way.
- **The F4 menu is translated, whole.** Labels, yes and no, the category name and every skill name
  now follow the player's language (`ac.debug.*` and the usual `getDisplayKey`), with the value
  column computed instead of written by hand in each format: French, German and Spanish no longer
  mix `threatening_teleport` into a French screen.
- **MisakaCloud speaks French, German and Spanish.** Its thirteen pages existed only in English and
  Chinese; they are now written in the three other languages of the mod, tags (`![key …]`,
  `![image …]`) intact, and a unit test fails if a language lacks one of the thirteen pages or
  still carries the English title.
- **The skill tree app is the original's page now.** The port had invented a flat list of
  categories where the 1.12.2 drew its tree; the app now reads the very same page as the
  developer machine (`rework/page_developer`), with the player's ability on the left, the circuit
  backdrop on the right, every node in its place, its icon in a disc, its experience filling the
  ring, and the link that pulls it from the skill it depends on. Clicking a node opens the same
  fiche the machine opens — name, level, experience, description, and what is still missing to
  learn it — and, as in the original, the app teaches nothing: no button, because the machine is
  what has the energy and the right to teach. The tree itself is one piece of code now
  (`SkillTreeView`), shown twice, which is exactly how the original did it.
- **A pause stays a pause.** Pausing a track in the terminal and closing the screen started it
  again: vanilla's `setScreen(null)` calls `SoundManager.resume()`, which unpauses *every* channel
  of the engine, the mod's own included. The reader now puts its channel back on pause after it.
- **The clock stops with the game.** Escape pauses every sound of the engine, but the reader's
  clock kept running on its own: it now stops with `Minecraft.isPaused()` — the same flag that
  decides vanilla's `soundManager.pause()` — and starts again where it stopped, so the sound and
  the time shown stay together. Quitting the world *from* that menu no longer restarts the clock:
  leaving lifts the pause too, and without a world nothing resumes.
- **The reader's volume is remembered.** It is written to the client config
  (`academy-client.toml`, `media.volume`) when the slider is released, and read back at startup:
  no need to set it again every time.
- **The track survives a reconnection, and waits for you.** Leaving a world stops every sound of
  the game, the track's channel included, and the clock kept running at the menu. Leaving now
  freezes the clock and remembers the position, and the track comes back **paused** where it was
  left — pressing play resumes it from there, not from the start (the engine cannot seek in a
  stream, so it is handed one that reads and throws away the head of the track).
- **The control hint shows what a skill is doing.** Each row of the ability hint carries the
  three states of `KeyHintUI` now: at rest the icon sits at 70 % and does not move; a skill that
  is **running** — a charge you are holding, a hold, a flight — gets a **blue** halo and its icon
  breathes between 40 % and 100 %; a skill in its **preparation** gets an **orange** one. That is
  the original's own rule, and it is the other way round from what a reader expects: in 1.12.2
  any living context answered `ACTIVE` (blue), and only the skills that declared a preparation
  answered `CHARGE` — the plasma cannon before its window, the ground shock on its first five
  ticks, the storm wing before its wings open, and the railgun with an ingot in hand. The halo is
  the original's: eight glow images around the icon box, tinted with the state's colour, drawn
  in front of the icon. The 0,3 s fade-in is there too, and the key cap greys out while the
  skill is on cooldown (`!canUseAbility || tickLeft > 0`), which the port forgot. The halo is the
  original's eight glow images, with their transparent pixels: a first pass had replaced them by
  a plain line, and the player asked for the gradients back ("*tu n'as pas mis les pixels bleu
  transparent. Pareil pour le orange*"). The real cause of their smudge was the **filtering**:
  the skill tree and the developer screen hand their icons to `CGuiShaders.linear`, once for good,
  so the hint inherited a smoothed texture — blurred icons, a smeared halo, and semi-transparent
  pixels that were not. The hint now reads every texture — glow, plate, cap, frame, icon — at the
  nearest neighbour, as the original did, and gives the icons back their smoothing afterwards for
  those two screens.
- **The direction column is back.** A skill that reads the four movement keys — the teleport's
  flashing, the storm wing — opens a **second column** to the left of the first: four rows with
  the W/A/S/D caps (read like the cross, the forward key on top), the flashing's own four icons
  (`abilities/teleporter/flashing/a|d|w|s`), and the direction you are aiming lit in blue. The
  original registered those four keys in a separate group and drew one column per group, 200
  units apart; the port had lost the second one. It opens **only once the skill is running**:
  the storm wing adds its four keys when its wings open (`initKeys()` right after
  `state = STATE_ACTIVE`) and forgets them when it closes, so nothing shows up while it charges.
- **The hint sits where the original sits, and its letters are centred.** Put next to a capture
  of the 1.12.2 (same window, same interface scale), the whole block — plate, cap and frame, not
  just the icon — landed **two pixels too low**, and its key letters were **eleven percent too
  big and a pixel low**: the label was drawn as a ten-pixel text scaled by 32/9, the ratio of the
  *game's* font, instead of asking the mod's own font for a 32-cell as `FontOption(32)` did. The
  block now lifts by the measured three units, and the letter takes its cell's metrics, so it is
  centred in its cap like the original's — and then two units to the left, because the player
  still read it a pixel too far right.
- **The F4 menu is the original's again.** The port had translated it, with computed columns;
  the player no longer recognised his own menu. Its lines are now the 1.12.2 ones **word for
  word**: `AcademyCraft developer info`, `Skill status`, the internal names (`electromaster`,
  `arc_gen` — not the tree's translated ones), `CP:` followed by its seven spaces,
  `CPData.canUseAbility`, `CPData.activated`, `CPData.addMaxCP`, `CPData.interfering` and
  ` AData.levelProgress`, numbers written with the system's decimal separator as they were then.
  The fourteen language keys that only existed for that translation are gone from the five files.
- **The CP bar says which preset you are on.** Changing preset shows the original's four numbered
  squares under the bar for two seconds, the one in service ringed with a white halo — a question
  the port's HUD left unanswered ("*quand on change de preset, dans le CP indicator on est cense
  voir dans quel preset on se trouve*"). Same geometry as 1.12.2's `drawPresetHint` (squares of 52
  every 62, from 580 by 136), its fade-in only when no other switch happened within three seconds,
  and its text that never goes fully transparent. The clock is read on the server's preset sync, so
  a switch coming from the presets screen or from a category change shows it too.
- **The CP bar carries your power's emblem.** The original cut the category's icon out of the bar
  with `cpbar_cp.frag` (`alpha *= 1 - icon.a`), so the silhouette of your power showed through with
  the bar's plate behind it; the port had recorded that as an accepted gap. It is drawn now, in the
  same place and size (65 units at 857 by 43), by painting the plate's own grey in the icon's shape
  — the icon's tint is pushed to white first, because only its opacity is supposed to count. Its
  coverage is full for now: the player wanted it darker than a see-through watermark and asked to
  leave the question there ("*pour l'instant remet le a 1, on verra plus tard*").
- **The music is in the chests, and the factors go further.** The three tracks never dropped
  anywhere: the port had only carried the induction factor's pool. Both pools are there now, and
  the tracks fall in **every** structure of the game — the 41 chest tables of 1.20.1, village
  chests included, minus the spawn bonus chest and the jungle temple's dispenser. The factors keep
  their five original tables and gain the **End city**, the four **bastion** chests and the
  **Nether fortress**. The tracks keep the original's odds (30 % per chest) despite there being
  three of them: the empty entry weighs 70 against three at 10.
- **Tests**: 172 in-game tests, 911 unit tests.

### Francais

Tout le 0.1.4, plus l'imag phase liquide refaite sur le rendu de l'original, une table de butin
retravaillee, tout le jeu en quatre langues, et un fichier de config qui regle les pouvoirs.

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
  machine, avec IN, MODE et OUT, ses emplacements a leur place, l'icone du mode dans sa case MODE
  et son nom dessous. Les **pages de tags de JEI sont masquees** — une liste de noms internes n'est
  pas une recette. Les deux machines sont declarees comme catalyseurs : la touche "utilisations" sur un formeur ouvre
  ses recettes. JEI reste **facultatif**, en soft depend : le mod se lance sans lui.
- **CraftTweaker parle aux machines.** `mods.academy.MetalFormer.addRecipe(<entree>, <sortie>, "incise")`,
  `removeRecipe(<sortie>)`, `removeRecipeByInput(<entree>, "plate")`, `removeAllRecipes()`, et la
  meme chose pour `mods.academy.ImagFusor` avec le nombre de millibassins. Les scripts sont
  **rejouables** : une recette remplace celle qui traitait deja la meme entree, donc un `/reload`
  ne duplique jamais rien. Soft depend lui aussi.
- **Les deux recettes du developpeur nomment un lit rouge** au lieu du tag `minecraft:beds`. Le
  tag venait du port (la 1.12.2 n'avait qu'un seul lit) ; dans JEI il transformait l'emplacement en
  tag, ce qui ne disait rien de ce qu'il fallait y mettre.
- **Le fichier de config regle enfin les pouvoirs.** Les quatre tables que la 1.12.2 tenait dans
  `ac_ability` sont revenues — `initCp`, `addCp`, `initOverload`, `addOverload`, six valeurs
  chacune, du niveau 0 au niveau 5 — et elles sont lues : un test unitaire les change et regarde le
  plafond d'un joueur suivre. Trois multiplicateurs se posent dessus : `cpCostScale` et
  `overloadCostScale` (appliques la ou le paiement se fait, donc aucun cout ne peut y echapper) et
  `cooldownScale`, que le gris de l'icone suit puisque c'est la duree REELLE qui est rangee.
  `expScale` accelere l'experience. Une competence s'eteint dans `ability.disabledSkills`, par nom
  court (`arc_gen`) ou complet (`electromaster.arc_gen`) : elle ne se lance plus et ne se range
  plus sur une touche — le `enabled = false` de la 1.12.2, en une liste au lieu de trente
  fichiers. Chaque reglage est commente en francais puis en anglais, chaque nombre est borne, et
  une table qui n'a pas exactement six valeurs retombe sur le defaut au lieu de planter.
- **La touche du terminal de donnees agit au relachement**, comme celle de l'original. La garder
  enfoncee n'ouvre plus et ne referme plus la page en boucle : le jeu livre les REPETITIONS d'une
  touche tenue a l'ecran ouvert, et chaque repetition qui refermait la page transformait la
  suivante en clic qui la rouvrait — trente millisecondes par tour. La touche est maintenant lue
  physiquement et basculee une fois par appui : ouverture au relachement, fermeture au relachement,
  exactement le `onKeyUp` de la 1.12.2 ; et l'appuyer pendant qu'on ecrit dans le chat de la page
  ne la referme plus.
- **Plus aucun texte ne sort de sa boite : il defile.** Dans les machines, la largeur d'un libelle
  vient du document de mise en page de l'original, ecrit pour l'anglais ; en francais, « Mot de passe »
  est plus large que « Password » et passait par-dessus le champ du mot de passe. Un texte qui ne tient
  pas est maintenant coupe a sa boite et **defile** : une pause pour lire le debut, un pixel toutes les
  25 ms, une pause a la fin, puis retour au debut. Il garde sa taille — un libelle retreci se lit mal,
  un libelle qui defile se lit en entier. Un champ en cours de saisie ne bouge jamais. La colonne des
  noms d'une ligne modifiable s'arrete aussi la ou commence son crochet, au lieu de passer dessous.
- **Chaque machine a ete mesuree dans chaque langue.** `scripts/balayage-langues.groovy` ouvre les
  neuf ecrans de machine en anglais, francais, allemand, espagnol et chinois, et compare la largeur
  de chaque texte a celle de sa boite. L'anglais et le chinois n'en signalent aucun, le francais
  quatre (« Nom du nœud », « Mot de passe », « Bande passante »), l'allemand huit (« KAPAZITÄT »,
  « Knotenname », « Sende-Reichw. », « FLÜSSIGKEIT », « Erzeug.-Geschw. », « Nicht verbunden »),
  l'espagnol sept (« CAPACIDAD », « Nombre del nodo », « Alcance trans. », « Ancho de banda »,
  « Teletransportador ») ; tous defilent dans leur boite au lieu de passer sur la colonne voisine.
- **L'ecran A propos et le menu F4 parlent quatre langues.** L'onglet des dons n'avait de texte
  qu'en anglais et en chinois — les deux de l'original — donc le francais, l'allemand et l'espagnol
  lisaient l'anglais ; il a maintenant le sien. Les noms de role (« Programming », « Game Design »...)
  etaient des noms anglais dans le fichier des credits : une table `roles` par langue les traduit, et
  un role qu'une langue oublie reste en anglais. La liste des donateurs prend son titre dans les
  fichiers de langue (« Donateurs »), et le menu F4 — « Infos développeur AcademyCraft », « Aucune
  aptitude acquise », « État des compétences », « [non apprise] », « Niveau » — est traduit aussi,
  tandis que ses mesures gardent les noms de champs de l'original (`CP:`, `Overload:`, `CPData.*`) :
  c'est ce qu'un developeur compare aux sources.
- **Casser une machine rend son contenu.** Toutes les machines rangeaient des objets et aucune ne
  les rendait : le block entity disparaissait avec ce qu'il contenait. Chaque machine vide maintenant
  son inventaire au sol dans son `onRemove` — le point d'accroche du four de Minecraft, qui couvre
  aussi un bloc pousse par un piston ou remplace — par `MachineDrops`, et les machines multiblocs
  ne le font qu'une fois, depuis la partie qui porte le block entity. L'helice de l'eolienne se
  perdait de la meme facon.
- **Le menu F4 est traduit en entier.** Libelles, oui et non, nom de la categorie et nom de CHAQUE
  competence suivent maintenant la langue du joueur (`ac.debug.*` et les `getDisplayKey` habituels),
  et la colonne des valeurs se calcule au lieu d'etre ecrite a la main dans chaque format : le
  francais, l'allemand et l'espagnol ne melangent plus `threatening_teleport` a un ecran francais.
- **Le MisakaCloud parle francais, allemand et espagnol.** Ses treize pages n'existaient qu'en
  anglais et en chinois ; elles sont ecrites dans les trois autres langues du mod, balises
  (`![key …]`, `![image …]`) intactes, et un test unitaire echoue si une langue perd une des treize
  pages ou garde le titre anglais.
- **L'application Arbre de competences est la page de l'original.** Le port avait invente une
  liste plate de categories la ou la 1.12.2 dessinait son arbre ; l'application lit maintenant la
  **meme** page que la machine du developpeur (`rework/page_developer`) : l'aptitude du joueur
  a gauche, le fond de circuit a droite, chaque competence a sa place, son icone dans un disque,
  son experience qui remplit l'anneau, et le trait qui la tire de celle dont elle depend. Cliquer
  un noeud ouvre la meme fiche que la machine — nom, niveau, experience, description, et ce qui
  manque encore pour l'apprendre — et, comme dans l'original, l'application n'apprend rien : aucun
  bouton, parce que c'est la machine qui a l'energie et le droit d'enseigner. L'arbre lui-meme
  n'est plus qu'un seul morceau de code (`SkillTreeView`), montre deux fois — exactement ce que
  faisait l'original.
- **Une pause reste une pause.** Mettre un morceau en pause dans le terminal puis refermer
  l'ecran le relancait : le `setScreen(null)` de Minecraft appelle `SoundManager.resume()`, qui
  remet en marche **tous** les canaux du moteur, celui du mod compris. Le lecteur repose
  maintenant sa pause apres lui.
- **Le chrono s'arrete avec le jeu.** Echap met tous les sons du moteur en pause, mais l'horloge du
  lecteur continuait de tourner toute seule : elle s'arrete maintenant avec `Minecraft.isPaused()`
  — le drapeau qui decide du `soundManager.pause()` de vanilla — et repart ou elle s'etait arretee,
  donc le son et le temps affiche restent ensemble. Et quitter le monde depuis ce menu ne relance
  plus l'horloge : partir leve aussi la pause, et sans monde rien ne repart.
- **Le volume du lecteur est retenu.** Il s'ecrit dans la config du client
  (`academy-client.toml`, `media.volume`) quand on lache la glissiere, et se relit au demarrage :
  plus besoin de le regler a chaque fois.
- **Le morceau survit a une reconnexion, et il attend.** Quitter un monde arrete tous les sons du
  jeu, le canal du morceau compris, et l'horloge continuait de tourner au menu. La deconnexion
  fige maintenant l'horloge et retient la position, et le morceau revient **en pause** la ou il en
  etait — le bouton lecture le reprend de la, et non du debut (le moteur ne sait pas se deplacer
  dans un flux, on lui en donne donc un qui lit et jette la tete du morceau).
- **Le rappel des touches dit ce que fait la competence.** Chaque ligne porte maintenant les
  trois etats de `KeyHintUI` : au repos l'icone reste a 70 % et ne bouge pas ; une competence qui
  **s'execute** — une charge qu'on tient, un maintien, un vol — prend un halo **bleu** et son
  icone respire entre 40 et 100 % ; une competence en **preparation** prend un halo **orange**.
  C'est la regle de l'original, et elle est a l'inverse de ce qu'on attend : en 1.12.2 tout
  contexte vivant repondait `ACTIVE` (bleu), et seules les competences qui declaraient une
  preparation repondaient `CHARGE` — le canon a plasma avant sa fenetre, l'onde de choc sur ses
  cinq premiers ticks, les ailes avant de s'ouvrir, et le railgun avec un lingot en main. Le halo
  est celui de l'original : huit images de lueur autour de la boite de l'icone, teintees de la
  couleur de l'etat, posees devant l'icone. Le fondu d'apparition de 0,3 s est la aussi, et le
  capuchon grise pendant la recharge (`!canUseAbility || tickLeft > 0`), que le port avait oublie.
  Le halo est fait des huit images de lueur de l'original, avec leurs pixels transparents : un
  premier passage les avait remplacees par un filet uni, et le joueur a redemande les degrades
  (« tu n'as pas mis les pixels bleu transparent. Pareil pour le orange »). La vraie cause de
  leur tache etait le **filtrage** : l'arbre des competences et l'ecran du developpeur passent
  leurs icones a `CGuiShaders.linear`, une fois pour toutes, et le rappel heritait donc d'une
  texture lissée — icones floues, halo baveux, et des pixels semi-transparents qui ne l'etaient
  plus. Le rappel lit maintenant toutes ses textures — lueur, plaque, capuchon, cadre, icone — au
  plus proche voisin, comme l'original, et rend leur lissage aux icones apres coup pour ces deux
  ecrans-la.
- **La colonne des directions est revenue.** Une competence qui lit les quatre touches de
  deplacement — le scintillement du teleporteur, les ailes de tempete — ouvre une **seconde
  colonne** a gauche de la premiere : quatre lignes avec les capuchons W/A/S/D (lues comme la
  croix, la touche d'avance en haut), les quatre images du scintillement
  (`abilities/teleporter/flashing/a|d|w|s`), et la direction visee allumee en bleu. L'original
  enregistrait ces quatre touches dans un groupe a part et dessinait une colonne par groupe, a
  200 unites l'une de l'autre ; le port avait perdu la seconde. Elle ne s'ouvre qu'une fois la
  competence **en marche** : les ailes de tempete n'ajoutent leurs quatre touches qu'a
  l'ouverture des ailes (`initKeys()` juste apres `state = STATE_ACTIVE`) et les oublient en se
  refermant, donc rien n'apparait pendant la charge.
- **Le bloc est a sa place, et ses lettres sont centrees.** Pose a cote d'une capture de la
  1.12.2 (meme fenetre, meme echelle d'interface), le bloc entier — plaque, capuchon et cadre, pas
  seulement l'icone — tombait **deux pixels trop bas**, et ses lettres de touche etaient **onze
  pour cent trop grosses et un pixel trop bas** : l'etiquette etait dessinee comme un texte de dix
  pixels grossi de 32/9, le rapport de la police du *jeu*, au lieu de demander a la police du mod
  une cellule de 32 comme le faisait `FontOption(32)`. Le bloc remonte des trois unites mesurees,
  et la lettre prend les metriques de sa cellule : elle est centree dans son capuchon comme chez
  l'original — puis deux unites plus a gauche, parce que le joueur la lisait encore un pixel trop
  a droite.
- **Le gris d'une recharge est celui de l'original.** Le voile etait dessine au quart de sa
  force : un `blit` laisse sa propre couleur — et son alpha — sur le shader, et `fill` multiplie
  sa couleur par celle-la, donc trente pour cent devenaient douze. Dessine au vrai trente pour
  cent, le carre est aussi clair que celui de la 1.12.2.
- **Le menu F4 est redevenu celui de l'original.** Le port l'avait traduit, avec des colonnes
  calculees ; le joueur n'y reconnaissait plus son menu. Ses lignes sont maintenant celles de la
  1.12.2 **mot pour mot** : `AcademyCraft developer info`, `Skill status`, les noms internes
  (`electromaster`, `arc_gen`, et non ceux de l'arbre), `CP:` suivi de ses sept espaces,
  `CPData.canUseAbility`, `CPData.activated`, `CPData.addMaxCP`, `CPData.interfering` et
  ` AData.levelProgress`, les nombres ecrits avec le separateur decimal du systeme comme a
  l'epoque. Les quatorze cles de langue qui n'existaient que pour cette traduction ont quitte les
  cinq fichiers.
- **La barre de CP dit sur quel prereglage on est.** Un changement de prereglage affiche pendant
  deux secondes les quatre carres numerotes de l'original sous la barre, celui en service entoure
  d'un halo blanc — une question que le HUD du port laissait sans reponse (« quand on change de
  preset, dans le CP indicator on est cense voir dans quel preset on se trouve »). Meme geometrie
  que le `drawPresetHint` de la 1.12.2 (carres de 52 tous les 62, a partir de 580 par 136), meme
  fondu d'entree qui ne se fait que si aucun autre changement n'a eu lieu depuis trois secondes,
  et meme chiffre qui ne s'efface jamais tout a fait. L'horloge se lit sur la synchronisation du
  serveur : un changement venu de l'ecran des prereglages ou d'une categorie s'affiche donc aussi.
- **La barre de CP porte l'embleme de votre pouvoir.** L'original decoupait l'icone de la
  categorie DANS la barre (`cpbar_cp.frag` : `alpha *= 1 - icone.a`), donc la silhouette du pouvoir
  se voyait a travers, avec la plaque de la barre derriere ; le port avait note cet ecart comme
  assume. Il est dessine maintenant, a la meme place et a la meme taille (65 unites a 857 par 43),
  en peignant le gris de la plaque dans la forme de l'icone — la teinte de l'icone est d'abord
  poussee au blanc, puisque seule son opacite doit compter. La couverture est pleine pour
  l'instant : le joueur la voulait plus sombre qu'un filigrane, et a demande de laisser la
  question la (« pour l'instant remet le a 1, on verra plus tard »).
- **Des tests** : 171 tests en jeu, 905 tests unitaires.

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