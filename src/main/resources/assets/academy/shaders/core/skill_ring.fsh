#version 150

// L'anneau de progression d'une competence, mot pour mot celui de l'original :
//     progress > texture2D(texGradient, uv).r ? texture2D(texCircle, uv) : vec4(0, 0, 0, 0)
//
// Le masque (Sampler1) porte l'ANGLE : sa composante rouge monte de zero a un sur un tour complet, si
// bien que la comparaison ci-dessous decoupe l'anneau en un secteur dont la taille suit progress.
// Mesure faite sur l'image : 52 a zero degre, 203 a cent quatre-vingts, 0 a deux cent soixante-dix.
//
// C'est ce qui interdit de faire plus simple : la valeur depend de l'angle et non de la position, donc
// aucun decoupage rectangulaire ne peut la remplacer.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform float SkillProgress;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (SkillProgress <= texture(Sampler1, texCoord0).r) {
        discard;
    }
    fragColor = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
}
