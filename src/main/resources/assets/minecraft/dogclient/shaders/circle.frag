#version 120

uniform vec4 color;
uniform float size;
uniform float blur;

void main(){
    vec2 coord = gl_TexCoord[0].st * 2.0 - 1.0;
    float len = length(coord);
    float alpha = smoothstep(0, blur, 1-len) ;

    gl_FragColor = vec4(color.rgb, color.a * alpha);
}
