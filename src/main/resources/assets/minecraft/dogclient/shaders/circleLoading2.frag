#version 120

uniform vec2 iResolution;
uniform float iTime;

float dist(vec2 p) {
    float min_d = 0.;
    float t = -iTime;
    float v = 0.;

    for (int i=0; i<8; i++) {
        float o = smoothstep(0.0,.75,fract((v+t)/3.)-.25)+floor((v+t)/3.);
        float a = o*3.14159265*2.;
        min_d += 1./length(p+vec2(sin(a), cos(a))*.5)*(1.5-v);
        v += .125;
    }
    return min_d;
}

void main()
{
    vec2 fragCoord = gl_TexCoord[0].st * iResolution;
    vec2 uv = (fragCoord-iResolution.xy/2.0)/iResolution.y*2.;
    float d = dist(uv)/10.-6.;

    gl_FragColor = vec4(d);
}