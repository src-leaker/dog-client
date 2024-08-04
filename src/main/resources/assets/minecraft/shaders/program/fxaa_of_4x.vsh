#version 400 core

layout(location = 0) in vec4 inPosition;

uniform mat4 ProjMat;
uniform vec2 OutSize;

out vec2 texCoord;

void main()
{
    vec4 outPos = ProjMat * vec4(inPosition.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    texCoord = inPosition.xy / OutSize;
    texCoord.y = 1.0 - texCoord.y;
}