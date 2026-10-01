package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;

/**
 * Le serveur refusera-t-il d'ouvrir ce maintien ? Ce que le client peut en savoir tout de suite.
 *
 * <h2>Pourquoi le client se mele de ce qui ne le regarde pas</h2>
 *
 * <p>Il ne le fait que pour le <b>montrer</b> : un maintien s'ouvre d'abord chez le client — c'est
 * lui qui tient la touche, et c'est cette avance qui rend le bouclier, la charge et la traction
 * immediats. Mais le serveur a le dernier mot, et quand il refuse, le client avait deja commence a
 * animer : le joueur voyait le debut du bouclier et entendait son son pour un pouvoir inexistant.
 * Ce sont ses mots : « quand on est en cooldown ou en overload, on peut toujours activer brievement
 * le pouvoir, ce qui lance le son et affiche tres rapidement le debut du bouclier ».
 *
 * <h2>Ce qu'elle relit</h2>
 *
 * <p>Les refus d'ouverture de {@code ActivateSkillPacket} : l'aptitude eteinte, le brouillage, une
 * recharge en cours, la surcharge qui redescend, et la reserve trop juste pour payer l'ouverture.
 * La surcharge en elle-meme n'en est pas un — {@code AbilityData#perform} l'encaisse sans broncher
 * — donc elle ne l'est pas ici non plus.
 *
 * <p>Le client ne peut que <b>sous-estimer</b> ce que le joueur a : sa reserve et sa surcharge ne
 * bougent que par son propre tick et par les envois du serveur, donc elles sont toujours au moins
 * aussi bonnes que les vraies. Un refus lu ici est donc un refus certain, et une activation
 * legitime ne peut pas etre empechee — l'inverse serait bien pire qu'un scintillement.
 *
 * <p>Classe sans aucun type de client, expres : la regle se fige par un test, comme les courbes
 * du bouclier.
 */
public final class HoldRefusal {

    private HoldRefusal() {}

    /**
     * Vrai si l'appui n'ouvrira rien : le serveur refusera.
     *
     * @param data les nombres du client, tels qu'il les connait — voir {@code ClientAbilityData}
     */
    public static boolean refusesStart(Skill skill, AbilityData data) {
        if (!data.isActivated()) return true;
        if (data.isInterfered()) return true;
        if (data.getCooldown(skill) > 0) return true;
        if (data.isOverloadRecovering()) return true;
        return data.getControlPoint() < skill.getCpCost(data);
    }
}
