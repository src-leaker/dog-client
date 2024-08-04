#version 120

const float PI=3.14159265358979323846;
const float TAU = 6.28318530717958647692;
const float STEP_LENGTH = 0.01;
const float ANGLE_OFFSET = PI*0.5;
uniform vec4 color1;
uniform vec4 color2;

uniform float progress;
uniform vec2 iResolution;
uniform int isRound;

vec4 getGradientValue(in vec2 uv)
{
    vec2 dist =	vec2(1.0, 0.0) - vec2(-1.0, 0.0);
    float val = dot( uv - vec2(-1,0), dist ) / dot( dist, dist );
    clamp( val, 0.0, 1.0 );
    vec4 color = mix( color1, color2, val );
    color.a = mix(color1.a, color2.a, val);
    return color;
}

void main()
{
    float innerRadius = 0.5;
    float outerRadius = 0.65;
    float startAngle = 0.0;
    float endAngle = progress* TAU;
    vec2 fragCoord = gl_TexCoord[0].st * iResolution;
    vec2 uv = (2.0*fragCoord.xy - iResolution.xy)/iResolution.y;
    float d = length( uv );
    vec4 ioColor = getGradientValue(uv);

    float w = fwidth( d ) * 1.0;
    float c = smoothstep( outerRadius + w, outerRadius - w, d );
    c -= smoothstep( innerRadius + w, innerRadius - w, d );

    gl_FragColor = vec4(ioColor.rgb * vec3(c), 1.0 * vec3(c) * ioColor.a);

    float angle = (atan(uv.y,uv.x)) + ANGLE_OFFSET;
    if( angle < 0.0 ) angle += PI * 2.0;
    if( angle > endAngle){
        float a = smoothstep( 0.02, -w*2.0,  abs(endAngle - angle) );
        gl_FragColor *= a;
    }
    if(angle - w*2.0 < startAngle ){
        float a = smoothstep(  -w*2.0, w*2.0, (abs(startAngle - angle)) );
        gl_FragColor *= a;
    }

    float lineWidth = (outerRadius - innerRadius) * 0.5;
    float midRadius = innerRadius + lineWidth;

    vec2 endAnglePos = vec2( cos(endAngle-ANGLE_OFFSET), sin(endAngle-ANGLE_OFFSET)) * vec2(midRadius);
    float dist = length( uv - endAnglePos );

    if(isRound == 0){
        vec2 startAnglePos = vec2( cos(startAngle-ANGLE_OFFSET), sin(startAngle-ANGLE_OFFSET)) * vec2(midRadius);
        dist = length(uv - startAnglePos);
    }

    float buttAlpha = smoothstep( lineWidth + w, lineWidth - w, dist );

    gl_FragColor = mix(gl_FragColor, ioColor, buttAlpha );
}