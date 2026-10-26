package cn.academy.client.hud;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.client.AbilityKeyBindings;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.preset.client.ClientPresetData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Le rappel des touches d'aptitude : le <i>Control Hint</i> de l'original.
 *
 * <p>Une ligne par touche d'aptitude, posee a la place que la config lui donne. Elle montre le
 * <b>capuchon</b> de la touche, l'<b>icone</b> de la competence que le prereglage en service y a
 * rangee, son <b>halo d'etat</b> (orange en charge, bleu en execution) et la <b>recharge</b> qui
 * reste (le bas de l'icone s'assombrit). C'est le portage de {@code KeyHintUI} : ses mesures,
 * son echelle et ses textures sont les siennes, et tout est dans {@link KeyHintVisuals}, qui se
 * relit en JUnit.
 *
 * <p>Une <b>seconde colonne</b> s'ouvre a gauche de la premiere quand la competence tenue lit les
 * quatre directions (le scintillement, les ailes de tempete) : c'est la colonne ZQSD de
 * l'original, qui enregistrait quatre touches de plus dans un groupe a part. Voir
 * {@link #drawDirectionColumn}.
 *
 * <p>Le rappel apparait avec un fondu de 0,3 s, comme chez l'original : il ne se dessine qu'avec
 * l'aptitude allumee, donc le fondu se voit a chaque allumage.
 */
public final class KeyHintHud {

    private static final ResourceLocation PLATE = texture("back");
    private static final ResourceLocation FRAME = texture("icon_back");

    /** La plaque en degrade fait 256x83 chez l'original, etiree sur la largeur de la ligne. */
    private static final int PLATE_TEX_W = 256;
    private static final int PLATE_TEX_H = 83;

    /**
     * La cellule de l'etiquette du capuchon, en unites : le {@code new FontOption(32, CENTER, ...)}
     * de l'original.
     *
     * <p>C'est la CELLULE, et non un facteur d'echelle : le port dessinait un texte de 10 pixels
     * grossi de 32/9 (le rapport de la police du JEU), qui sortait donc 11 % trop gros et trois
     * pixels trop bas — le joueur : « avec la nouvelle police d'ecriture les lettres pour les
     * competences sont decalees ». La police du mod se demande en cellules, comme chez l'original.
     */
    private static final int LABEL_CELL = 32;

    /**
     * Le micro-decalage de l'etiquette, en unites.
     *
     * <p>Mesure sur les captures : avec la cellule seule, l'encre de la lettre tombait huit
     * unites sous le haut du capuchon la ou le vrai mod la pose a sept (un pixel a l'echelle 3,
     * deux unites) — c'est {@link #LABEL_LIFT}. Et elle etait un pixel trop a droite : la police
     * du port est un peu plus large que celle de l'original (elle est gravee avec un poids de
     * 0,3), donc une lettre centree par sa largeur deborde plus a droite. Le joueur : « les
     * lettres dans les casses sont a la bonne hauteur mais pas assez a gauche ».
     */
    private static final int LABEL_LIFT = -2;
    private static final int LABEL_LIFT_X = -2;

    /** La duree du fondu d'apparition, en secondes (le {@code 0.3} de l'original). */
    private static final double FADE_TIME = 0.3;

    /** L'heure (au sens d'{@link cn.academy.ability.client.EffectClock}) de la derniere image. */
    private static double lastFrame = -1;
    /** L'heure ou le rappel est reapparu : c'est de la que part son fondu. */
    private static double showTime;

    private KeyHintHud() {}

    /**
     * Dessine le rappel, ou l'ecran et la config le posent.
     *
     * <p>Rien n'est dessine tant que l'aptitude est eteinte : l'original accrochait cet element
     * a {@code CPData.isActivated()}, comme le temoin de CP. Les deux apparaissent et
     * disparaissent donc ensemble.
     */
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        AbilityData data = ClientAbilityData.get();
        if (!data.isActivated()) return;

        HudElement element = HudElement.KEY_HINT;
        HudLayout layout = HudConfig.read();
        int left = layout.placeX(element, screenWidth, element.getWidth());
        int top = layout.placeY(element, screenHeight, element.getHeight());

        // L'aptitude ne peut pas servir : l'original grisait le capuchon plutot que de cacher
        // le rappel, pour que le joueur voie ce qu'il a sous la main. C'est le VERROU qui
        // compte ici — il tient jusqu'a la fin de la descente — et non l'etat montre par la
        // barre, qui s'efface des le delai ecoule.
        boolean usable = !data.isOverloadRecovering() && !data.isInterfered();

        // La respiration des etats (halo orange en charge, bleu en execution, voir
        // KeyHintVisuals.State) : l'original la lisait sur son GameTimer, donc sur le temps
        // REEL. Le port lit EffectClock, qui est le meme temps moins les pauses — sinon la
        // pulsation continuerait derriere le menu d'Echap, ce que le joueur a deja signale
        // pour les arcs.
        double now = cn.academy.ability.client.EffectClock.now() / 1000.0;
        float wave = KeyHintVisuals.sinWave(now);

        // Le fondu d'apparition de 0,3 s : l'original le declenchait quand une image s'ecoulait
        // plus de 0,3 s apres la precedente, c'est-a-dire quand le rappel REVIENT (aptitude
        // rallumee, ou element qui reapparait). Sans lui, le rappel se pose d'un coup.
        if (now - lastFrame > FADE_TIME) showTime = now;
        lastFrame = now;
        float fade = (float) Math.min(1.0, (now - showTime) / FADE_TIME);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left, top, 0);
        pose.scale(KeyHintVisuals.SCALE, KeyHintVisuals.SCALE, 1.0f);
        // Le bloc entier remonte de trois unites : c'est la correction mesuree contre la capture du
        // vrai mod (voir KeyHintVisuals.CONTENT_SHIFT_Y). Elle porte sur le CONTENU, donc l'apercu
        // de l'ecran de reglage ne bouge pas.
        pose.translate(0, KeyHintVisuals.CONTENT_SHIFT_Y, 0);

        // La PREMIERE colonne : les quatre touches d'aptitude. L'original reculait toute sa
        // colonne avant de dessiner, sinon plaque, cadre et icone mordent sur le bord droit de
        // l'ecran (voir KeyHintVisuals.CONTENT_SHIFT_X, ou le chiffre est explique).
        pose.pushPose();
        pose.translate(KeyHintVisuals.contentShift(0), 0, 0);

        // Seules les touches qui portent une competence s'affichent, et elles se suivent sans
        // trou : c'est ce que faisait l'original, qui ne parcourait que ses delegues existants
        // et avancait d'une ligne a chacun.
        java.util.List<String> names = new java.util.ArrayList<>(KeyHintVisuals.ROWS);
        for (int slot = 0; slot < KeyHintVisuals.ROWS; slot++) {
            names.add(ClientPresetData.skillAt(slot));
        }
        int row = 0;
        for (int slot : KeyHintVisuals.visibleSlots(names)) {
            drawSlotRow(graphics, data, slot, row * KeyHintVisuals.ROW_STEP, usable, wave, fade);
            row++;
        }
        pose.popPose();

        // Et la SECONDE, celle des directions : voir drawDirectionColumn.
        drawDirectionColumn(graphics, data, usable, wave, fade);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        pose.popPose();
        RenderSystem.disableBlend();
    }

    /**
     * La colonne des quatre directions, ouverte seulement par une competence qui s'en sert.
     *
     * <p>C'est la deuxieme colonne de l'original, celle qui manquait au port : ses competences
     * <b>enregistraient quatre touches de plus</b> dans un groupe a part ({@code KEY_GROUP}), une
     * par direction de deplacement, et {@code KeyHintUI} dessinait <b>une colonne par groupe</b>
     * — d'ou un deplacement de 200 unites par groupe. Le scintillement s'en sert pour ses quatre
     * sauts (A, D, W, S, chacune avec son image) et les ailes de tempete pour se diriger (meme
     * icone sur les quatre lignes). Le joueur decrit ces lignes comme « les carres des
     * competences a directions (ZQSD) ».
     *
     * <p>La ligne s'allume de l'etat de sa touche : l'original rendait ACTIVE sur la direction
     * tenue et IDLE sur les autres, donc un halo bleu sur celle qui va partir.
     */
    private static void drawDirectionColumn(GuiGraphics graphics, AbilityData data,
                                            boolean usable, float wave, float fade) {
        String skillName = directionalSkill();
        if (skillName == null) return;
        Skill skill = KeyHintVisuals.skillByName(skillName);
        if (skill == null) return;
        // Les quatre carres n'apparaissent qu'une fois la competence UTILISABLE, c'est-a-dire
        // en bleu. Chez l'original, les ailes de tempete n'enregistraient leurs touches de
        // direction qu'au passage a l'etat ACTIF (`initKeys()` juste apres `state = STATE_ACTIVE`)
        // et les oubliaient en se refermant ; pendant la charge, la colonne n'existait donc pas.
        // Le joueur : « on voit les directions pendant que la competence est en chargement, donc
        // en orange, alors que normalement ces carres n'apparaissent qu'une fois utilisable ».
        if (KeyHintVisuals.stateOf(skill, data, false) != KeyHintVisuals.State.ACTIVE) return;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(KeyHintVisuals.contentShift(1), 0, 0);
        for (int row = 0; row < KeyHintVisuals.DIRECTION_ROWS.length; row++) {
            int i = KeyHintVisuals.DIRECTION_ROWS[row];
            String path = KeyHintVisuals.directionIcon(skillName, i);
            ResourceLocation icon = path == null
                    ? skill.getHintIcon()
                    : ResourceLocation.fromNamespaceAndPath("academy", path);
            KeyHintVisuals.State state = cn.academy.ability.client.AbilityClientEvents.directionHeld(i)
                    ? KeyHintVisuals.State.ACTIVE
                    : KeyHintVisuals.State.IDLE;
            drawRow(graphics, cn.academy.ability.client.AbilityClientEvents.directionKey(i), icon,
                    state, 0f, row * KeyHintVisuals.ROW_STEP, usable, wave, fade);
        }
        pose.popPose();
    }

    /**
     * La competence tenue qui ecoute les directions, ou {@code null}.
     *
     * <p>Deux competences du port lisent les touches de deplacement (le scintillement et les
     * ailes de tempete) ; c'est leur maintien ouvert qui dit que la colonne a quelque chose a
     * montrer, et c'est lui qui donne les icones.
     */
    private static String directionalSkill() {
        for (String name : cn.academy.ability.client.ClientCharge.openSkills()) {
            Skill skill = KeyHintVisuals.skillByName(name);
            if (skill != null && skill.listensToDirections()) return name;
        }
        return null;
    }

    /** Une ligne de la premiere colonne : la competence rangee sur cette touche. */
    private static void drawSlotRow(GuiGraphics graphics, AbilityData data, int slot, int y,
                                    boolean usable, float wave, float fade) {
        String name = ClientPresetData.skillAt(slot);
        Skill skill = KeyHintVisuals.skillByName(name);

        // La duree vient de la donnee du JOUEUR (celle qui a ete posee), et non de la courbe :
        // trois competences posent leur recharge depuis leur effet et n'annoncent aucune duree
        // (voir AbilityData.getCooldownTotal), donc leur icone ne s'estompait jamais.
        float remaining = 0f;
        if (skill != null) {
            int total = data.getCooldownTotal(skill);
            if (total <= 0) total = skill.getCooldownTicks(data);
            remaining = KeyHintVisuals.cooldownFraction(data.getCooldown(skill), total);
        }

        // L'ARMEMENT, ou la PIECE EN L'AIR : les deux fenetres du railgun, et l'original les
        // montrait de la meme couleur — l'orange de la preparation. Le joueur du 26/10 : « si je
        // lance une piece en l'air, la competence du railgun brille en orange ».
        boolean armed = cn.academy.ability.client.AbilityClientEvents.armingTicks(slot) > 0
                || (skill == cn.academy.ability.electromaster.ElectromasterCategory.RAILGUN
                        && cn.academy.ability.electromaster.RailgunSkill.coinInFlight(
                                net.minecraft.client.Minecraft.getInstance().player));

        drawRow(graphics, AbilityKeyBindings.abilityKey(slot),
                skill == null ? null : skill.getHintIcon(),
                KeyHintVisuals.stateOf(skill, data, armed),
                remaining, y, usable, wave, fade);
    }

    /** Une ligne : la plaque, le capuchon, la competence, son halo, et la recharge qui reste. */
    private static void drawRow(GuiGraphics graphics, KeyMapping key, ResourceLocation icon,
                                KeyHintVisuals.State state, float remaining, int y,
                                boolean usable, float wave, float fade) {
        blit(graphics, nearest(PLATE), 0, y, KeyHintVisuals.PLATE_W, KeyHintVisuals.PLATE_H,
                0, 0, PLATE_TEX_W, PLATE_TEX_H, PLATE_TEX_W, PLATE_TEX_H, 0xFFFFFF, fade);

        boolean mouse = key.getKey().getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE;
        int button = key.getKey().getValue();
        String label = key.getTranslatedKeyMessage().getString();
        KeyHintVisuals.Cap cap = KeyHintVisuals.cap(mouse, button);

        // Le capuchon, gris quand l'aptitude ne peut pas servir OU quand la competence est en
        // recharge (comme l'original, qui testait les deux ensemble).
        float capTint = KeyHintVisuals.capTint(usable, remaining);
        blit(graphics, capTexture(cap, label),
                KeyHintVisuals.CAP_X, y + KeyHintVisuals.CAP_Y,
                KeyHintVisuals.CAP_SIZE, KeyHintVisuals.CAP_SIZE,
                0, 0, 50, 48, 50, 48,
                gray(capTint), fade);

        String capLabel = KeyHintVisuals.capLabel(cap, label, button);
        if (!capLabel.isEmpty()) {
            drawLabel(graphics, capLabel, y, fade);
        }

        // Le cadre de l'icone, puis l'icone de la competence rangee sur cette touche.
        //
        // LE CADRE SE LIT AU PLUS PROCHE VOISIN, comme les capuchons et le halo : lu en lisse, son
        // liseré de cinq unites s'etalait sur deux ou trois pixels d'un degrade pale, la ou
        // l'original en pose UN seul, net — le joueur l'a vu tout de suite (« la partie haute et a
        // droite sont plus epaisses que les autres cotes »). Le filtre est repose a chaque image,
        // parce que d'autres ecrans passent les memes textures en lisse.
        blit(graphics, nearest(FRAME),
                KeyHintVisuals.FRAME_X, y + KeyHintVisuals.FRAME_Y,
                KeyHintVisuals.FRAME_SIZE, KeyHintVisuals.FRAME_SIZE,
                0, 0, KeyHintVisuals.FRAME_SIZE, KeyHintVisuals.FRAME_SIZE,
                KeyHintVisuals.FRAME_SIZE, KeyHintVisuals.FRAME_SIZE, 0xFFFFFF, fade);

        if (icon == null) return;

        // Le halo de l'etat, AVANT l'icone : c'est un cadre de huit images posees autour de sa
        // boite, et l'icone doit passer par-dessus. Orange en charge, bleu en execution, rien au
        // repos ni pendant une recharge.
        float glow = KeyHintVisuals.glowAlpha(state, remaining, wave) * fade;
        if (glow > 0f) {
            drawGlow(graphics, y, state.glow, glow);
        }

        // L'ICONE au plus proche voisin, comme tout le rappel : l'arbre des competences et l'ecran
        // du developpeur la passent en lisse (une fois pour toutes), et le rappel la dessinait donc
        // floué — « les competences sont un peu flou, et on voit des pixels qui devraient etre en
        // semi transparence qui ne le sont pas ». On lui rend son filtre apres, sinon ces deux
        // ecrans-la perdraient leur lissage.
        boolean smoothed = cn.academy.client.gui.cgui.CGuiShaders.isLinear(icon);
        cn.academy.client.gui.GuiTextures.nearest(icon);
        blit(graphics, icon,
                KeyHintVisuals.ICON_X, y + KeyHintVisuals.ICON_Y,
                KeyHintVisuals.ICON_SIZE, KeyHintVisuals.ICON_SIZE,
                0, 0, KeyHintVisuals.ICON_SIZE, KeyHintVisuals.ICON_SIZE,
                KeyHintVisuals.ICON_SIZE, KeyHintVisuals.ICON_SIZE, 0xFFFFFF,
                KeyHintVisuals.iconAlpha(state, remaining, wave) * fade);
        if (smoothed) cn.academy.client.gui.GuiTextures.linear(icon);

        // La recharge qui reste : l'original assombrissait le bas de l'icone d'autant, en plus
        // de l'estomper entierement (voir COOLDOWN_ICON_ALPHA).
        if (remaining <= 0f) return;

        int filled = Math.round(KeyHintVisuals.ICON_SIZE * remaining);
        // Le melange n'est pas la seule chose que le dernier `blit` laisse derriere lui : il
        // laisse aussi sa COULEUR au shader, donc son alpha (0,4 sur une icone en recharge).
        // `fill` multiplie sa propre couleur par celle du moment (`ColorModulator`), d'ou un
        // voile a 0,4 x 0,3 = 0,12 au lieu de 0,3 : le gris de la recharge sortait plus sombre
        // que celui de l'original — c'est ce que le joueur a vu (« pas aussi clair que dans le
        // vrai mod »). On remet la couleur a blanc opacque avant l'aplat.
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.fill(
                KeyHintVisuals.ICON_X, y + KeyHintVisuals.ICON_Y + KeyHintVisuals.ICON_SIZE - filled,
                KeyHintVisuals.ICON_X + KeyHintVisuals.ICON_SIZE,
                y + KeyHintVisuals.ICON_Y + KeyHintVisuals.ICON_SIZE,
                KeyHintVisuals.withAlpha(KeyHintVisuals.COOLDOWN_OVERLAY, fade));
    }

    /**
     * Le halo de l'etat : les huit images de lueur de l'original, avec leurs pixels transparents.
     *
     * <p>Quatre cotes ({@code glow_left}, {@code right}, {@code up}, {@code down}) et quatre coins
     * ({@code ru}, {@code rd}, {@code lu}, {@code ld}). Ce sont des DEGRADES : mesure sur
     * {@code glow_left.png}, 32x166 dont l'alpha va de 22 a 195 d'un bord a l'autre. Le port les
     * avait remplacees une fois par un filet uni — le halo paraissait alors « flou plutot qu'une
     * petite ligne » —, et le joueur a redemande les degrades : « tu n'as pas mis les pixels bleu
     * transparent. Pareil pour le orange ». Entre les deux, la vraie difference est le FILTRAGE :
     * un degrade resserre dans cinq unites (un pixel a l'ecran) lu en lisse n'est qu'une tache,
     * lu au plus proche voisin il donne la petite ligne liseree de l'original.
     *
     * <p>Le dessin lui-meme vit dans {@code GlowSprites} : c'est le meme halo qui entoure le
     * numero du prereglage en service, sous la barre de CP.
     */
    private static void drawGlow(GuiGraphics graphics, int rowY, int rgb, float alpha) {
        cn.academy.client.gui.GlowSprites.draw(graphics,
                KeyHintVisuals.ICON_X, rowY + KeyHintVisuals.ICON_Y,
                KeyHintVisuals.ICON_SIZE, KeyHintVisuals.ICON_SIZE,
                KeyHintVisuals.GLOW_SIZE, rgb, alpha);
    }

    /**
     * L'etiquette du capuchon, centree dessus.
     *
     * <p>Elle se pose par le HAUT DE SA CELLULE ({@code AcademyFonts.drawTop}), la convention des
     * widgets de l'original : le centre de la cellule tombe donc sur le centre du capuchon, et le
     * dessin de la lettre suit ses propres metriques. L'ancien calcul passait par une pose mise a
     * l'echelle d'un facteur fractionnaire, qui deplacait la lettre et l'epaississait.
     */
    private static void drawLabel(GuiGraphics graphics, String text, int rowY, float fade) {
        var font = Minecraft.getInstance().font;
        int width = cn.academy.client.gui.AcademyFonts.width(font, text, LABEL_CELL);
        int left = KeyHintVisuals.CAP_X + KeyHintVisuals.CAP_SIZE / 2 - width / 2 + LABEL_LIFT_X;
        int top = rowY + KeyHintVisuals.CAP_Y
                + (KeyHintVisuals.CAP_SIZE - LABEL_CELL) / 2 + LABEL_LIFT;

        // La police du mod multiplie chaque glyphe par la teinte DU MOMENT : c'est par elle que
        // le fondu passe pour le texte (les images, elles, ont leur alpha par le blit).
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fade);
        cn.academy.client.gui.AcademyFonts.drawTop(graphics, font, text, left, top,
                KeyHintVisuals.LABEL_COLOR, LABEL_CELL);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static ResourceLocation capTexture(KeyHintVisuals.Cap cap, String label) {
        return nearest(texture(KeyHintVisuals.capTexture(cap, label)));
    }

    /**
     * Une texture du rappel, lue au plus proche voisin.
     *
     * <p>C'est le filtrage de l'original : ses icones sont des dessins au pixel, et un degradé
     * resserre dans un pixel d'ecran doit rester lisible. Le rappel pose ce filtre a chaque image
     * parce que d'autres ecrans passent les memes images en lisse ({@code CGuiShaders.linear}).
     */
    private static ResourceLocation nearest(ResourceLocation texture) {
        cn.academy.client.gui.GuiTextures.nearest(texture);
        return texture;
    }

    /** Un gris a trois canaux egaux, pour la teinte du capuchon. */
    private static int gray(float value) {
        int channel = Math.round(255.0f * value);
        return (channel << 16) | (channel << 8) | channel;
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture,
                             int x, int y, int width, int height,
                             int u, int v, int uWidth, int vHeight, int texW, int texH,
                             int rgb, float alpha) {
        if (width <= 0 || height <= 0) return;
        // Le melange se perd en route, et c'est le piege de cet element. `drawString` (comme
        // `fill`) dessine via un RenderType translucide dont le nettoyage appelle
        // `RenderSystem.disableBlend()` : tout ce qui suit l'etiquette d'un capuchon se
        // dessine donc sans melange. Consequences vecues : les pixels semi-transparents du
        // cadre et de l'icone s'ecrivent en plein (cadre plus sombre, icone opaque), et
        // l'estompage de recharge est ignore sur toutes les lignes SAUF la premiere (ses deux
        // touches sont des boutons de souris, donc sans etiquette). On rallume avant chaque
        // texture : c'est le seul endroit qui dessine ici.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f, alpha);
        graphics.blit(texture, x, y, width, height, u, v, uWidth, vHeight, texW, texH);
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("academy", "textures/guis/key_hint/" + path + ".png");
    }
}
