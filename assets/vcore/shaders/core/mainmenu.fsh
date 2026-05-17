#version 150

uniform float iTime;
uniform vec3 iResolution;
uniform int mode;

uniform float animationSpeed;

uniform float enableTop;
uniform float enableMiddle;
uniform float enableBottom;

uniform float topLineCount;
uniform float middleLineCount;
uniform float bottomLineCount;

uniform float topLineDistance;
uniform float middleLineDistance;
uniform float bottomLineDistance;

uniform vec3 topWavePosition;
uniform vec3 middleWavePosition;
uniform vec3 bottomWavePosition;

uniform vec2 iMouse;
uniform float interactive;
uniform float bendRadius;
uniform float bendStrength;
uniform float bendInfluence;

uniform float parallax;
uniform vec2 parallaxOffset;

uniform vec3 middleLineColor;
uniform vec3 sideLineColor;
uniform float glowStrength;

const int MAX_LINES = 64;

out vec4 fragColor;

mat2 rot(float a) {
    float s = sin(a);
    float c = cos(a);
    return mat2(c, s, -s, c);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float lineWave(vec2 uv, float offset, vec2 screenUv, vec2 mouseUv, bool shouldBend) {
    float time = iTime * animationSpeed;
    float xOffset = offset;
    float xMovement = time * 0.1;
    float amp = sin(offset + time * 0.2) * 0.3;
    float y = sin(uv.x + xOffset + xMovement) * amp;

    if (shouldBend) {
        vec2 d = screenUv - mouseUv;
        float influence = exp(-dot(d, d) * bendRadius);
        float bendOffset = (mouseUv.y - screenUv.y) * influence * bendStrength * bendInfluence;
        y += bendOffset;
    }

    float m = uv.y - y;
    return 0.0175 / max(abs(m) + 0.01, 1e-3) + 0.01;
}

void addWaveGroup(
    inout vec3 col,
    vec2 baseUv,
    vec2 mouseUv,
    float lineCount,
    float lineDistance,
    vec3 wavePos,
    float baseOffset,
    float stepOffset,
    float intensity,
    vec3 baseColor,
    bool flipX,
    bool shouldBend,
    bool centerBlend
) {
    int count = int(lineCount);
    if (count <= 0) {
        return;
    }

    for (int i = 0; i < MAX_LINES; ++i) {
        if (i >= count) {
            break;
        }

        float fi = float(i);
        float angle = wavePos.z * log(length(baseUv) + 1.0);
        vec2 ruv = baseUv * rot(angle);

        if (flipX) {
            ruv.x *= -1.0;
        }

        vec3 lineColor = baseColor;
        float coreBoost = 0.0;

        if (centerBlend && count > 1) {
            float t = fi / float(count - 1);
            float edgeFactor = abs(t * 2.0 - 1.0);
            lineColor = mix(middleLineColor, sideLineColor, edgeFactor);
            coreBoost = 1.0 - edgeFactor;
        }

        col += (lineColor * 0.5) * lineWave(
            ruv + vec2(lineDistance * fi + wavePos.x, wavePos.y),
            baseOffset + stepOffset * fi,
            baseUv,
            mouseUv,
            shouldBend
        ) * intensity * (1.0 + coreBoost * 0.35);
    }
}

vec3 renderFloatingLines(vec2 fragCoord) {
    vec2 baseUv = (2.0 * fragCoord - iResolution.xy) / iResolution.y;
    baseUv.y *= -1.0;

    if (parallax > 0.5) {
        baseUv += parallaxOffset;
    }

    vec3 col = vec3(0.0);
    vec2 mouseUv = vec2(0.0);
    bool isInteractive = interactive > 0.5;

    if (isInteractive) {
        mouseUv = (2.0 * iMouse - iResolution.xy) / iResolution.y;
        mouseUv.y *= -1.0;
    }

    if (enableBottom > 0.5) {
        addWaveGroup(col, baseUv, mouseUv, bottomLineCount, bottomLineDistance, bottomWavePosition, 1.5, 0.2, 0.2, sideLineColor, false, isInteractive, false);
    }

    if (enableMiddle > 0.5) {
        addWaveGroup(col, baseUv, mouseUv, middleLineCount, middleLineDistance, middleWavePosition, 2.0, 0.15, 1.0, middleLineColor, false, isInteractive, true);
    }

    if (enableTop > 0.5) {
        addWaveGroup(col, baseUv, mouseUv, topLineCount, topLineDistance, topWavePosition, 1.0, 0.2, 0.1, sideLineColor, true, isInteractive, false);
    }

    vec3 glow = pow(max(col, vec3(0.0)), vec3(0.9)) * glowStrength;
    return col + glow;
}

vec3 renderSilk(vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    vec2 suv = uv;
    float scale = 1.0;
    float rotation = 0.0;
    float speed = 0.7;
    float noiseIntensity = 1.5;

    vec2 ruv = rot(rotation) * ((suv - 0.5) * scale) + 0.5;
    vec2 tex = ruv * scale;
    float tOffset = speed * iTime;

    tex.y += 0.03 * sin(8.0 * tex.x - tOffset);

    float pattern = 0.6 +
                    0.4 * sin(5.0 * (tex.x + tex.y +
                    cos(3.0 * tex.x + 5.0 * tex.y) +
                    0.02 * tOffset) +
                    sin(20.0 * (tex.x + tex.y - 0.1 * tOffset)));

    float rnd = hash12(gl_FragCoord.xy);
    vec3 silkColor = mix(middleLineColor, sideLineColor, 0.4);
    vec3 col = silkColor * pattern - vec3(rnd / 15.0 * noiseIntensity);
    return max(col, vec3(0.0));
}

void main() {
    vec3 color;

    if (mode == 1) {
        color = renderSilk(gl_FragCoord.xy);
    } else {
        color = renderFloatingLines(gl_FragCoord.xy);
    }

    fragColor = vec4(max(color, vec3(0.0)), 1.0);
}
