#version 120

varying vec4 texCoord;
varying vec3 normal, color, pos;

uniform sampler2D textureSampler;
uniform float time;

#define F cos(x - y) * cos(y), sin(x + y) * sin(y)

vec2 s(vec2 p)
{
    float d = time * 0.2;
    float x = 16.0 * (p.x + d);
    float y = 16.0 * (p.y + d);
    return vec2(F);
}

void main(void)
{
    vec2 w = gl_TexCoord[0].xy;
    vec3 res = vec3(10, 10, 0);
    vec2 i = res.xy;
    vec2 r = w / i;
    vec2 q = r + 8.0 / res.x * (s(r) - s(r + i));
    q.y = 1.0 - q.y;
    gl_FragColor = texture2D(textureSampler, q);


    //vec2 v = s(gl_TexCoord[0].xy);
    //float alpha = v.x * v.y;
    //
    //gl_FragColor = vec4(1, 1, 1, alpha);
}

//void main(void) {
//
////  vec3 nlight = normalize(gl_LightSource[0].position.xyz - pos);
////  vec3 neye = normalize(-pos);
////  vec3 nnormal = normalize(normal);
////  vec3 nhalf = normalize(neye + nlight);
////  float diff = max(0.0, dot(nlight, nnormal));
////  float spec = diff > 0.0 ? pow(dot(nhalf, nnormal), 0.0) : 0.0;
//
//    //gl_FragColor = texture2D(textureSampler, gl_TexCoord[0].yx);
//    //gl_FragColor.x = texCoord.y;
//    //gl_FragColor.w = 0.5;
//    gl_FragColor.w = texCoord.y > 0 ? 1 : 0;
//
////    float timeScaled = time * 0.1;
////    vec4 distortedPosition = texCoord;
////
////    vec2 scale = vec2(0.1, 0.1);
////
////    vec2 distortedTexCoords = texture2D(textureSampler, vec2(texCoord.x + timeScaled, texCoord.y - timeScaled)).rg * 0.1;
////    distortedTexCoords = (texCoord.xy) + (vec2(distortedTexCoords.x, distortedTexCoords.y + timeScaled));
////    vec2 rippleDirection = (texture2D(textureSampler, distortedTexCoords * scale).rg * 2.0 - 1.0) * 0.06;
////
////    distortedPosition += vec4(rippleDirection.x * 5.0, rippleDirection.y * 5.0, 0.0, 0.0);
////
////    float centerDistanceDistorted = (distortedPosition.x * distortedPosition.x + distortedPosition.y * distortedPosition.y + (cos(distortedPosition.z * distortedPosition.z) * 2));
////    float centerDistance = (texCoord.x * texCoord.x + texCoord.y * texCoord.y + (cos(texCoord.z * texCoord.z) * 2));
////
////    float brightness = sin(rippleDirection.x + rippleDirection.y);
////    float brightnessEdge = clamp(sin(clamp((centerDistanceDistorted - 0.7) / 2, 0.0, 4.0)) + clamp((cos(timeScaled) + 1.0) * 0.15, 0.0, 0.3), 0.7, 1.0);
////    float alphaEdge = 1.0 - clamp(sin((centerDistanceDistorted - 3.3) / 2), 0.0, 1.0);
////    float alphaEdgeNonDistorted = 1.0 - clamp(sin((centerDistance - 3.3) / 2) * 2, 0.0, 1.0);
////    float greenPulse = clamp(sin(timeScaled + (centerDistanceDistorted * 0.3)) * 0.1, -0.1, 0.1);
////
////    float mixedBrightness = clamp(brightnessEdge + brightness, 0.0, 1.0);
////    gl_FragColor = texture2D(textureSampler, gl_TexCoord[0].xy) * vec4(0.4 - greenPulse, 0.8, 0.7 - greenPulse, 1.0) * vec4(brightnessEdge, mixedBrightness, brightnessEdge, alphaEdge * alphaEdgeNonDistorted);
//}
