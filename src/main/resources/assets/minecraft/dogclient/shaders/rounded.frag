#version 120

uniform vec2 size;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform float round;

//https://www.shadertoy.com/view/NtVSW1
float sdRoundRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p)-b+r;
    return min(max(q.x,q.y),0.0) + length(max(q,0.0)) - r;
}

vec4 normalBlend(vec4 src, float dst) {
    float edgeSoftness = 1.0f;
    float finalAlpha = 1.0f-smoothstep(0.0f, edgeSoftness * 2.0f, dst);

    return vec4(
        src.rgb, src.a * finalAlpha
    );
}

//https://stackoverflow.com/questions/12964279/whats-the-origin-of-this-glsl-rand-one-liner
float rand(vec2 co) {
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    if (round > min(size.x, size.y)/2) {
        discard;
    }

    vec2 rectCenter = size/2;

    float distRect = sdRoundRect(rectCenter - (gl_TexCoord[0].st * size), rectCenter - 1.0, round);
    vec4 color = mix(mix(color1, color2, gl_TexCoord[0].st.y), mix(color3, color4, gl_TexCoord[0].st.y), gl_TexCoord[0].st.x);
    vec4 finalOutput = normalBlend(color, distRect);

    float factor = 0.005;
    float dither = rand(gl_TexCoord[0].st * size) * factor;
    finalOutput.rgb += dither;

    gl_FragColor = finalOutput;
}
