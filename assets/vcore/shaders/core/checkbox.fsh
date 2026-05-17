#version 150

uniform vec4 color;
uniform vec2 uSize;
uniform vec2 uLocation;
uniform float radius;
uniform float thickness;
uniform float progress;

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, float radius) {
    return length(max(abs(center) - size + radius, 0.0)) - radius;
}

float segmentSDF(vec2 point, vec2 start, vec2 end) {
    vec2 segment = end - start;
    float segmentLength = max(dot(segment, segment), 0.0001);
    float t = clamp(dot(point - start, segment) / segmentLength, 0.0, 1.0);
    return length(point - start - segment * t);
}

float drawSegment(vec2 point, vec2 start, vec2 end, float drawProgress, float radius, float aa) {
    if (drawProgress <= 0.0) {
        return 0.0;
    }

    vec2 partialEnd = mix(start, end, clamp(drawProgress, 0.0, 1.0));
    float distance = segmentSDF(point, start, partialEnd);
    return 1.0 - smoothstep(radius, radius + aa, distance);
}

void main() {
    vec2 local = gl_FragCoord.xy - uLocation;
    vec2 center = local - uSize * 0.5;

    float distance = roundedBoxSDF(center, uSize * 0.5 - vec2(thickness * 0.5), radius);
    float aa = max(fwidth(distance), 0.85);
    float border = 1.0 - smoothstep(thickness * 0.5, thickness * 0.5 + aa, abs(distance));
    float fill = (1.0 - smoothstep(-aa, aa, distance)) * 0.045 * progress;

    vec2 checkPoint = vec2(local.x, uSize.y - local.y);
    vec2 checkMin = vec2(2.0, 7.0);
    vec2 checkMax = vec2(18.8, 18.0);
    float checkScale = min(uSize.x, uSize.y) * 0.56 / (checkMax.x - checkMin.x);
    vec2 checkOffset = uSize * 0.5 - (checkMin + checkMax) * checkScale * 0.5;
    vec2 checkStart = checkOffset + vec2(2.0, 13.1) * checkScale;
    vec2 checkMid = checkOffset + vec2(8.0, 18.0) * checkScale;
    vec2 checkEnd = checkOffset + vec2(18.8, 7.0) * checkScale;
    float firstProgress = clamp(progress / 0.36, 0.0, 1.0);
    float secondProgress = clamp((progress - 0.36) / 0.64, 0.0, 1.0);
    float checkRadius = 0.28 * max(checkScale / 0.33, 1.0);
    float checkAlpha = max(
            drawSegment(checkPoint, checkStart, checkMid, firstProgress, checkRadius, aa * 0.55),
            drawSegment(checkPoint, checkMid, checkEnd, secondProgress, checkRadius, aa * 0.55)
    ) * progress;

    float alpha = max(border * color.a, fill * color.a);
    alpha = max(alpha, checkAlpha * color.a);
    fragColor = vec4(color.rgb, alpha);
}
