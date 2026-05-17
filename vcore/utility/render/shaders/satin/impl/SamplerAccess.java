package vcore.utility.render.shaders.satin.impl;

import java.util.List;

public interface SamplerAccess {
   boolean hasSampler(String var1);

   List<String> getSamplerNames();

   List<Integer> getSamplerShaderLocs();
}
