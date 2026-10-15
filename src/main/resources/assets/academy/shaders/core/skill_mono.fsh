#version 150

// L'icone grisee d'une competence qui n'est pas apprise. C'est le programme de LambdaLib2, mot pour
// mot : la moyenne des trois canaux, l'opacite gardee telle quelle.
//     vec4 result = Color * texture2D(sampler, UV);
//     float c = (result.r + result.g + result.b) / 3;
//     gl_FragColor = vec4(c, c, c, result.a);
//
// Il ne suffirait pas d'une teinte grise : une teinte MULTIPLIE, donc elle assombrit les couleurs sans
// les eteindre — une icone bleue reste bleue, plus sombre. C'est une DESATURATION qu'il faut, et elle
// demande de lire les trois canaux pour en faire la moyenne.

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 result = texture(Sampler0, texCoord0) * vertexColor;
    float mono = (result.r + result.g + result.b) / 3.0;
    fragColor = vec4(mono, mono, mono, result.a) * ColorModulator;
}
