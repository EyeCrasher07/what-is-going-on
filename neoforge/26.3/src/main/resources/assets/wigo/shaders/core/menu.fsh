#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;
layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

void main() {
    vec4 color = texture(Sampler0, texCoord0) * ColorModulator;
    // Vertex RGB stores an integer brightness percentage; alpha stores opacity independently.
    float brightness = round(vertexColor.r * 255.0) / 100.0;
    color.rgb *= vertexColor.a * brightness;
    color.a *= vertexColor.a;
    if (color.a <= 0.0) discard;
    // The local GUI target contains premultiplied RGB; retain the original coverage.
    color.rgb /= color.a;
    #ifdef WIGO_OPAQUE
    color.a = 1.0;
    #endif

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #elif defined(OIT_ACCUMULATE)
    fragColor = sampleColorForAccumulation(color);
    #else
    fragColor = vec4(color.rgb * color.a, color.a);
    #endif
}
