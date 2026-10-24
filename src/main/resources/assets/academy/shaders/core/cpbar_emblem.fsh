#version 150

// Le REMPLISSAGE du temoin de CP, perce de l'embleme du pouvoir.
//
// C'est le cpbar_cp.frag de l'original, a la ligne pres : la barre est une image de 964x147, et
// l'icone de la categorie occupe une boite de 65 unites posee a (857, 43). La ou l'icone a de
// l'opacite, celle du remplissage est RETIREE (mask = 1 - icone.a) : la silhouette du pouvoir
// apparait donc en creux, avec le fond de la barre et le monde derriere.
//
// L'original faisait ce calcul sur les coordonnees de la texture de la barre, et non sur celles du
// quad dessine : c'est pour cela que texCoord0 est multiplie par la taille de l'image. Un quad
// partiel (la coupe en biais du bout gauche) tombe donc juste, lui aussi.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

const vec2 ORIG_SIZE = vec2(964.0, 147.0);
const vec2 ICON_OFFSET = vec2(857.0, 43.0);
const vec2 ICON_MUL = vec2(1.0 / 65.0, 1.0 / 65.0);

void main() {
    vec2 spot = ((texCoord0 * ORIG_SIZE) - ICON_OFFSET) * ICON_MUL;

    float mask;
    if (spot.s < 0.0 || spot.s > 1.0 || spot.t < 0.0 || spot.t > 1.0) {
        mask = 1.0;
    } else {
        mask = 1.0 - texture(Sampler1, spot.st).a;
    }

    vec4 tex = texture(Sampler0, texCoord0.st);
    fragColor = vertexColor * ColorModulator * vec4(tex.rgb, tex.a * mask);
}
